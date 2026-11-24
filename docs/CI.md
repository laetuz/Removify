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

## Debug signing for update-compatible APKs

Debug APKs built locally and in CI only update each other when they share the same
signing key. GitHub runners generate a throwaway debug keystore per run, so both
workflows seed a stable one:

1. Add the secret `KEYSTORE_BASE64` with the base64 of the debug keystore used on your
   machine (the same file Gradle uses, usually `~/.android/debug.keystore`):

   ```bash
   # macOS
   base64 -i ~/.android/debug.keystore | pbcopy

   # Linux
   base64 -w0 ~/.android/debug.keystore
   ```

2. `ci.yml` and `release.yml` decode it to `$RUNNER_TEMP/debug.keystore` and export
   `DEBUG_KEYSTORE_FILE`, which `composeApp/build.gradle.kts` uses to configure the
   debug signing config explicitly. Debug signing uses the standard credentials
   (`androiddebugkey` / `android`), so no alias or password secrets are needed.
   (`KEYSTORE_BASE64` is decoded to a known path because AGP does not reliably pick up
   `$HOME/.android/debug.keystore` on CI runners.)
3. Both workflows then verify the seeded keystore's SHA-256 fingerprint
   (`FA:BB:6A:8E:57:47:2B:B6:54:01:7D:6C:2F:C5:20:C2:D1:06:A3:0A:88:71:33:BC:35:F0:4C:69:AE:0C:A8:0F`)
   and fail when it differs, so CI cannot silently produce APKs that won't update local installs.
4. The release workflow refuses to publish an Android artifact when neither
   `KEYSTORE_BASE64` nor `RELEASE_KEYSTORE_BASE64` is configured.

Notes:

- Use the exact same keystore locally; otherwise local and CI APKs still reject each
  other.
- Switching an installed app to a different signing key requires one uninstall (app data
  is lost). After switching, updates install in place.
- The `RELEASE_KEYSTORE_BASE64` secrets above remain available for signed release builds.

## macOS notes

The DMG is not code-signed or notarized yet, so Gatekeeper shows a warning on other
Macs. Signing requires an Apple Developer certificate and additional jpackage
configuration.
