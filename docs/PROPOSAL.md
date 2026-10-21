# Removify — Product & Technical Proposal

> **Status:** Draft v1 — for concept finalization
> **Platforms:** Android + Desktop (macOS / Windows / Linux) via Kotlin Multiplatform
> **Base architecture:** `NeostoreAdmin` (single-module CMP) + media/platform patterns from `OrpheumUploader`

---

## 1. Concept

**Removify** is a local-first media cleanup app. It turns reviewing thousands of photos and videos into a fast, Tinder-style triage session:

- One media item per card, full-bleed preview.
- **Swipe left → Trash**, **swipe right → Keep**, **swipe up → Skip**.
- Decisions are held in memory during the session; nothing touches the filesystem until the user confirms.
- On finish, trashed items are **moved to a Trash folder** (not permanently deleted) — fully restorable, until the user explicitly empties the trash.
- A summary shows what was kept, what moved to trash, and how much space an "Empty Trash" would free.

No accounts, no network, no analytics. Everything runs on-device.

### Why swipe?

Large collections (10k+ WhatsApp/gallery items) are slow to clean up with grid multi-select. Swipe triage is one-handed, fast, and requires no selection state. Desktop gets the same flow with keyboard shortcuts (←/→/↑), so both platforms share one mental model.

---

## 2. Goals & Non-goals

### Goals (v1)

| # | Goal |
|---|------|
| G1 | Load media from the Android gallery (MediaStore) or any desktop folder |
| G2 | Swipe/keys triage: Keep / Trash / Skip, with Undo |
| G3 | Safe **move-to-trash** flow with restore and empty-trash |
| G4 | Session summary with counts and reclaimable bytes |
| G5 | Adaptive UI (phone / tablet / desktop) with one shared codebase |
| G6 | Local persistence of settings, trash manifest, and session history |
| G7 | Release artifacts: APK, DMG, MSI, DEB (same CI pattern as the reference projects) |

### Non-goals (v1)

- Backend, accounts, auth, sync, telemetry
- Duplicate detection, blur/quality scoring, ML ranking
- Photo/video editing
- Cloud backup or cloud trash
- iOS target
- Undo *after* a session is committed (restore from Trash covers this)

---

## 3. Interaction model

### 3.1 Gestures & keys

| Input | Action | Card feedback |
|-------|--------|---------------|
| Swipe **left** / `←` | **Trash** | Red tint + "TRASH" stamp rotates in |
| Swipe **right** / `→` | **Keep** | Green tint + "KEEP" stamp |
| Swipe **up** / `↑` | **Skip** (defer to end of session) | Blue/gray tint + "SKIP" stamp |
| Swipe **down** / `↓` | **Undo** last decision | Previous card springs back |
| `Ctrl`/`Cmd` + `Z` | Undo last decision | — |
| Tap on video card | Play/pause inline | Play icon overlay |
| Tap on image card | Toggle metadata overlay (name/date/size) | — |
| `Space` (desktop) | Pause/resume video | — |

Also provide on-screen buttons (✕ Trash, ↑ Skip, ✓ Keep, ↺ Undo) for accessibility and mouse users.

### 3.2 Swipe mechanics

- Commit threshold: 30% of card width **or** fling velocity > 800 dp/s.
- Card rotates up to ±12° while dragging, scales 0.95 on the stack behind.
- Below threshold → spring back.
- Next card is preloaded (image already decoded, video first frame ready).
- Undo stack holds the last 100 decisions for the session.
- Skipped items are appended to a "Skipped" queue at the end of the deck (one re-review pass, then they remain undecided).

### 3.3 During the session

- Progress: `142 / 3,208` + thin progress bar.
- Counters: kept / trashed / skipped (live).
- "Finish" is always enabled; a confirmation dialog appears:
  > *Move 214 items (1.2 GB) to Trash?* → **Move to Trash** / **Cancel**
- Skipped items are ignored at finish (default) or can be re-queued once.

---

## 4. Trash semantics (explicit)

**Trash = move, not delete.** The original file is removed from its location only after it has been copied/moved into a Removify-managed trash, and every entry is recorded in a manifest so it can be restored.

### 4.1 Desktop

- Trash location: platform app-support directory
  - macOS: `~/Library/Application Support/Removify/trash/`
  - Windows: `%APPDATA%\Removify\trash\`
  - Linux: `~/.local/share/Removify/trash/`
- Move = atomic rename when source and trash are on the same volume; copy+delete across volumes.
- Trash filename: `{timestamp}_{originalName}` to avoid collisions.
- Manifest `trash.json` stores: id, originalPath, trashedPath, name, type, sizeBytes, trashedAt, sessionId.
- **Restore** moves the file back to `originalPath` (recreates parent dirs; if a file already exists, restores as `name (restored).ext`).
- **Empty Trash** permanently deletes trashed files after a confirmation dialog.

### 4.2 Android

- Trash album on shared storage, visible in the gallery:
  - Images → `Pictures/Removify/Trash/`
  - Videos → `Movies/Removify/Trash/`
- Commit flow:
  1. Insert copy into MediaStore with `RELATIVE_PATH` above, `IS_PENDING=1`, write bytes, set `IS_PENDING=0`.
  2. Request original deletion with `MediaStore.createDeleteRequest(...)` (single system dialog for the batch, API 30+ — no special permissions).
  3. On success, record manifest entry in app storage.
- If the user cancels the system dialog, no originals are deleted; the newly created trash copies are rolled back.
- **Restore**: insert back into the original `RELATIVE_PATH` (e.g. `DCIM/Camera`), then delete the trash copy.
- **Empty Trash**: `createDeleteRequest` on trash items → permanent delete.
- App-private manifest: `filesDir/trash.json` (mirrors desktop schema).

### 4.3 What "Undo" means

Undo only edits the in-memory decision list during a session. It never needs to reverse filesystem operations, because file operations happen only at session commit.

---

## 5. Technical stack

| Area | Choice | Source of pattern |
|------|--------|-------------------|
| Language / targets | Kotlin **2.4.10**, `androidTarget()` + `jvm()` (JVM 17) | NeostoreAdmin |
| UI | Compose Multiplatform **1.11.1**, Material3 | NeostoreAdmin |
| Build | Gradle **8.14.3**, AGP **8.9.3**, version catalog, `composeHotReload` | NeostoreAdmin |
| SDK | compileSdk/targetSdk **36**, minSdk **30** | NeostoreAdmin |
| Navigation | JetBrains **Navigation 3** (`navigation3-ui` 1.1.1) | both |
| DI | **Koin 4.0.1** (`koin-core`, `koin-compose`, `koin-compose-viewmodel`) | both |
| Images | **Coil 3** (`coil-compose`) — local `File` / `content://` URIs | both |
| Serialization | kotlinx.serialization JSON | both |
| Android media | MediaStore, **Media3 ExoPlayer** for inline video | new |
| Desktop video | JavaFX `MediaView` inside `SwingPanel` (fallback: system player) | OrpheumUploader (JavaFX precedent) |
| Desktop pickers | AWT `FileDialog`/`JFileChooser` + OS drag-and-drop | OrpheumUploader |
| Persistence | JSON files + `expect/actual` app-dirs / SharedPreferences | simplified TokenStorage pattern |
| CI | GitHub Actions → APK + DMG + MSI + DEB on `v*` tags | both |
| Tests | kotlin-test (commonTest), JUnit4 (jvmTest) | NeostoreAdmin |

**Dropped vs references:** Ktor client, bearer auth, token storage, `AuthView` gate, network image loading, pagination APIs (replaced by local cursors).

---

## 6. Architecture

Same house style as both reference projects:

```
Composable View ──> ViewModel (StateFlow<UiState>) ──> Repository interface (domain)
                                                          │
                                                          ▼
                                            RepositoryImpl (data)
                                                          │
                              MediaStore / FileSystem / JSON manifests
```

- **MVVM + unidirectional data flow.** One `XxxViewModel` + `XxxUiState` per screen.
- **Repositories return `kotlin.Result<T>`**; ViewModels write `isLoading` / `errorMessage` into state.
- **No use-case layer** (consistent with references; the session engine lives in a small domain service `ReviewSessionEngine` used by the ViewModel).
- **`expect/actual` platform layer** for: media scanning, file operations (trash/restore), app directories, video player, folder picker, key handling.
- **Koin modules:** `appModule` (repos, VMs, engine) + `expect fun platformModule(): Module`.
- **Adaptive shell:** `BoxWithConstraints`, `isCompact = maxWidth < 600.dp` → bottom bar / side rail; reference `MainView` pattern.

### 6.1 Layers

| Layer | Contents |
|-------|----------|
| `ui/` | `App.kt` root, `MainView` shell, navigation, theme/components, `feature/*` screens + VMs |
| `domain/` | `model/*` (MediaItem, decisions…), `repository/*` interfaces, `service/ReviewSessionEngine` |
| `data/` | `MediaRepositoryImpl`, `TrashRepositoryImpl`, `SettingsRepositoryImpl`, `SessionHistoryRepositoryImpl`, JSON stores |
| `platform/` | expect declarations + android/jvm actuals |
| `di/` | `AppModules.kt`, `PlatformModule.kt` / `.android.kt` / `.jvm.kt` |
| `utils/` | format helpers (bytes, dates), constants |

---

## 7. Module & package layout

Single Gradle module `:composeApp` (NeostoreAdmin style), package root **`id.neotica.removify`** *(assumption — easy to rename)*.

```
Removify/
├── settings.gradle.kts              # include(":composeApp")
├── build.gradle.kts                 # plugins apply false
├── gradle/libs.versions.toml
├── gradle.properties                # config cache, build cache
├── docs/PROPOSAL.md
├── Images/  vid/                    # desktop test data (existing)
└── composeApp/
    ├── build.gradle.kts             # android + jvm targets, desktop packaging
    └── src/
        ├── commonMain/kotlin/id/neotica/removify/
        │   ├── App.kt
        │   ├── di/AppModules.kt
        │   ├── di/PlatformModule.kt          # expect
        │   ├── domain/
        │   │   ├── model/MediaItem.kt
        │   │   ├── model/ReviewSession.kt
        │   │   ├── model/TrashEntry.kt
        │   │   ├── model/Settings.kt
        │   │   ├── model/MediaSource.kt
        │   │   ├── repository/MediaRepository.kt
        │   │   ├── repository/TrashRepository.kt
        │   │   ├── repository/SettingsRepository.kt
        │   │   └── service/ReviewSessionEngine.kt
        │   ├── data/
        │   │   ├── MediaRepositoryImpl.kt     # delegates to platform scanner
        │   │   ├── TrashRepositoryImpl.kt
        │   │   ├── SettingsRepositoryImpl.kt
        │   │   └── store/JsonFileStore.kt
        │   ├── platform/                       # expect declarations
        │   │   ├── MediaScanner.kt
        │   │   ├── TrashFileOps.kt
        │   │   ├── AppDirs.kt
        │   │   ├── VideoPlayer.kt
        │   │   ├── FolderPicker.kt
        │   │   ├── PlatformKeyEvents.kt
        │   │   └── PlatformPermissions.kt
        │   ├── ui/
        │   │   ├── MainView.kt
        │   │   ├── components/ (NeoCard, NeoColor, ButtonBasic, SwipeCard, ProgressHeader…)
        │   │   ├── navigation/ (Screen, NavigationItem, AppNavigationBar, AppNavigationRail)
        │   │   └── feature/
        │   │       ├── source/                 # SourceView + SourceViewModel
        │   │       ├── setup/                  # SessionSetupView + VM
        │   │       ├── deck/                   # SwipeDeckView + VM (core)
        │   │       ├── summary/                # SessionSummaryView + VM
        │   │       ├── trash/                  # TrashView + VM
        │   │       └── settings/               # SettingsView + VM
        │   └── utils/ (ByteFormat.kt, DateFormat.kt, MediaExtensions.kt)
        ├── androidMain/kotlin/id/neotica/removify/
        │   ├── MainActivity.kt
        │   ├── RemovifyApplication.kt
        │   ├── data/local/AndroidSettingsStorage.kt
        │   ├── di/PlatformModule.android.kt
        │   └── platform/ (MediaScanner.android.kt, TrashFileOps.android.kt,
        │                  AppDirs.android.kt, VideoPlayer.android.kt,
        │                  FolderPicker.android.kt, PlatformPermissions.android.kt,
        │                  PlatformKeyEvents.android.kt [no-op])
        ├── jvmMain/kotlin/id/neotica/removify/
        │   ├── main.kt
        │   ├── data/local/DesktopSettingsStorage.kt
        │   ├── di/PlatformModule.jvm.kt
        │   └── platform/ (…*.jvm.kt, PlatformKeyEvents.jvm.kt [AWT])
        └── commonTest/kotlin/…    # engine + formatting tests
```

---

## 8. Domain model

```kotlin
@Serializable
enum class MediaType { IMAGE, VIDEO }

@Serializable
data class MediaItem(
    val id: String,             // content:// id on Android, absolute path on desktop
    val name: String,
    val type: MediaType,
    val sizeBytes: Long,
    val dateTakenEpochMs: Long,
    val durationMs: Long? = null,       // videos only
    val bucket: String? = null,         // Android RELATIVE_PATH / desktop folder
)

enum class SwipeDecision { KEEP, TRASH, SKIP }

@Serializable
data class ReviewSession(
    val id: String,
    val startedAtEpochMs: Long,
    val source: MediaSource,
    val decisions: Map<String, SwipeDecision>,   // mediaItemId -> decision
    val finishedAtEpochMs: Long? = null,
)

@Serializable
data class TrashEntry(
    val id: String,
    val originalPath: String,
    val trashedPath: String,
    val name: String,
    val type: MediaType,
    val sizeBytes: Long,
    val trashedAtEpochMs: Long,
    val sessionId: String,
)

@Serializable
data class Settings(
    val includeImages: Boolean = true,
    val includeVideos: Boolean = true,
    val sortNewestFirst: Boolean = true,
    val lastDesktopFolder: String? = null,
    val skippedRequedAtEnd: Boolean = true,
)
```

`MediaSource` is serializable: `MediaSource.Gallery` (Android) or `MediaSource.Folder(path)` (desktop).

### 8.1 `ReviewSessionEngine` (pure, testable)

- `next(): MediaItem?` — deck order, skipped appended at end
- `decide(item, decision)` + undo stack (cap 100)
- `undo(): MediaItem?`
- `summary(): SessionSummary` — counts + reclaimable bytes
- No I/O; fully unit-tested in `commonTest`.

---

## 9. Navigation graph

```kotlin
@Serializable sealed interface Screen : NavKey {
    @Serializable data object Source : Screen
    @Serializable data object Setup : Screen
    @Serializable data object Deck : Screen
    @Serializable data object Summary : Screen
    @Serializable data object Trash : Screen
    @Serializable data object Settings : Screen
}
```

Flow (Navigation 3 `NavBackStack` + `NavDisplay`, as in references):

```
[Permission/Source] ──pick──▶ [SessionSetup] ──start──▶ [SwipeDeck]
                                                          │ finish
                                                          ▼
                                                     [Summary] ──▶ Trash / New session
Top-level tabs: Review · Trash · Settings   (tab switch = backStack.clear(); add(target))
```

- Start destination: `Source`.
- `Deck` blocks back navigation while a session is active (confirm discard dialog on Back/Escape).
- `Summary` is replaced (not stacked) when starting a new session.
- Android Back / Desktop Escape behavior mirrors NeostoreAdmin's `installPlatformKeyDispatcher`.
- No auth gate (offline app) — `Source` is the gate: it also handles the permission rationale.

---

## 10. Core flows

### 10.1 Load (Android)

1. Request `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` (API 33+) or `READ_EXTERNAL_STORAGE` (API 30–32).
2. On Android 14+ partial access (`READ_MEDIA_VISUAL_USER_SELECTED`), offer "Select more photos" in Source.
3. Query MediaStore (`Images.Media` + `Video.Media`, merged), sorted by `DATE_TAKEN DESC` (fallback `DATE_MODIFIED`).
4. Paged retrieval (cursor, page size 100); Coil loads thumbnails lazily from `content://` URIs.

### 10.2 Load (Desktop)

1. Folder picker (`JFileChooser` directory mode) + drag-and-drop folder onto the window.
2. Recursive walk, case-insensitive extensions:
   - Images: `jpg jpeg png webp gif bmp heic`
   - Videos: `mp4 mov m4v mkv webm avi`
3. Sort by `lastModified` desc; skip hidden files and the app's own trash dir.
4. Thumbnails via Coil `File` URIs; unsupported codecs (e.g. HEIC without Skia support) fall back to a type icon.

### 10.3 Swipe session

1. Setup: toggles (images/videos), optional date range, count preview → **Start**.
2. Deck shows current card; decisions recorded via engine.
3. **Finish** → confirmation dialog with counts + total bytes.
4. Commit: `TrashRepository.moveToTrash(items)` → platform trash flow (Section 4) → progress sheet.
5. Summary: kept/trashed/skipped counts, total moved bytes, "Empty Trash" CTA, "Start new session".

Failure handling: any item that fails to move is reported ("12 items could not be moved") and stays on-device; the session is still recorded.

### 10.4 Trash

- List/grid with thumbnails, size, trashed date, source session.
- **Restore** (single / all) with conflict-safe naming.
- **Empty Trash** (permanent, confirmation, reports freed bytes).
- Trash is the only place where permanent deletion happens.

### 10.5 Settings

- Include images / videos, default sort, skipped-item behavior.
- Desktop: default folder, "remember last folder".
- Data: session history (date, counts), trash manifest size, app version, licenses.

---

## 11. Persistence

- **No Room/DataStore in v1.** JSON files written atomically (temp file + rename) through `JsonFileStore`.
- Locations:
  - Desktop: app-support dir (`trash.json`, `settings.json`, `sessions.json`, `trash/`).
  - Android: `filesDir/` (`trash.json`, `sessions.json`); settings in SharedPreferences (mirrors reference `TokenStorage` pattern); media in MediaStore trash albums.
- Trash manifest is the source of truth for restore; corrupt/missing manifest → Trash screen shows what it can and offers cleanup, never blind-deletes.
- Session history capped at the last 50 sessions.

---

## 12. UI / UX

### Theme

Copy the NeoColor dark palette:

| Token | Value |
|-------|-------|
| Background | `0xFF151A3B` |
| Primary | `0xFF5D6EE0` |
| Card | `0xFF3A4486` |
| Negative (trash) | golden-ratio red from references |
| Positive (keep) | add `0xFF3FBF7F` |
| Skip | add `0xFF8A8FA8` |

Material3 `MaterialTheme` wrapper; shared `NeoCard`, `ButtonBasic` components.

### Swipe card

- 24dp rounded card, full media preview, bottom gradient scrim.
- Metadata row: filename, date, size, type badge + duration.
- Stamps (`KEEP` / `TRASH` / `SKIP`) fade/rotate with drag progress.
- Action row beneath the stack: Undo · Trash · Skip · Keep.
- Video card: centered play button; tap → inline player (Media3 / JavaFX); tap again to pause; auto-pause when card leaves.

### Adaptive layout

| Width | Layout |
|-------|--------|
| < 600dp | bottom navigation bar; card fills width; action buttons below |
| ≥ 600dp | side navigation rail; card centered max 720dp; keyboard hint footer |
| ≥ 1200dp | rail + right-side "Up next" strip (next 3 thumbnails) |

### States

- Loading (shimmer card), permission rationale, empty folder, all-reviewed ("You're all caught up"), error banner with retry.

---

## 13. Platform matrix

| Concern | Android | Desktop |
|---------|---------|---------|
| Media listing | MediaStore (images + videos, merged cursor) | Recursive file walk |
| Permissions | `READ_MEDIA_IMAGES/VIDEO` (33+), `READ_EXTERNAL_STORAGE` (30–32), partial-access handling (14+) | None |
| Thumbnails | Coil from `content://` | Coil from `File` |
| Video playback | Media3 ExoPlayer inline | JavaFX `MediaView` in `SwingPanel`; fallback "Open in system player" |
| Trash location | `Pictures|Movies/Removify/Trash` (MediaStore) | App-support `trash/` |
| Original removal | `MediaStore.createDeleteRequest` (batch system dialog) | rename/copy+delete |
| Restore | Insert back into original `RELATIVE_PATH` | Move back to manifest path |
| Permanent delete | `createDeleteRequest` on trash items | `File.delete()` after confirm |
| Picker | Gallery source (no SAF needed) | Folder chooser + OS drag-and-drop |
| Keyboard | Back = pop/discard confirm | ←/→/↑/↓, Ctrl+Z, Space, Escape |
| Manifest | `filesDir/trash.json` | app-support `trash.json` |

---

## 14. Testing

| Suite | Coverage |
|-------|----------|
| `commonTest` | `ReviewSessionEngine` (order, skip requeue, undo cap/underflow, summary math), extension classification, date-range filter, byte formatting |
| `jvmTest` | Trash move/restore/empty on temp dirs, manifest round-trip, atomic write, conflict naming |
| Manual (Android) | Permission flows 30/33/34+, partial access, cancel delete dialog, rotate/process-death mid-session, 14k-item load |
| Manual (Desktop) | Folder pick/drag-drop, HEIC fallback, same-volume and cross-volume move, window resize breakpoints |

Desktop test data available in-repo: `Images/` (13,060 images) and `vid/` (1,064 videos).

---

## 15. Milestones

| Milestone | Scope | Acceptance |
|-----------|-------|------------|
| **M0 Scaffold** | Gradle/CMP setup, Koin, theme, shell + Nav3, CI workflow | App launches on Android + desktop; empty tabs navigate |
| **M1 Sources** | Android MediaStore scanner + permissions; desktop folder scan; Coil thumbnails | Correct counts/sorting for `Images/` + `vid/` test data; paged loading |
| **M2 Deck** | Swipe gestures, keys, stamps, undo, tap-to-play video, progress | Triage 100+ items with no dropped decisions; undo/redo consistent |
| **M3 Trash** | Move/restore/empty both platforms, manifests, rollback on cancel | MD5-identical file after move→restore; empty frees space; cancel leaves originals |
| **M4 Summary + Settings** | Summary screen, session history, settings persistence, skipped requeue | Restart preserves settings/history; counts reconcile |
| **M5 Polish + Release** | Empty/error states, adaptive tweaks, tests green, release workflow | Tag `v0.1.0` produces APK + DMG + MSI + DEB |

---

## 16. Reference mapping (what comes from where)

| Removify concern | Borrowed from | Reference file |
|------------------|---------------|----------------|
| Single-module CMP build, catalog | NeostoreAdmin | `composeApp/build.gradle.kts`, `gradle/libs.versions.toml` |
| Adaptive shell + tabs | NeostoreAdmin | `ui/MainView.kt`, `ui/navigation/*` |
| Nav3 graph & escape/back handling | NeostoreAdmin | `App.kt`, `ui/navigation/Screen.kt` |
| Koin modules + expect/actual DI | both | `di/AppModules.kt`, `di/PlatformModule.*` |
| Repository + `Result<T>` + UiState VMs | both | `ui/feature/*/…ViewModel.kt` |
| Theme + cards + dark palette | both | `ui/components/NeoColor.kt`, `NeoCard.kt`, `ButtonBasic.kt` |
| Key dispatcher (AWT actual, Android no-op) | NeostoreAdmin / OrpheumUploader | `platform/PlatformKeyEvents.*` |
| Coil singleton loader per platform | both | `MainActivity.kt`, `main.kt` |
| Desktop packaging + CI release | both | `build.gradle.kts`, `.github/workflows/release.yml` |
| Folder picker + drag-and-drop | OrpheumUploader | `ui/feature/upload/PlatformUploadView.jvm.kt` |
| JavaFX media playback on desktop | OrpheumUploader | `data/local/DesktopAudioPlayer.kt`, `platform/AudioMetadata.jvm.kt` |
| Android cache-copy / URI handling | OrpheumUploader | `AndroidTokenStorage` neighbors, upload platform code |

Known warts to *not* copy: dual Ktor clients, unused refresh token, god-composable `App.kt`, orphaned screens, hardcoded role gates.

---

## 17. Risks & open questions

| Risk | Mitigation |
|------|------------|
| Android 14 partial photo access hides most media | Detect `READ_MEDIA_VISUAL_USER_SELECTED`; explain + offer re-selection |
| MediaStore batch delete dialog UX (Android) | One dialog per session commit; clear copy beforehand; rollback on cancel |
| 14k-item deck performance | Cursor paging, prefetch next 2 thumbnails, recycle players |
| HEIC/HEVC thumbnails on desktop | Coil/Skia first, type-icon fallback; optional ImageIO plugins later |
| JavaFX runtime bloat in desktop packages | Accept for v1 (OrpheumUploader precedent); revisit `vlcj` if needed |
| Cross-volume desktop move | Copy+delete with progress; only delete source after verified copy |
| Files modified externally mid-session | Move/restore failures reported per item; never crash the batch |
| Trash grows unbounded | Show trash size in Settings; prompt when > 5 GB |

**Assumptions:** app id `id.neotica.removify`, display name "Removify", English-only UI, dark theme only for v1.

---

## 18. Decision log

| Date | Decision | By |
|------|----------|----|
| 2026-10-21 | Swipe mapping: ← Trash, → Keep, ↑ Skip, ↓/Ctrl+Z Undo | concept |
| 2026-10-21 | Trash = move-to-folder with manifest (restorable), never silent delete | concept |
| 2026-10-21 | Fully local/offline, no backend | concept |
| 2026-10-21 | Base = NeostoreAdmin single module; media/platform code from OrpheumUploader | concept |
| 2026-10-21 | Sources: Android full gallery + desktop folder picker | concept |
| 2026-10-21 | Videos: thumbnail + tap-to-play inline | concept |
