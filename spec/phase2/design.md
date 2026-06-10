# Weather Forecast App — Phase 2: Log Kit + Feature Toggle

> **Prerequisite:** Phase 1 complete and building.
> **Goal:** Add logging and feature flag infrastructure to `:core`. Wire into existing code.
> **Time budget:** ~2 hours (two agents can run in parallel: one for logging, one for config)
> **Done criteria:** All repository/VM calls logged. Feature flags control hourly forecast visibility.

---

## 1. What Changes

| Module | Change Type | Description |
|---|---|---|
| `:core` | **ADD** packages | `logging/` and `config/` packages with interfaces + default impls |
| `:core` | **ADD** deps | Timber in `build.gradle.kts` |
| `:core` | **ADD** assets | `feature_defaults.json` |
| `:core:data` | **MODIFY** | Add log calls to `WeatherRepositoryImpl`, `CityRepositoryImpl` |
| `:feature:weather` | **MODIFY** | Inject `FeatureTogglePort`, guard hourly forecast + offline banner |
| `:feature:citylist` | **MODIFY** | Inject `FeatureTogglePort`, guard search feature |
| `:app` | **MODIFY** | Plant Timber.DebugTree in `App.onCreate()` |

**No structural changes.** No new modules. No interface changes. Pure additions.

---

## 2. Log Kit — `:core/logging/`

### 2.1 Files to Create

```
:core/src/main/java/com/weather/core/logging/
├── LogPort.kt
├── LogPortFactory.kt
├── TimberLogAdapter.kt
├── TimberLogPortFactory.kt
└── di/LogModule.kt
```

### 2.2 Interfaces

```kotlin
package com.weather.core.logging

interface LogPort {
    fun d(tag: String, message: String, extras: Map<String, Any?> = emptyMap())
    fun i(tag: String, message: String, extras: Map<String, Any?> = emptyMap())
    fun w(tag: String, message: String, throwable: Throwable? = null, extras: Map<String, Any?> = emptyMap())
    fun e(tag: String, message: String, throwable: Throwable? = null, extras: Map<String, Any?> = emptyMap())
}

interface LogPortFactory {
    fun create(tag: String): LogPort
}
```

### 2.3 Default Implementation

```kotlin
class TimberLogAdapter(private val defaultTag: String) : LogPort {
    override fun d(tag: String, message: String, extras: Map<String, Any?>) {
        Timber.tag(tag).d(formatMessage(message, extras))
    }
    override fun i(tag: String, message: String, extras: Map<String, Any?>) {
        Timber.tag(tag).i(formatMessage(message, extras))
    }
    override fun w(tag: String, message: String, throwable: Throwable?, extras: Map<String, Any?>) {
        Timber.tag(tag).w(throwable, formatMessage(message, extras))
    }
    override fun e(tag: String, message: String, throwable: Throwable?, extras: Map<String, Any?>) {
        Timber.tag(tag).e(throwable, formatMessage(message, extras))
    }

    private fun formatMessage(message: String, extras: Map<String, Any?>): String {
        if (extras.isEmpty()) return message
        return "$message [${extras.entries.joinToString(", ") { "${it.key}=${it.value}" }}]"
    }
}

@Singleton
class TimberLogPortFactory @Inject constructor() : LogPortFactory {
    override fun create(tag: String): LogPort = TimberLogAdapter(tag)
}
```

### 2.4 DI

```kotlin
@Module @InstallIn(SingletonComponent::class)
abstract class LogModule {
    @Binds @Singleton
    abstract fun bindLogPortFactory(impl: TimberLogPortFactory): LogPortFactory
}
```

### 2.5 Structured Extras Convention

Use consistent keys across the codebase:

| Key | Type | Usage |
|---|---|---|
| `city` | String | City name context |
| `source` | String | `"cache"` / `"network"` |
| `latency_ms` | Long | API call duration |
| `stale` | Boolean | Whether data came from stale cache |
| `error_type` | String | Exception class name |

### 2.6 Integration Points

**`:app` — WeatherApp.kt:**
```kotlin
override fun onCreate() {
    super.onCreate()
    if (BuildConfig.DEBUG) {
        Timber.plant(Timber.DebugTree())
    }
}
```

**`:core:data` — WeatherRepositoryImpl:**
```kotlin
class WeatherRepositoryImpl @Inject constructor(
    private val api: WeatherApiService,
    private val weatherDao: WeatherDao,
    logFactory: LogPortFactory           // ← ADD
) : WeatherRepository {

    private val log = logFactory.create("WeatherRepo")   // ← ADD

    private suspend fun fetchAndSave(cityName: String): Resource<Unit> {
        log.d("WeatherRepo", "Syncing", mapOf("city" to cityName))     // ← ADD
        return try {
            // ... existing code ...
            log.i("WeatherRepo", "Sync success", mapOf("city" to cityName, "source" to "network"))  // ← ADD
            Resource.Success(Unit)
        } catch (e: Exception) {
            log.e("WeatherRepo", "Sync failed", e, mapOf("city" to cityName))  // ← ADD
            Resource.Error(e.message ?: "Sync failed", e)
        }
    }
}
```

**Same pattern for:** `CityRepositoryImpl`, `RetrofitWeatherApiService`, `WeatherViewModel`, `CityListViewModel`.

### 2.7 Swap Example

Datadog: write `DatadogLogAdapter : LogPort`, change one `@Binds` in `LogModule`. The `extras` map naturally maps to Datadog tags.

---

## 3. Feature Toggle — `:core/config/`

### 3.1 Files to Create

```
:core/src/main/java/com/weather/core/config/
├── FeatureTogglePort.kt
├── FeatureFlag.kt
├── LocalJsonFeatureToggleAdapter.kt
└── di/ConfigModule.kt

:core/src/main/assets/
└── feature_defaults.json
```

### 3.2 Interface

```kotlin
package com.weather.core.config

interface FeatureTogglePort {
    fun isEnabled(key: FeatureFlag, defaultValue: Boolean = false): Boolean
    fun observeFlag(key: FeatureFlag): Flow<Boolean>
    suspend fun refresh()
}

enum class FeatureFlag(val key: String) {
    SOCKET_IO_ENABLED("socket_io_enabled"),
    CITY_SEARCH_ENABLED("city_search_enabled"),
    HOURLY_FORECAST_ENABLED("hourly_forecast_enabled"),
    OFFLINE_BANNER_ENABLED("offline_banner_enabled"),
    WEATHER_ALERTS_ENABLED("weather_alerts_enabled")
}
```

### 3.3 Default Config

`feature_defaults.json`:
```json
{
  "socket_io_enabled": false,
  "city_search_enabled": true,
  "hourly_forecast_enabled": true,
  "offline_banner_enabled": true,
  "weather_alerts_enabled": false
}
```

> Note: `socket_io_enabled` and `weather_alerts_enabled` default to `false` — Phase 3 features.

### 3.4 Default Implementation

```kotlin
@Singleton
class LocalJsonFeatureToggleAdapter @Inject constructor(
    @ApplicationContext private val context: Context
) : FeatureTogglePort {

    private val flags = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    init {
        val json = context.assets.open("feature_defaults.json").bufferedReader().readText()
        flags.value = Json.decodeFromString<Map<String, Boolean>>(json)
    }

    override fun isEnabled(key: FeatureFlag, defaultValue: Boolean): Boolean {
        return flags.value[key.key] ?: defaultValue
    }

    override fun observeFlag(key: FeatureFlag): Flow<Boolean> {
        return flags.map { it[key.key] ?: false }.distinctUntilChanged()
    }

    override suspend fun refresh() { /* no-op for local adapter */ }
}
```

### 3.5 DI

```kotlin
@Module @InstallIn(SingletonComponent::class)
abstract class ConfigModule {
    @Binds @Singleton
    abstract fun bindFeatureToggle(impl: LocalJsonFeatureToggleAdapter): FeatureTogglePort
}
```

### 3.6 Integration Points

**`:feature:weather` — WeatherViewModel:**
```kotlin
@HiltViewModel
class WeatherViewModel @Inject constructor(
    // ... existing deps ...
    private val featureToggle: FeatureTogglePort     // ← ADD
) : ViewModel() {
    // Expose toggle state for Compose
    val showHourlyForecast: Boolean
        get() = featureToggle.isEnabled(FeatureFlag.HOURLY_FORECAST_ENABLED)
    val showOfflineBanner: Boolean
        get() = featureToggle.isEnabled(FeatureFlag.OFFLINE_BANNER_ENABLED)
}
```

**`:feature:weather` — WeatherScreen:**
```kotlin
// In the LazyColumn:
if (viewModel.showHourlyForecast) {
    item { SectionHeader("Today") }
    item { HourlyForecastRow(uiState.hourlyForecasts) }
}

if (viewModel.showOfflineBanner && uiState.isOffline) {
    item { OfflineBanner(uiState.currentWeather?.timestamp ?: 0L) }
}
```

**`:feature:citylist` — CityListViewModel:**
```kotlin
@HiltViewModel
class CityListViewModel @Inject constructor(
    // ... existing deps ...
    private val featureToggle: FeatureTogglePort     // ← ADD
) : ViewModel() {
    val showSearch: Boolean
        get() = featureToggle.isEnabled(FeatureFlag.CITY_SEARCH_ENABLED)
}
```

**`:feature:citylist` — CityListScreen:**
```kotlin
// Only show search bar if flag is on
if (viewModel.showSearch) {
    OutlinedTextField(/* search bar */)
}
```

### 3.7 Swap Example

Firebase Remote Config: write `FirebaseRemoteConfigAdapter : FeatureTogglePort`. The `feature_defaults.json` becomes Firebase's in-app default. `refresh()` calls `remoteConfig.fetchAndActivate()`. Change one `@Binds`.

---

## 4. Phase 2 Deliverables Checklist

- [ ] `./gradlew assembleDebug` still passes
- [ ] Logcat shows structured logs with `[city=London, source=network]` style extras
- [ ] Set `hourly_forecast_enabled` to `false` in JSON → hourly section disappears
- [ ] Set `city_search_enabled` to `false` → search bar disappears
- [ ] No existing Phase 1 behavior broken
