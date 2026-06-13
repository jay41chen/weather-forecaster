# Weather Forecaster

Android weather app built with Clean Architecture, Jetpack Compose, and the OpenWeatherMap API. Includes a Socket.IO push server for real-time weather updates and alerts.

## Getting Started

### Prerequisites

1. **Android Studio** — download from [developer.android.com/studio](https://developer.android.com/studio) (Hedgehog 2023.1.1 or later). It bundles JDK 17 and the Android SDK, so no separate install is needed.
2. **OpenWeatherMap API key** — sign up for free at [openweathermap.org/api](https://openweathermap.org/api) and copy your key from the dashboard.
3. **Docker** (optional) — only needed if you want to run the real-time push server.

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

The app should launch and show a weather screen for the default city.

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

The server runs on port 3000. The app connects automatically when running on an Android emulator. For a physical device on the same WiFi, replace the server URL with your computer's local IP address.

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

## Architecture

### Module Dependency Graph

```
┌─────────────────────────────────────────────────────┐
│                        app                          │
│              (Composition Root / DI)                │
└──────┬──────────┬──────────┬──────────┬─────────────┘
       │          │          │          │
       ▼          ▼          ▼          ▼
  ┌─────────┐ ┌─────────┐ ┌────────┐ ┌────────┐
  │feature: │ │feature: │ │ core:  │ │ core:  │
  │weather  │ │citylist │ │  data  │ │   ui   │
  │(Screen +│ │(Screen +│ │(Adapters│ │(Shared │
  │   VM)   │ │   VM)   │ │  DB,   │ │Compose)│
  └────┬────┘ └────┬────┘ │  API)  │ └───┬────┘
       │          │       └───┬────┘     │
       │          │           │          │
       ▼          ▼           ▼          ▼
  ┌──────────────────────────────────────────┐
  │              core:domain                 │
  │           (Use Cases only)               │
  └──────────────────┬───────────────────────┘
                     │
                     ▼
  ┌──────────────────────────────────────────┐
  │                 core                     │
  │    (Port interfaces + Domain models)     │
  └──────────────────────────────────────────┘
```

All arrows point inward — outer layers depend on inner layers, never the reverse. Feature modules never import `core:data`; they depend only on interfaces from `core` and use cases from `core:domain`. Concrete implementations are wired via Hilt in the `app` module (composition root).

### Data Flow (UDF)

```
  ┌──────────────────────────────────────────────────────┐
  │                     Screen                           │
  │  collectAsState(uiState) ◄──── StateFlow<UiState>    │
  │                                       ▲              │
  │  onClick / onRefresh ─────► ViewModel │              │
  │                              │  _uiState.update()    │
  │                              ▼                       │
  │                           UseCase                    │
  │                              │                       │
  │                              ▼                       │
  │                     Repository (Port)                │
  │                      ┌───────┴───────┐               │
  │                      ▼               ▼               │
  │                 Remote API      Local DB (Room)       │
  └──────────────────────────────────────────────────────┘
```

- **State** flows down: `Repository → UseCase → ViewModel → Screen` via `StateFlow`
- **Events** flow up: `Screen → ViewModel` via function calls
- **Offline-first**: Use cases observe Room via `Flow`, trigger sync in parallel. Cached data renders immediately; fresh data replaces it when the API responds.

### Project Structure

```
app/                        # Composition root: Hilt modules, navigation, Application
core/                       # Port interfaces, domain models, logging
  core:data/                # Adapter implementations (Retrofit, Room, Socket.IO)
  core:domain/              # Use cases (business rules: TTL, dedup, sync)
  core:ui/                  # Shared Compose components (ErrorContent, LoadingContent)
feature/
  feature:weather/          # Weather detail screen + ViewModel (feature module)
  feature:citylist/         # City list / selection screen + ViewModel (feature module)
demo/                       # Standalone demo app for feature toggle testing
server/                     # Socket.IO push server (Node.js + Docker)
```

## CI/CD

| Workflow | Trigger | What it does |
|---|---|---|
| **PR Check** (`check.yml`) | Pull request → `main` / `develop` | Run unit tests, build debug APK |
| **Release** (`release.yml`) | Push tag `v*` | Build signed release APK + AAB (R8 minified), publish to GitHub Release |

**Downloading builds:**
- **Release build** — go to [Releases](../../releases) and download `app-release.apk` (direct install) or `app-release.aab` (for Play Store upload).
- **PR / release build** — go to [Actions](../../actions), click the workflow run, and download artifacts from the **Artifacts** section at the bottom of the page.

Release signing is configured via repository secrets. Without secrets, the build falls back to a debug signing key.

## Testing

```bash
./gradlew test
```

12 test files covering ViewModels, use cases, and repositories.
