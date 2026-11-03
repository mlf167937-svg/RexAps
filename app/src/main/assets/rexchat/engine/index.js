import makeWASocket, { useMultiFileAuthState, DisconnectReason } from '@whiskeysockets/baileys'
import pino from 'pino'
import readline from 'node:readline'
import path from 'node:path'
import { pathToFileURL } from 'node:url'
import { readdir } from 'node:fs/promises'

const home = process.env.REXCHAT_HOME || process.cwd()
const sessionRoot = process.env.REXCHAT_SESSION_ROOT || path.join(home, 'sessions')
const accounts = new Map()
const plugins = []
const logger = pino({ level: 'silent' })

function emit(event, payload = {}) {
  process.stdout.write(`${JSON.stringify({ event, ...payload })}\n`)
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
      const moduleUrl = pathToFileURL(path.join(directory, file)).href
      const plugin = await import(moduleUrl)
      if (typeof plugin.default === 'function') {
        plugins.push({ name: file, register: plugin.default })
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
            const handler = plugin.handler
            if (typeof handler === 'function') await handler(api)
          } catch (error) {
            emit('error', { sessionId, message: `Plugin ${plugin.name}: ${error.message}` })
          }
        }
      }
    }
  })
  for (const plugin of plugins) {
    try {
      await plugin.register({ sessionId, socket, onCommand: handler => { plugin.handler = handler } })
    } catch (error) {
      emit('error', { sessionId, message: `Plugin ${plugin.name}: ${error.message}` })
    }
  }
  return account
}

async function handle(command) {
  const sessionId = Number(command.sessionId)
  if (command.type === 'add-account') {
    await createAccount(sessionId)
    return
  }
  if (command.type === 'pairing-code') {
    const account = accounts.get(sessionId) || await createAccount(sessionId)
    account.pairingPhone = String(command.phone || '').replace(/\D/g, '')
    account.pairingRequested = true
    if (!account.pairingPhone) throw new Error('Phone number is required')
    try {
      const code = await account.socket.requestPairingCode(account.pairingPhone)
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
    await account.socket.sendMessage(String(command.chatId), { text: String(command.text || '') })
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
  if (command.type === 'shutdown') {
    for (const account of accounts.values()) account.socket.end(undefined)
    accounts.clear()
    process.exit(0)
  }
}

await loadPlugins()
const input = readline.createInterface({ input: process.stdin, crlfDelay: Infinity })
input.on('line', line => {
  try {
    const command = JSON.parse(line)
    handle(command).catch(error => emit('error', { message: error.message }))
  } catch (error) {
    emit('error', { message: error.message })
  }
})
