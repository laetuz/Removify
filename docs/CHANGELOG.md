# Changelog

All notable changes to Removify are documented here. Format follows Keep a Changelog;
versions use semantic versioning.

## [0.2.0] - 2026-11-19

### Added

- Android folder picker on the Review screen using the Storage Access Framework
  (USB drive, SD card, or cloud), with recursive image/video scanning and folder display name.
- Remember the last picked folder across app restarts (SharedPreferences on Android,
  `java.util.prefs` on desktop), with automatic rescan on launch.
- Android video cards open in the system video player (`ACTION_VIEW`) instead of a dead tap.
- Photos / Videos filter chips for DCIM export, with live selected count and size.
- Shared MIME/extension classification helper with unit tests.

### Changed

- Review now uses a composition-scoped picker launcher on both platforms; the desktop
  still opens the native `JFileChooser` through the same callback.
- Deck finish dialog and session summary on Android now state that trashed items are not
  moved yet ("Marked for Trash" instead of "Moved to Trash").

### Fixed

- Review screen "Choose folder" was a no-op on Android (the platform picker returned `null`).
- DCIM export rejects internal `primary:DCIM` as a destination, preventing files from being
  copied onto themselves; previously saved invalid destinations are cleared on launch.

### Release

- Android artifacts: debug-signed APK (versionName `0.2.0`, versionCode from CI run number).
- Desktop artifact: `Removify-1.0.2.dmg` (jpackage requires a non-zero major version).

## [0.1.0] - 2026-10-21

### Added

- Initial Kotlin Multiplatform project for Android and desktop (JVM) with Compose
  Multiplatform, Navigation 3, Koin, Coil, and kotlinx.serialization.
- Adaptive Material3 shell with Review / Trash / Settings tabs and a dark theme.
- Desktop media workflow: folder picker, recursive scan, swipe deck
  (keep / trash / skip / undo, plus keyboard shortcuts), move-to-trash with a JSON
  manifest, restore and empty trash, and a session summary.
- Android DCIM export: SAF destination picker, SHA-256 verified copy, batch deletion of
  originals through the system confirmation dialog, progress and cancel support,
  persisted destination, and media-permission handling.
- CI on pushes and pull requests (Android tests + APK, macOS DMG); release workflow on
  `v*` tags producing a GitHub Release with DMG and Android artifacts, with optional
  signed APK/AAB when keystore secrets are configured.
- Documentation: proposal and CI setup guide.

### Notes

- Android gallery/MediaStore scanning for the swipe deck is not implemented yet.
- Android move-to-trash remains stubbed; desktop trash is fully functional.
- The macOS DMG is not code-signed or notarized.
