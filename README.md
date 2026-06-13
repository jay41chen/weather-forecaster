# Weather Forecaster

Android weather app built with Clean Architecture, Jetpack Compose, and the OpenWeatherMap API. Includes a Socket.IO push server for real-time weather updates and alerts.

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34 (compile) / min SDK 26
- Docker (optional, for the push server)

## Setup

### 1. API Key

This app requires an [OpenWeatherMap](https://openweathermap.org/api) API key.

Add the following to `local.properties` in the project root (create it if it doesn't exist):

```properties
OPEN_WEATHER_API_KEY=<your_api_key>
```

The same key is needed for the push server — see step 3.

### 2. Build & Run the App

```bash
# Clone
git clone https://github.com/jay41chen/weather-forecaster.git
cd weather-forecaster

# Add your API key to local.properties (see step 1)

# Build (command line)
./gradlew assembleDebug

# Or open in Android Studio → Run on emulator / device
```

### 3. Push Server (optional)

The Socket.IO push server provides real-time weather updates and weather alerts. The app works without it (feature toggle `socket_io_enabled` can be set to `false`), but for the full experience:

```bash
cd server
cp .env.example .env
# Edit .env and add your OpenWeatherMap API key

# Option A: Docker
docker compose up

# Option B: Node.js
npm install && npm start
```

**Client connection:**
- Emulator: `http://10.0.2.2:3000` (default)
- Physical device (same WiFi): replace with your machine's LAN IP
- Physical device (USB): `adb reverse tcp:3000 tcp:3000`, then `http://localhost:3000`

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

## Testing

```bash
# Run all unit tests
./gradlew test

# Run tests for a specific module
./gradlew :core:domain:test
./gradlew :feature:weather:test
```

12 test files covering ViewModels, use cases, and repositories.
