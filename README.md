# DarkVault — Local Streaming Library

A Crunchyroll-inspired, dark/orange themed Android app that turns your **own local
video files** into a polished streaming-style library — no video player built in,
no online streaming, no cloud. Movies, TV shows, and anime you already have on
your device, organized with a premium UI.

## What it is / is not

- ✅ Local folder scanning (Storage Access Framework)
- ✅ Filename/folder-based media identification (Nova-style parser)
- ✅ Optional online metadata (posters, backdrops, overviews) via TMDB — pluggable, never required
- ✅ Room database (movies, series, seasons, episodes, profiles, watchlist, history)
- ✅ External playback only — taps launch your installed video player via `ACTION_VIEW`
- 🚫 No built-in video player
- 🚫 No streaming/hosting of any kind — everything is 100% local

## Project structure

```
app/src/main/java/com/darkjade/streamlib/
├── data/
│   ├── db/            Room entities, DAOs, database, converters
│   ├── scanner/        SAF folder scanner
│   ├── parser/          Filename/folder identification engine
│   ├── metadata/        Metadata provider abstraction + TMDB implementation
│   └── repository/     LibraryRepository, ProfileRepository, WatchRepository
├── player/               External player launcher (Intent-based)
├── work/                 WorkManager background scan worker
├── ui/
│   ├── theme/            Centralized design system (colors, type, spacing, shapes)
│   ├── components/     Reusable poster cards, rails, bottom bar, empty states
│   ├── navigation/     Nav graph + routes
│   └── screens/         Home, Browse, Details, My Lists, Search, Account, Settings
├── AppContainer.kt      Lightweight manual DI (no Hilt — simpler first build)
├── MainActivity.kt
└── StreamLibApp.kt
```

## Setting up metadata

TMDB metadata is already wired in — the API key lives in `local.properties`
(gitignored, never pushed to GitHub) and is injected at build time via
`BuildConfig.TMDB_API_KEY`, read in `StreamLibApp.onCreate()` into
`TmdbConfig.apiKey`. Nothing further to do for local builds.

**For CI-built APKs to also have posters/backdrops:** since `local.properties`
never reaches GitHub, add your TMDB key as a repository secret so the Actions
workflow can inject it at build time:

1. Repo → **Settings → Secrets and variables → Actions → New repository secret**
2. Name: `TMDB_API_KEY`, value: your TMDB v3 API key
3. Re-run the workflow — the debug APK from Actions will now have metadata enabled

If you ever need to rotate the key, just update `local.properties` locally
and the GitHub secret — no source file needs touching, since the key is never
hardcoded anywhere in the codebase.

## Building

This project was authored without local Android SDK/Gradle access, so it has **not**
been compiled locally. It ships with a GitHub Actions workflow
(`.github/workflows/android-build.yml`) that will:

1. Set up JDK 17 + Gradle 8.7
2. Generate the Gradle wrapper
3. Run lint + unit tests
4. Assemble a debug APK
5. Upload the APK as a workflow artifact

**To build:**
1. Push this repo to GitHub (or a branch)
2. Go to the **Actions** tab → the workflow runs automatically on push
3. Download the `DarkVault-debug-apk` artifact once it's green

**To build locally in Android Studio:**
1. Open the project folder in Android Studio (Koala/Ladybug or newer recommended)
2. Let Gradle sync — it will generate the wrapper automatically if missing
3. Run on a device/emulator with API 26+

If the CI build surfaces compile errors, they'll show up in the **Lint** or
**Run unit tests** / **Build debug APK** step logs — send those over and they can be
fixed in the next pass, same as previous projects.

## Using the app

1. Launch the app → Settings (or the empty-state "Add Folder" button) → pick a folder
   containing your Movies / TV Shows / Anime folders
2. The library scans in the background (WorkManager) — progress shows in Settings
3. Browse Home / Browse / Search, tap anything to open Details
4. Tap **Play** — Android's app chooser (or your default player) opens the file directly
5. Continue Watching / History / Watchlist update automatically from what you open

## Known limitations (by design, per spec)

- Playback position isn't tracked (playback happens outside the app) — only "last opened"
- No Room `Migration` objects yet; `fallbackToDestructiveMigration()` is used since the
  schema is still evolving. Swap in real migrations before a production release.
- TMDB key must be supplied by you (see above)
