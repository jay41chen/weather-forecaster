# Phase 3 Tasks

> Reference: [design.md](design.md) for full code snippets and contracts.
> Prerequisite: Phase 1 + Phase 2 complete and building.

---

## Task 1: Node.js Server

**Files (new):**
- `server/package.json`
- `server/index.js`
- `server/.env.example`
- `server/README.md`

**Steps:**
1. Create `package.json` with `express@^4.18`, `socket.io@^4.7`, `dotenv@^16.3`. Per design §2.2.
2. Create `index.js` (~120 lines) per design §2.3:
   - Load `OPEN_WEATHER_API_KEY`, `PORT` (default 3000), `POLL_INTERVAL_MS` (default 60000) from `.env`.
   - Express + Socket.IO server.
   - On `"connection"`: listen for `"subscribe"` event with `{ cities: [...] }`, store `socketId → [cityNames]` mapping.
   - Per-city polling loop: fetch OpenWeatherMap current weather, compare with `lastKnownState[city]` in-memory Map. Emit `"weather_update"` if temp/description/icon changed. Emit `"weather_alert"` if >5°C shift or weather category change.
   - On `"disconnect"`: remove mapping, stop polling for cities with 0 subscribers.
   - Console log connections, subscriptions, updates.
3. Create `.env.example` with `OPEN_WEATHER_API_KEY`, `PORT`, `POLL_INTERVAL_MS`.
4. Create `README.md` with setup instructions.

**Dependencies:** None (independent of Android).

**Acceptance:**
```bash
cd server && npm install && npm start
# Server starts on port 3000 without error
# Console shows "Server listening on port 3000"
```

---

## Task 2: `:core` — Realtime Interface & Alert Model

**Files (new):**
- `core/src/main/java/com/weather/core/model/WeatherAlert.kt`
- `core/src/main/java/com/weather/core/repository/WeatherRealtimeService.kt`

**Steps:**
1. Create `WeatherAlert` data class with `cityName`, `type`, `message`, `timestamp`. Per design §3.1.
2. Create `WeatherRealtimeService` interface with `observeWeatherUpdates(): Flow<CurrentWeather>`, `observeWeatherAlerts(): Flow<WeatherAlert>`, `suspend fun connect()`, `suspend fun disconnect()`, `suspend fun subscribeCities(cityNames: List<String>)`. Per design §3.2.

**Dependencies:** Phase 1 Task 2 (`:core` models exist). Can run in parallel with Task 1.

**Acceptance:**
```bash
./gradlew :core:compileDebugKotlin
```

---

## Task 3: `:core:data` — Socket.IO Implementation

**Files (new):**
- `core/data/src/main/java/com/weather/core/data/realtime/SocketIORealtimeServiceImpl.kt`

**Files (modify):**
- `core/data/build.gradle.kts` — add `io.socket:socket.io-client:2.1.0` dependency (exclude `org.json`).
- `core/data/build.gradle.kts` — add `BuildConfig.SOCKET_URL` field.
- `gradle.properties` — add `SOCKET_URL=http://10.0.2.2:3000`.
- `core/data/src/main/java/com/weather/core/data/di/DataModule.kt` — add `@Binds` for `WeatherRealtimeService`.

**Steps:**
1. Add socket.io-client dependency with `exclude(group = "org.json", module = "json")` — Android ships its own `org.json`.
2. Add `BuildConfig.SOCKET_URL` via `buildConfigField`. Per design §4.2.
3. Create `SocketIORealtimeServiceImpl` (`@Singleton`, `@Inject constructor` with `WeatherDao` + `LogPortFactory`). Per design §4.3:
   - `MutableSharedFlow` for updates and alerts.
   - `connect()`: create `IO.socket()` with reconnection options (5 attempts, 2s delay). Register listeners for `EVENT_CONNECT`, `EVENT_CONNECT_ERROR`, `"weather_update"`, `"weather_alert"`.
   - `"weather_update"` handler: parse `JSONObject` → `CurrentWeatherEntity` → `weatherDao.insertCurrentWeather()` → emit to SharedFlow.
   - `"weather_alert"` handler: parse `JSONObject` → `WeatherAlert` → emit to SharedFlow.
   - `disconnect()`: disconnect socket, clear listeners, null out reference.
   - `subscribeCities()`: emit `"subscribe"` event with `JSONObject { cities: JSONArray }`.
4. Add `@Binds` in `DataModule`: `SocketIORealtimeServiceImpl` → `WeatherRealtimeService`.

**Dependencies:** Task 2 (interface exists), Phase 2 Task 1 (LogPortFactory exists), Phase 1 Task 4 (WeatherDao exists).

**Acceptance:**
```bash
./gradlew :core:data:compileDebugKotlin
```

---

## Task 4: `:feature:weather` — Realtime Integration

**Files (modify):**
- `feature/weather/src/main/java/com/weather/feature/weather/WeatherUiState.kt`
- `feature/weather/src/main/java/com/weather/feature/weather/WeatherViewModel.kt`
- `feature/weather/src/main/java/com/weather/feature/weather/WeatherScreen.kt`

**Steps:**
1. Add `alertMessage: String? = null` to `WeatherUiState`. Per design §5.2.
2. `WeatherViewModel`: inject `WeatherRealtimeService`. Per design §5.1:
   - In `init`: if `FeatureFlag.SOCKET_IO_ENABLED` → launch `realtime.connect()`.
   - In selected city collector: if socket enabled → call `realtime.subscribeCities(listOf(cityName))`.
   - If `FeatureFlag.WEATHER_ALERTS_ENABLED` → collect `realtime.observeWeatherAlerts()` and update `uiState.alertMessage`.
   - Add `dismissAlert()` method.
   - In `onCleared()`: if socket enabled → launch `realtime.disconnect()`.
3. `WeatherScreen`: add `SnackbarHost` to `Scaffold`, `LaunchedEffect` on `uiState.alertMessage` to show snackbar and call `dismissAlert()`. Per design §5.3.

**Dependencies:** Task 3 (Socket.IO impl exists), Phase 2 Task 4 (feature toggle wired).

**Acceptance:**
```bash
./gradlew :feature:weather:compileDebugKotlin
```

---

## Task 5: Enable Feature Flags

**Files (modify):**
- `core/src/main/assets/feature_defaults.json`

**Steps:**
1. Set `socket_io_enabled` to `true`.
2. Set `weather_alerts_enabled` to `true`.

**Dependencies:** Task 4.

**Acceptance:**
```bash
./gradlew assembleDebug
```

---

## Task 6: End-to-End Verification

**No files to create.** Validation-only task.

**Steps:**
1. Start server: `cd server && npm start`.
2. `./gradlew installDebug` on emulator.
3. Select a city → Logcat shows `SocketIO: Connected` and `SocketIO: Subscribed [cities=London]`.
4. Server console shows subscription log.
5. Wait for poll interval (or set `POLL_INTERVAL_MS=10000` in `.env` for faster testing). If weather changed → UI updates without pull-to-refresh.
6. Test flag guard: set `socket_io_enabled` to `false` in `feature_defaults.json` → rebuild → app works without Socket.IO, no crash.
7. Test alert: manufacture a dramatic temp change on server (or wait for natural one) → snackbar appears.

**Dependencies:** Task 5.

**Acceptance:**
- Server logs show client connection and city subscription.
- Logcat shows `SocketIO` tagged logs for connect/subscribe/update.
- UI updates automatically when server pushes `weather_update`.
- Setting `socket_io_enabled=false` → no Socket.IO activity, no crash.
- `./gradlew assembleDebug` passes.
