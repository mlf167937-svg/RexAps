# RexAps Release Signing

The Android distribution workflow builds three signed ARM Release APKs:

- `arm64-v8a`
- `armeabi-v7a`
- Universal (contains both ARM ABIs)

Each APK is signature-verified with `apksigner`, ABI-checked, and packed into
its corresponding `RexAps-*.zip`. The workflow also creates `Actions.zip` with
`Build.log`, `NodeJS-Download.log`, and `Full-Actions.log`.

Configure these repository or environment secrets in GitHub Actions:

- `RELEASE_KEYSTORE_BASE64`: base64-encoded persistent `.jks` / `.keystore` file.
- `RELEASE_STORE_PASSWORD`: keystore password.
- `RELEASE_KEY_ALIAS`: alias of the signing key.
- `RELEASE_KEY_PASSWORD`: password for that key.

Create the keystore once, store it outside the repository, and keep a secure
backup. Do not regenerate it for each build; updates signed with another key
cannot be installed as an in-place update over the previous release.

On a system with `keytool`, a new key can be generated with:

```bash
keytool -genkeypair -v \\
  -keystore rexaps-release.jks \\
  -alias rexaps-release \\
  -keyalg RSA -keysize 4096 -validity 10000
```

Encode the existing keystore as a single-line Base64 value (Linux):

```bash
base64 -w 0 rexaps-release.jks
```

Add that output as `RELEASE_KEYSTORE_BASE64` and set the three password/alias
secrets separately. Never commit the keystore or credentials.

The workflow intentionally stops if any required secret is missing or the key
cannot be opened. Local `:app:assembleRelease` builds do not use a debug key as
a fallback and are unsigned unless the same `RELEASE_*` environment variables
are supplied.

For a first-time walkthrough in Indonesian, see
[RELEASE_BUILD_GUIDE_ID.md](RELEASE_BUILD_GUIDE_ID.md).
