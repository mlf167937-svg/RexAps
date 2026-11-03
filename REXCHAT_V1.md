# RexChat V1 foundation

RexChat V1 replaces the old WebView entry point with a native Jetpack Compose screen and introduces the local Node.js process bridge contract.

## Included

- Native Compose screens for Chats, Plugins, and Account.
- Existing RexAps session metadata remains the source of account/session identity.
- One shared Node.js process is designed to host multiple account sockets.
- The runtime is requested when an account module opens and stopped when the last open RexChat session closes.
- JSON-lines bridge between Kotlin and Node.js.
- Baileys engine source with QR, pairing-code, connection, incoming-message, and text-send event contracts.
- Starter `menu.js` plugin and shared `RichHtml` helper library.
- QR rendering support through ZXing.

## Important build status

This source archive does not contain an Android Node.js 20+ executable or the shared `node_modules` dependency tree. The runtime adapter checks for both and reports a clear unavailable state instead of pretending that WhatsApp is connected. Therefore QR login, pairing, and live messaging are not yet operational in this archive.

The expected runtime layout in app-private storage is:

```text
files/rexchat/
  runtime/node
  engine/index.js
  package.json
  node_modules/@whiskeysockets/baileys/
  node_modules/pino/
  plugins/menu.js
  plugins/lib/richhtml.js
```

The Node.js executable must be built for Android and the device ABI (arm64-v8a or armeabi-v7a), with a Node.js version compatible with the Baileys package. Dependencies are shared once under `files/rexchat/node_modules`; they are not duplicated per account. Each account's Baileys auth state is stored under the existing app-private session directory at `files/sessions/<id>/baileys-auth`.

## Baileys package

The starter package pins `@whiskeysockets/baileys` 7.0.0-rc14 and Node.js >=20. This is a release candidate, so compatibility and API changes must be tested before shipping. The project is not affiliated with WhatsApp.

## Build verification

A Gradle compile was attempted in the editing environment, but the Gradle wrapper distribution could not be downloaded because the environment had no network/DNS access. No successful APK build is claimed.
