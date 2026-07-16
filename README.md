# Weather Forecaster

**English** | [繁體中文](README.zh-TW.md)

Android weather app built with Clean Architecture, Jetpack Compose, and the OpenWeatherMap API. Includes a Socket.IO push server for real-time weather updates and alerts.

## Features

- Current conditions, hourly (today) and 5-day forecasts per city
- City search via OpenWeatherMap geocoding; save, remove, and select cities (default city list seeded on first launch)
- Location-based city detection (Fused Location Provider)
- Offline-first: Room cache renders immediately; syncs when stale (30-minute TTL) with per-city request deduplication
- Pull-to-refresh
- Real-time weather updates and alert notifications pushed over Socket.IO (optional companion server)
- Runtime feature toggles with local JSON defaults
- Material 3 UI with light/dark theme (Jetpack Compose)

## Getting Started

### Prerequisites

1. **Android Studio** — download from [developer.android.com/studio](https://developer.android.com/studio) (Koala 2024.1.1 or later (required by AGP 8.5.2)). It bundles JDK 17 and the Android SDK, so no separate install is needed.
2. **OpenWeatherMap API key** — sign up for free at [openweathermap.org/api](https://openweathermap.org/api) and copy your key from the dashboard.
3. **Docker or Node.js 18+** (optional) — only needed if you want to run the real-time push server.

### Step 1 — Clone the project

```bash
git clone https://github.com/jay41chen/weather-forecaster.git
```

### Step 2 — Add your API key

Open the project folder and create a file named `local.properties` in the root (next to `build.gradle.kts`). Add this line, replacing the placeholder with your actual key:

```properties
OPEN_WEATHER_API_KEY=your_key_here
```

> `local.properties` is a standard Android config file that stays on your machine — it is git-ignored and will not be committed.

### Step 3 — Build & run

1. Open Android Studio → **File → Open** → select the project folder.
2. Wait for Gradle sync to finish (progress bar at the bottom).
3. Select a device or emulator from the toolbar, then click **Run ▶**.

The app should launch and show a weather screen for the default city. First time using an emulator? Create one via **Tools → Device Manager** in Android Studio.

<details>
<summary>Command-line alternative</summary>

```bash
cd weather-forecaster
./gradlew assembleDebug
# The APK is at app/build/outputs/apk/debug/app-debug.apk
```
</details>

### Step 4 — Push server (optional)

The Socket.IO server provides real-time weather push and alerts. The app works without it — skip this step if you just want to try the app.

```bash
cd server
cp .env.example .env
# Open .env in a text editor and paste your OpenWeatherMap API key

# Start with Docker:
docker compose up

# Or with Node.js (v18+):
npm install && npm start
```

The server runs on port 3000. The app connects automatically when running on an Android emulator. For a physical device on the same WiFi, replace the server URL with your computer's local IP address. The server URL the app uses is the `socket_url` key in `core/src/main/assets/feature_defaults.json`. See [server/README.md](server/README.md) for all connection options, including USB via `adb reverse tcp:3000 tcp:3000`.

<details>
<summary>Changing the server port</summary>

If port 3000 is already in use, you can change it in two places:

1. **Server** — set `PORT` in `server/.env` (e.g. `PORT=4000`), or pass it at startup: `PORT=4000 npm start`
2. **App** — update `socket_url` in `core/src/main/assets/feature_defaults.json` to match (e.g. `http://10.0.2.2:4000`). For a physical device, use your computer's local IP instead of `10.0.2.2`.

</details>

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Async**: Kotlin Coroutines + Flow
- **Architecture**: Clean Architecture (Ports & Adapters)
- **DI**: Hilt
- **Network**: Retrofit + OkHttp
- **Local Storage**: Room (offline cache) + DataStore (preferences)
- **Real-time**: Socket.IO
- **Location**: Fused Location Provider
- **Testing**: JUnit 4 + MockK + kotlinx-coroutines-test
- **Image Loading**: Coil
- **Serialization**: kotlinx-serialization (JSON)
- **Navigation**: Navigation Compose
- **Logging**: Timber (behind a `LogPort` abstraction)

## Architecture

### Module Dependency Graph

```
┌─────────────────────────────────────────────────────┐
│                        app                          │
│        (Application, navigation, DI glue)           │
│   depends on: all modules below                     │
└─────────────────────────────────────────────────────┘
┌──────────────────┐  ┌──────────────────┐
│ feature:weather  │  │ feature:citylist │
│  (Screen + VM)   │  │  (Screen + VM)   │
│  → core:domain, core:ui, core          │
└──────────────────┘  └──────────────────┘
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ core:domain  │ │  core:data   │ │   core:ui    │
│ (use cases)  │ │ (Retrofit,   │ │  (shared     │
│              │ │ Room, Socket)│ │  Compose)    │
│  → core      │ │  → core      │ │  → core      │
└──────────────┘ └──────────────┘ └──────────────┘
┌─────────────────────────────────────────────────────┐
│                        core                         │
│   (port interfaces + domain models + shared         │
│    config/log implementations) — no project deps    │
└─────────────────────────────────────────────────────┘
```

All arrows point inward — outer layers depend on inner layers, never the reverse. Feature modules never import `core:data`; they depend only on interfaces from `core` and use cases from `core:domain`. Adapter bindings live in `core:data`'s own Hilt modules (`DataModule`, `NetworkModule`, `DatabaseModule`, ...); the `app` module wires only the two swap-points that differ per app: `ConfigModule` (feature toggle source) and `LogModule` (log sink).

### Data Flow (UDF)

```
┌──────────────────────────────────────────────┐
│                   Screen                     │
└───────┬──────────────────────────▲───────────┘
        │ onClick / onRefresh      │ collectAsState(uiState)
        ▼                          │
┌──────────────────────────────────┴───────────┐
│                  ViewModel                   │
└───────┬──────────────────────────▲───────────┘
        │ invoke                   │ Flow<Resource<T>>
        ▼                          │
┌──────────────────────────────────┴───────────┐
│                  UseCase                     │
└───────┬──────────────────────────▲───────────┘
        │ sync / observe           │ Flow (Room)
        ▼                          │
┌──────────────────────────────────┴───────────┐
│              Repository (Port)               │
│        Remote API ──writes──▶ Room DB        │
└──────────────────────────────────────────────┘
```

- **Events** flow one way down the left side: `Screen → ViewModel → UseCase → Repository` via function calls.
- **State** flows one way back up the right side: `Room → Repository → UseCase → ViewModel → Screen` via `Flow`/`StateFlow`.
- **Offline-first**: Use cases observe Room via `Flow`, trigger sync in parallel. Cached data renders immediately; fresh data replaces it when the API responds.

### Project Structure

```
app/                        # Composition root: Hilt modules, navigation, Application
core/                       # Port interfaces, domain models, logging
  core/data/                # Adapter implementations (Retrofit, Room, Socket.IO)
  core/domain/               # Use cases (business rules: TTL, dedup, sync)
  core/ui/                   # Shared Compose components (ErrorContent, LoadingContent)
feature/
  feature/weather/           # Weather detail screen + ViewModel (feature module)
  feature/citylist/          # City list / selection screen + ViewModel (feature module)
demo/                       # Standalone demo app for feature-toggle and logging experiments; run with `./gradlew :demo:installDebug` or the `demo` run configuration.
server/                     # Socket.IO push server (Node.js + Docker)
```

## CI/CD

| Workflow | Trigger | What it does |
|---|---|---|
| **PR Check** (`check.yml`) | Pull request → `main` / `develop` | Run unit tests, build debug APK |
| **Release** (`release.yml`) | Push tag `v*` | Build signed release APK + AAB (R8 minified), publish to GitHub Release |

**Downloading builds:**
- **Tagged releases** — go to [Releases](../../releases) and download `app-release.apk` (direct install) or `app-release.aab` (Play Store upload).
- **Any other run (including PRs)** — go to [Actions](../../actions), open the workflow run, and download from the **Artifacts** section at the bottom.

Release signing is configured via repository secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). When they are absent, the build falls back to the debug signing key, so tag builds still succeed.

## Testing

```bash
./gradlew test
```

12 test files covering ViewModels, use cases, and repositories (see [TOOLS.md](TOOLS.md) for how the suite grew).

## AI-Assisted Development

This project was built with [Claude Code](https://docs.anthropic.com/en/docs/claude-code)
using a structured Think → Do workflow: findings are discussed and
prioritized before any code is written, then implemented as minimal,
focused commits. This covered a full multi-dimension code review
(correctness, concurrency, architecture, test coverage), race condition
fixes, Clean Architecture refactoring.

See [TOOLS.md](TOOLS.md) for the workflow and a findings summary.
