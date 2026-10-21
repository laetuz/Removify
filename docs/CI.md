# CI / CD

## Workflows

| Workflow | Trigger | Outputs |
|----------|---------|---------|
| `.github/workflows/ci.yml` | push to `main`, pull requests | `removify-android-debug` (APK), `removify-desktop-dmg` (DMG) |
| `.github/workflows/release.yml` | tags matching `v*` | GitHub Release with DMG + Android artifacts |

All jobs use JDK 21, the Gradle wrapper, and `gradle/actions/setup-gradle` caching.
CI runs the shared test suite (`:composeApp:allTests`) on the Android job.

## Versioning

- `versionName` comes from the pushed tag (e.g. `v0.2.0` → `0.2.0`), falling back to `0.1.0` locally.
- `versionCode` comes from `GITHUB_RUN_NUMBER`, falling back to `1` locally.
- Desktop packages use a fixed jpackage-compatible version (`1.0.0`) because jpackage rejects `0.x.y`.

## Android signing

Signed builds are produced automatically when these repository secrets exist:

| Secret | Value |
|--------|-------|
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded keystore file |
| `RELEASE_KEYSTORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias |
| `RELEASE_KEY_PASSWORD` | Key password |

Without them, the release workflow falls back to a debug-signed APK.

Create and encode a keystore:

```bash
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias removify \
  -keyalg RSA -keysize 2048 -validity 10000

# macOS
base64 -i release.keystore | pbcopy

# Linux
base64 -w0 release.keystore
```

Paste the output into the `RELEASE_KEYSTORE_BASE64` secret. Never commit the keystore
(`*.keystore` and `*.jks` are gitignored).

## macOS notes

The DMG is not code-signed or notarized yet, so Gatekeeper shows a warning on other
Macs. Signing requires an Apple Developer certificate and additional jpackage
configuration.
