# RexChat Android native runtime

## Runtime asset

The Release workflow downloads the immutable runtime archive from:

`https://github.com/mlf167937-svg/RexAps/releases/download/v1.1/rexchat-node-android-v1.zip`

The archive must contain these top-level directories and shared libraries:

- `arm64-v8a/libnode.so`
- `arm64-v8a/libcares.so`
- `arm64-v8a/libsqlite3.so`
- `arm64-v8a/libc++_shared.so`
- `arm64-v8a/librexchat_node.so`
- The same five files under `armeabi-v7a/`.

The workflow validates each file's ELF class, machine, dynamic dependencies, and absence of RPATH/RUNPATH. It also verifies that `libnode.so` exports `node::Start(int, char**)`, because the app builds its own JNI entry point against the official Node.js 26.13.0 headers. The NDK/AGP shared C++ runtime is packaged for the JNI bridge.

## Build and packaging

GitHub Actions downloads the archive before Gradle configures CMake. It places the ABI-specific runtime libraries under `app/src/main/jniLibs/<abi>/`, downloads Node.js 26.13.0 headers to `app/libnode/include/node/`, and installs the production Baileys dependencies into `app/src/main/assets/rexchat/node_modules/`. These generated files are CI inputs and should not be committed to Git.

CMake builds `librexnode.so`, a small JNI bridge that calls `node::Start(argc, argv)`. Kotlin loads the native dependency chain and starts Node on an IO worker thread. The Java/Kotlin side communicates with the Baileys engine over an authenticated HTTP server bound to `localhost` only.

`librexchat_node.so` from the archive is preserved in the APK and validated, but the app's Kotlin bridge currently calls the project's `librexnode.so`; it does not invoke undocumented JNI methods from `librexchat_node.so`.

## Verification

The workflow fails before packaging if an ABI library is missing, has the wrong architecture, depends on an unbundled non-system library, has RPATH/RUNPATH, or lacks the `node::Start` symbol. After packaging, it checks that the ABI APKs and universal APK contain the expected libraries and Baileys package. At runtime the app also checks that the embedded Node server reports `v26.13.0`.

The archive validation and successful APK build do not replace device testing. Pairing still needs to be tested on physical ARM64 and ARM32 devices, with WhatsApp QR/pairing, reconnect, and message send/receive.

## RexChat UI scope in this integration

The Chats tab now displays recent text messages received by Baileys, lets the user tap a message to select its chat JID, and sends text to either a full JID or a phone number (normalized to `@s.whatsapp.net`). Message history is held in the current app process; this change does not add a persistent local message database, media transfer, push notifications, or a full contact/chat synchronization layer.
