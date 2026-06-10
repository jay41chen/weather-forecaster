# Weather Forecast App — Phase 3: Socket.IO Real-Time Updates

> **Prerequisite:** Phase 1 + Phase 2 complete and building.
> **Goal:** Real-time weather push via Socket.IO. Simple Node.js server + Android client.
> **Time budget:** ~2 hours
> **Done criteria:** Server pushes weather update → Android UI updates automatically without user interaction.

---

## 0. Design Decisions (post-review)

1. **Interface keeps dual suspend** — `suspend fun connect()` / `suspend fun disconnect()`. Impl-agnostic; future backends (WebSocket, gRPC) may need suspend.
2. **Impl owns its CoroutineScope** — `CoroutineScope(SupervisorJob() + Dispatchers.IO)`. ViewModel is a notifier only; connect/disconnect execute on impl's scope, not caller's.
3. **ViewModel `onCleared()`** — calls `realtime.disconnect()` as a notification. Impl handles the actual teardown internally, so cancellation of `viewModelScope` is irrelevant.
4. **SnackbarHost shared** — Alert snackbar reuses the existing `SnackbarHostState` from Phase 2 error handling. No second host.
5. **Socket.IO client 2.1.2** — protocol v4, compatible with server `socket.io@^4.7`. No `allowEIO3` needed.
6. **LogPort calls** — Use `logFactory.create("SocketIO")` at construction; subsequent calls are `log.i("message")` / `log.d("message", mapOf(...))`, not `log.i("tag", "message")`.
7. **SOCKET_URL in `gradle.properties`** — compile-time `BuildConfig` field, not remote config. For production, URL should come from remote config / `FeatureTogglePort` for dynamic switching. For this demo, build-time default (`http://10.0.2.2:3000`) is sufficient — same pattern as API keys in `local.properties`.

---

## 1. What Changes

| Location | Change Type | Description |
|---|---|---|
| `server/` | **CREATE** | Node.js + Socket.IO server (~120 lines) |
| `:core` | **ADD** | `WeatherRealtimeService` interface in `repository/` |
| `:core` | **ADD** | `WeatherAlert` model in `model/` |
| `:core` | **ADD** dep | `io.socket:socket.io-client` |
| `:core:data` | **ADD** | `SocketIORealtimeServiceImpl` |
| `:core:data` | **ADD** config | `BuildConfig.SOCKET_URL` |
| `:feature:weather` | **MODIFY** | VM connects realtime, guarded by `SOCKET_IO_ENABLED` flag |
| `feature_defaults.json` | **MODIFY** | Set `socket_io_enabled` to `true` |

---

## 2. Server (`server/`)

### 2.1 Files

```
server/
├── package.json
├── index.js
├── .env.example
└── README.md
```

### 2.2 package.json

```json
{
  "name": "weather-push-server",
  "version": "1.0.0",
  "scripts": { "start": "node index.js" },
  "dependencies": {
    "express": "^4.18",
    "socket.io": "^4.7",
    "dotenv": "^16.3"
  }
}
```

### 2.3 index.js Behavior

```
1. Load OPEN_WEATHER_API_KEY from .env
2. Start Express + Socket.IO on port 3000
3. On client "connection":
   a. Listen for "subscribe" event → { cities: ["London", "Tokyo"] }
   b. Store mapping: socketId → [cityNames]
   c. Start polling for each city (if not already polling)
4. Polling loop (per city, every 60 seconds):
   a. fetch(`https://api.openweathermap.org/data/2.5/weather?q=${city}&appid=${key}&units=metric`)
   b. Compare with lastKnownState[city] (in-memory Map)
   c. If temp/description/icon changed → emit "weather_update" to all subscribers:
      {
        cityName: "London",
        temperature: 19.2,
        feelsLike: 17.8,
        description: "light rain",
        iconCode: "10d",
        humidity: 78,
        windSpeed: 4.1,
        pressure: 1010,
        timestamp: 1700000060
      }
   d. If dramatic change (>5°C shift OR weather category change) → also emit "weather_alert":
      {
        cityName: "London",
        type: "TEMPERATURE_DROP",
        message: "Temperature dropped 6°C in the last hour",
        timestamp: 1700000060
      }
5. On "disconnect" → remove mapping, stop polling for cities with 0 subscribers
6. Log connections, subscriptions, updates to console
```

### 2.4 Size Target

< 150 lines of JS. Demo server, not production.

### 2.5 .env.example

```
OPEN_WEATHER_API_KEY=your_key_here
PORT=3000
POLL_INTERVAL_MS=60000
```

### 2.6 server/README.md

```markdown
# Weather Push Server

## Setup
1. `cp .env.example .env`
2. Add your OpenWeatherMap API key to `.env`
3. `npm install`
4. `npm start`

## Events
- Client → Server: `subscribe` with `{ cities: ["London"] }`
- Server → Client: `weather_update` with current weather data
- Server → Client: `weather_alert` for dramatic changes
```

---

## 3. Android: Interface (`:core`)

### 3.1 Add to `:core/model/`

```kotlin
package com.weather.core.model

data class WeatherAlert(
    val cityName: String,
    val type: String,              // e.g. "TEMPERATURE_DROP"
    val message: String,
    val timestamp: Long
)
```

### 3.2 Add to `:core/repository/`

```kotlin
package com.weather.core.repository

interface WeatherRealtimeService {
    /** Continuous stream of realtime weather updates (written to Room internally). */
    fun observeWeatherUpdates(): Flow<CurrentWeather>
    /** Alerts for dramatic weather changes. */
    fun observeWeatherAlerts(): Flow<WeatherAlert>
    suspend fun connect()
    suspend fun disconnect()
    suspend fun subscribeCities(cityNames: List<String>)
}
```

---

## 4. Android: Implementation (`:core:data`)

### 4.1 Add Dependency

In `:core:data/build.gradle.kts`:
```kotlin
implementation("io.socket:socket.io-client:2.1.2") {
    exclude(group = "org.json", module = "json")  // Android already has org.json
}
```

### 4.2 BuildConfig

In `:core:data/build.gradle.kts`:
```kotlin
buildConfigField("String", "SOCKET_URL", "\"${project.findProperty("SOCKET_URL") ?: "http://10.0.2.2:3000"}\"")
```

In `gradle.properties`:
```
SOCKET_URL=http://10.0.2.2:3000
```

> `10.0.2.2` = emulator's alias for host machine's localhost.

### 4.3 SocketIORealtimeServiceImpl

```kotlin
package com.weather.core.data.realtime

@Singleton
class SocketIORealtimeServiceImpl @Inject constructor(
    private val weatherDao: WeatherDao,
    logFactory: LogPortFactory
) : WeatherRealtimeService {

    private val log = logFactory.create("SocketIO")
    private var socket: Socket? = null
    private val _updates = MutableSharedFlow<CurrentWeather>()
    private val _alerts = MutableSharedFlow<WeatherAlert>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeWeatherUpdates(): Flow<CurrentWeather> = _updates.asSharedFlow()
    override fun observeWeatherAlerts(): Flow<WeatherAlert> = _alerts.asSharedFlow()

    override suspend fun connect() {
        if (socket?.connected() == true) return

        log.i("Connecting", mapOf("url" to BuildConfig.SOCKET_URL))

        val opts = IO.Options().apply {
            reconnection = true
            reconnectionAttempts = 5
            reconnectionDelay = 2000
        }
        socket = IO.socket(BuildConfig.SOCKET_URL, opts)

        socket?.on(Socket.EVENT_CONNECT) {
            log.i("Connected")
        }

        socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val error = args.firstOrNull()
            log.w("Connection error", extras = mapOf("error" to error.toString()))
        }

        socket?.on("weather_update") { args ->
            val json = args[0] as JSONObject
            scope.launch {
                try {
                    val weather = CurrentWeather(
                        cityName = json.getString("cityName"),
                        country = "",
                        temperature = json.getDouble("temperature"),
                        feelsLike = json.optDouble("feelsLike", 0.0),
                        description = json.getString("description"),
                        iconCode = json.getString("iconCode"),
                        humidity = json.getInt("humidity"),
                        windSpeed = json.getDouble("windSpeed"),
                        pressure = json.optInt("pressure", 0),
                        timestamp = json.getLong("timestamp")
                    )
                    weatherDao.insertCurrentWeather(weather.toEntity())
                    _updates.emit(weather)
                    log.d("Update saved", mapOf("city" to weather.cityName, "temp" to weather.temperature))
                } catch (e: Exception) {
                    log.e("Parse error", e)
                }
            }
        }

        socket?.on("weather_alert") { args ->
            val json = args[0] as JSONObject
            scope.launch {
                try {
                    val alert = WeatherAlert(
                        cityName = json.getString("cityName"),
                        type = json.getString("type"),
                        message = json.getString("message"),
                        timestamp = json.getLong("timestamp")
                    )
                    _alerts.emit(alert)
                    log.i("Alert received", mapOf("city" to alert.cityName, "type" to alert.type))
                } catch (e: Exception) {
                    log.e("Alert parse error", e)
                }
            }
        }

        socket?.connect()
    }

    override suspend fun disconnect() {
        log.i("Disconnecting")
        socket?.disconnect()
        socket?.off()
        socket = null
    }

    override suspend fun subscribeCities(cityNames: List<String>) {
        val payload = JSONObject().put("cities", JSONArray(cityNames))
        socket?.emit("subscribe", payload)
        log.d("Subscribed", mapOf("cities" to cityNames.joinToString(",")))
    }
}
```

### 4.4 DI

Add to `DataModule`:
```kotlin
@Binds @Singleton
abstract fun bindRealtime(impl: SocketIORealtimeServiceImpl): WeatherRealtimeService
```

---

## 5. Feature Integration (`:feature:weather`)

### 5.1 WeatherViewModel Changes

```kotlin
@HiltViewModel
class WeatherViewModel @Inject constructor(
    // ... existing deps from Phase 1 & 2 ...
    private val realtime: WeatherRealtimeService,     // ← ADD
    private val featureToggle: FeatureTogglePort       // already from Phase 2
) : ViewModel() {

    init {
        // Socket.IO — guarded by feature flag; VM is notifier only
        if (featureToggle.isEnabled(FeatureFlag.SOCKET_IO_ENABLED)) {
            viewModelScope.launch { realtime.connect() }
        }

        // Existing: observe selected city
        viewModelScope.launch {
            getSelectedCity().filterNotNull().collectLatest { cityName ->
                _uiState.update { it.copy(cityName = cityName, isLoading = true, error = null) }
                if (featureToggle.isEnabled(FeatureFlag.SOCKET_IO_ENABLED)) {
                    realtime.subscribeCities(listOf(cityName))
                }
                loadWeather(cityName)
            }
        }

        // Collect alerts → show via shared snackbar
        if (featureToggle.isEnabled(FeatureFlag.WEATHER_ALERTS_ENABLED)) {
            viewModelScope.launch {
                realtime.observeWeatherAlerts().collect { alert ->
                    _uiState.update { it.copy(alertMessage = alert.message) }
                }
            }
        }
    }

    fun dismissAlert() {
        _uiState.update { it.copy(alertMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        if (featureToggle.isEnabled(FeatureFlag.SOCKET_IO_ENABLED)) {
            // runBlocking is safe here: SocketIO disconnect() is synchronous internally,
            // returns instantly. Future impls that need async teardown should launch
            // on their own scope inside disconnect() and return.
            runBlocking { realtime.disconnect() }
        }
    }
}
```

### 5.2 WeatherUiState Change

```kotlin
data class WeatherUiState(
    // ... existing fields ...
    val alertMessage: String? = null    // ← ADD for snackbar
)
```

### 5.3 WeatherScreen Change

```kotlin
// Add SnackbarHost to Scaffold
val snackbarHostState = remember { SnackbarHostState() }

LaunchedEffect(uiState.alertMessage) {
    uiState.alertMessage?.let { message ->
        snackbarHostState.showSnackbar(message)
        viewModel.dismissAlert()
    }
}

Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    // ... existing topBar ...
)
```

### 5.4 Enable Feature Flags

Update `feature_defaults.json`:
```json
{
  "socket_io_enabled": true,
  "city_search_enabled": true,
  "hourly_forecast_enabled": true,
  "offline_banner_enabled": true,
  "weather_alerts_enabled": true
}
```

---

## 6. Key Architecture Point

**Socket.IO writes directly to Room.** The existing `observeCurrentWeather()` Flow from `WeatherRepositoryImpl` automatically picks up these writes and emits to the UI. No extra plumbing needed — the offline-first architecture from Phase 1 naturally supports realtime updates.

```
Socket.IO event → SocketIORealtimeServiceImpl → weatherDao.insert() → Room
                                                                         ↓ Flow emits
WeatherViewModel ← GetCurrentWeatherUseCase ← WeatherRepository.observe ← Room
```

---

## 7. Testing This

1. Start server: `cd server && npm install && npm start`
2. Start emulator, install app
3. Select a city → should show weather
4. Wait 60 seconds → server polls OpenWeatherMap, pushes update if changed
5. To test quickly: modify server's poll interval to 10 seconds in `.env`
6. Check Logcat for `SocketIO` tag — should see connect, subscribe, update logs

---

## 8. Phase 3 Deliverables Checklist

- [ ] `cd server && npm start` runs without error
- [ ] App connects to Socket.IO on launch (check Logcat)
- [ ] Server logs show subscription for selected city
- [ ] When server pushes update → UI reflects change without pull-to-refresh
- [ ] `socket_io_enabled = false` → app works without Socket.IO (no crash)
- [ ] `weather_alerts_enabled = true` → alert snackbar shows on dramatic change
- [ ] Server handles multiple clients and disconnect cleanup
- [ ] `./gradlew assembleDebug` still passes

---

## 9. Final Project Deliverables (All Phases)

- [ ] Working Android app with offline-first weather display
- [ ] Clean modular architecture (`:core` → `:core:data` → `:core:domain` → `:feature:*`)
- [ ] Ports & Adapters: any data source swappable via one `@Binds` change
- [ ] Log Kit with structured logging (Timber, swappable)
- [ ] Feature Toggle system (local JSON, swappable to Firebase)
- [ ] Real-time updates via Socket.IO
- [ ] Working Node.js push server
- [ ] `README.md` with architecture overview, setup instructions, screenshots
- [ ] `AI_TOOLS.md` documenting tool usage
- [ ] Clean git history
