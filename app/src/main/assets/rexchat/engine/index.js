import makeWASocket, { useMultiFileAuthState, DisconnectReason } from '@whiskeysockets/baileys'
import pino from 'pino'
import http from 'node:http'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { readdir } from 'node:fs/promises'

const engineDirectory = path.dirname(fileURLToPath(import.meta.url))
const home = path.resolve(engineDirectory, '..')
const readArgument = (prefix, fallback) => {
  const value = process.argv.find(argument => argument.startsWith(prefix))
  return value ? value.slice(prefix.length) : fallback
}
const port = Number(readArgument('--rexchat-port=', '43827'))
const authToken = readArgument('--rexchat-token=', '')
const sessionRoot = readArgument('--rexchat-session-root=', path.join(home, 'sessions'))

const accounts = new Map()
const plugins = []
const eventQueue = []
const logger = pino({ level: 'silent' })

function emit(event, payload = {}) {
  eventQueue.push({ event, ...payload })
  if (eventQueue.length > 1000) eventQueue.splice(0, eventQueue.length - 1000)
}

function textFromMessage(message) {
  const content = message?.message
  return content?.conversation || content?.extendedTextMessage?.text || ''
}

async function loadPlugins() {
  const directory = path.join(home, 'plugins')
  let files = []
  try {
    files = (await readdir(directory)).filter(file => file.endsWith('.js'))
  } catch {
    return
  }
  for (const file of files) {
    try {
      const moduleUrl = new URL(`../plugins/${file}`, import.meta.url)
      const plugin = await import(moduleUrl.href)
      if (typeof plugin.default === 'function') {
        plugins.push({ name: file, register: plugin.default, handler: null })
      }
    } catch (error) {
      emit('error', { message: `Plugin ${file}: ${error.message}` })
    }
  }
}

async function createAccount(sessionId) {
  if (accounts.has(sessionId)) return accounts.get(sessionId)
  const authDirectory = path.join(sessionRoot, String(sessionId), 'baileys-auth')
  const { state, saveCreds } = await useMultiFileAuthState(authDirectory)
  const socket = makeWASocket({
    auth: state,
    logger,
    printQRInTerminal: false,
    syncFullHistory: false,
    markOnlineOnConnect: false
  })
  const account = { socket, saveCreds, pairingPhone: null, pairingRequested: false }
  accounts.set(sessionId, account)
  socket.ev.on('creds.update', saveCreds)
  socket.ev.on('connection.update', async update => {
    if (update.qr) emit('qr', { sessionId, value: update.qr })
    if (update.connection === 'open') emit('connected', { sessionId })
    if (update.connection === 'close') {
      const code = update.lastDisconnect?.error?.output?.statusCode
      emit('disconnected', { sessionId, reason: `Koneksi ditutup (${code ?? 'unknown'})` })
      if (code === DisconnectReason.loggedOut) accounts.delete(sessionId)
    }
    if (account.pairingPhone && !account.pairingRequested && !state.creds.registered) {
      account.pairingRequested = true
      try {
        const code = await socket.requestPairingCode(account.pairingPhone)
        emit('pairing-code', { sessionId, value: code })
      } catch (error) {
        account.pairingRequested = false
        emit('error', { sessionId, message: error.message })
      }
    }
  })
  socket.ev.on('messages.upsert', async ({ messages, type }) => {
    if (type !== 'notify') return
    for (const message of messages) {
      const body = textFromMessage(message)
      if (!body) continue
      emit('message', {
        sessionId,
        id: message.key?.id || '',
        chatId: message.key?.remoteJid || '',
        fromMe: Boolean(message.key?.fromMe),
        text: body,
        timestamp: Number(message.messageTimestamp || 0)
      })
      if (!message.key?.fromMe) {
        const api = {
          sessionId,
          message,
          body,
          reply: value => socket.sendMessage(message.key.remoteJid, { text: String(value) })
        }
        for (const plugin of plugins) {
          try {
            if (typeof plugin.handler === 'function') await plugin.handler(api)
          } catch (error) {
            emit('error', { sessionId, message: `Plugin ${plugin.name}: ${error.message}` })
          }
        }
      }
    }
  })
  for (const plugin of plugins) {
    try {
      await plugin.register({
        sessionId,
        socket,
        onCommand: handler => { plugin.handler = handler }
      })
    } catch (error) {
      emit('error', { sessionId, message: `Plugin ${plugin.name}: ${error.message}` })
    }
  }
  return account
}

async function handle(command) {
  const sessionId = Number(command.sessionId)
  if (!Number.isInteger(sessionId) || sessionId <= 0) {
    throw new Error('Invalid RexChat session ID')
  }

  if (command.type === 'add-account') {
    await createAccount(sessionId)
    return
  }
  if (command.type === 'pairing-code') {
    const account = accounts.get(sessionId) || await createAccount(sessionId)
    const phone = String(command.phone || '').replace(/\D/g, '')
    if (!phone) throw new Error('Phone number is required')
    account.pairingPhone = phone
    account.pairingRequested = true
    try {
      const code = await account.socket.requestPairingCode(phone)
      emit('pairing-code', { sessionId, value: code })
    } catch (error) {
      account.pairingRequested = false
      throw error
    }
    return
  }
  if (command.type === 'send-text') {
    const account = accounts.get(sessionId)
    if (!account) throw new Error('Account is not connected')
    const rawChatId = String(command.chatId || '').trim()
    const chatId = rawChatId.includes('@')
      ? rawChatId
      : `${rawChatId.replace(/\D/g, '')}@s.whatsapp.net`
    const text = String(command.text || '').trim()
    if (!chatId || chatId === '@s.whatsapp.net') throw new Error('Nomor atau ID chat tidak valid')
    if (!text) throw new Error('Pesan tidak boleh kosong')
    const sent = await account.socket.sendMessage(chatId, { text })
    emit('message', {
      sessionId,
      id: sent?.key?.id || `local-${Date.now()}`,
      chatId,
      fromMe: true,
      text,
      timestamp: Number(sent?.messageTimestamp || Math.floor(Date.now() / 1000))
    })
    return
  }
  if (command.type === 'remove-account') {
    const account = accounts.get(sessionId)
    if (account) {
      account.socket.end(undefined)
      accounts.delete(sessionId)
    }
    return
  }
  throw new Error(`Unsupported RexChat command: ${command.type}`)
}

function sendJson(response, statusCode, value) {
  response.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'Cache-Control': 'no-store'
  })
  response.end(JSON.stringify(value))
}

async function readJson(request) {
  const chunks = []
  let size = 0
  for await (const chunk of request) {
    size += chunk.length
    if (size > 1024 * 1024) throw new Error('Request body too large')
    chunks.push(chunk)
  }
  return JSON.parse(Buffer.concat(chunks).toString('utf8') || '{}')
}

async function loadAndStartServer() {
  await loadPlugins()
  const server = http.createServer(async (request, response) => {
    if (request.headers['x-rexchat-token'] !== authToken) {
      sendJson(response, 401, { ok: false, error: 'Unauthorized' })
      return
    }

    const pathname = new URL(request.url || '/', `http://127.0.0.1:${port}`).pathname
    if (request.method === 'GET' && pathname === '/health') {
      sendJson(response, 200, { ok: true, version: process.version, accounts: accounts.size })
      return
    }
    if (request.method === 'GET' && pathname === '/events') {
      sendJson(response, 200, { events: eventQueue.splice(0, eventQueue.length) })
      return
    }
    if (request.method === 'POST' && pathname === '/command') {
      try {
        await handle(await readJson(request))
        sendJson(response, 200, { ok: true })
      } catch (error) {
        const message = error?.message || String(error)
        emit('error', { message })
        sendJson(response, 400, { ok: false, error: message })
      }
      return
    }
    sendJson(response, 404, { ok: false, error: 'Not found' })
  })

  server.listen(port, 'localhost', () => {
    console.log(`RexChat Node.js ${process.version} ready on localhost:${port}`)
  })
  server.on('error', error => {
    emit('error', { message: `RexChat HTTP server: ${error.message}` })
  })
}

loadAndStartServer().catch(error => {
  console.error('RexChat engine failed to initialize:', error)
  process.exitCode = 1
})
