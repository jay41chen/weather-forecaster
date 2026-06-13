# Phase 2 Tasks

> Reference: [design.md](design.md) for full code snippets and contracts.
> Prerequisite: Phase 1 complete and building.

---

## Task 1: `:core` — Log Kit

**Files (new):**
- `core/src/main/java/com/weather/core/logging/LogPort.kt`
- `core/src/main/java/com/weather/core/logging/LogPortFactory.kt`
- `core/src/main/java/com/weather/core/logging/TimberLogAdapter.kt`
- `core/src/main/java/com/weather/core/logging/TimberLogPortFactory.kt`
- `core/src/main/java/com/weather/core/logging/di/LogModule.kt`

**Files (modify):**
- `core/build.gradle.kts` — add Timber dependency.

**Steps:**
1. Add `timber` to `libs.versions.toml` and `:core/build.gradle.kts`.
2. Create `LogPort` interface with `d/i/w/e` methods, each accepting `tag`, `message`, optional `extras: Map<String, Any?>` and optional `throwable` (for w/e). Per design §2.2.
3. Create `LogPortFactory` interface with `create(tag: String): LogPort`. Per design §2.2.
4. Create `TimberLogAdapter` implementing `LogPort` — delegates to `Timber.tag(tag).d/i/w/e()`. Format extras as `[key=value, ...]` suffix. Per design §2.3.
5. Create `TimberLogPortFactory` (`@Singleton`, `@Inject constructor`) implementing `LogPortFactory`. Per design §2.3.
6. Create `LogModule` Hilt module with `@Binds` for `LogPortFactory`. Per design §2.4.

**Dependencies:** Phase 1 `:core` module.

**Acceptance:**
```bash
./gradlew :core:compileDebugKotlin
```

---

## Task 2: `:core` — Feature Toggle

**Files (new):**
- `core/src/main/java/com/weather/core/config/FeatureTogglePort.kt`
- `core/src/main/java/com/weather/core/config/FeatureFlag.kt`
- `core/src/main/java/com/weather/core/config/LocalJsonFeatureToggleAdapter.kt`
- `core/src/main/java/com/weather/core/config/di/ConfigModule.kt`
- `core/src/main/assets/feature_defaults.json`

**Steps:**
1. Create `FeatureFlag` enum with keys: `SOCKET_IO_ENABLED`, `CITY_SEARCH_ENABLED`, `HOURLY_FORECAST_ENABLED`, `OFFLINE_BANNER_ENABLED`, `WEATHER_ALERTS_ENABLED`. Per design §3.2.
2. Create `FeatureTogglePort` interface with `isEnabled(key, defaultValue)`, `observeFlag(key): Flow<Boolean>`, `suspend fun refresh()`. Per design §3.2.
3. Create `feature_defaults.json` — `socket_io_enabled: false`, `weather_alerts_enabled: false`, rest `true`. Per design §3.3.
4. Create `LocalJsonFeatureToggleAdapter` (`@Singleton`) — loads JSON from assets in `init`, backs `MutableStateFlow<Map<String, Boolean>>`. Per design §3.4.
5. Create `ConfigModule` Hilt module with `@Binds` for `FeatureTogglePort`. Per design §3.5.

**Dependencies:** Phase 1 `:core` module. Can run in parallel with Task 1.

**Acceptance:**
```bash
./gradlew :core:compileDebugKotlin
```

---

## Task 3: Wire Logging into `:core:data`

**Files (modify):**
- `core/data/src/main/java/com/weather/core/data/repository/WeatherRepositoryImpl.kt`
- `core/data/src/main/java/com/weather/core/data/repository/CityRepositoryImpl.kt`
- `core/data/src/main/java/com/weather/core/data/network/RetrofitWeatherApiService.kt`

**Steps:**
1. Add `logFactory: LogPortFactory` to each class's `@Inject constructor`.
2. Create `private val log = logFactory.create("TAG")` in each class (tags: `WeatherRepo`, `CityRepo`, `WeatherApi`).
3. Add log calls at key points per design §2.6:
   - `WeatherRepositoryImpl.fetchAndSave()`: log start (`d`), success (`i` with `source=network`), failure (`e` with exception).
   - `WeatherRepositoryImpl.sync()`: log cache-hit (`d` with `stale=false`).
   - `CityRepositoryImpl`: log city save/remove/select, search.
   - `RetrofitWeatherApiService`: log API calls with `latency_ms`.
4. Use extras keys per convention: `city`, `source`, `latency_ms`, `stale`, `error_type` (design §2.5).

**Dependencies:** Tasks 1 (LogPort exists).

**Acceptance:**
```bash
./gradlew :core:data:compileDebugKotlin
```

---

## Task 4: Wire Feature Toggles into Features

**Files (modify):**
- `feature/weather/src/main/java/com/weather/feature/weather/WeatherViewModel.kt`
- `feature/weather/src/main/java/com/weather/feature/weather/WeatherScreen.kt`
- `feature/citylist/src/main/java/com/weather/feature/citylist/CityListViewModel.kt`
- `feature/citylist/src/main/java/com/weather/feature/citylist/CityListScreen.kt`

**Steps:**
1. `WeatherViewModel`: inject `FeatureTogglePort`, expose `showHourlyForecast` and `showOfflineBanner` properties checking `FeatureFlag.HOURLY_FORECAST_ENABLED` and `FeatureFlag.OFFLINE_BANNER_ENABLED`. Per design §3.6.
2. `WeatherScreen`: guard hourly forecast section and offline banner with viewModel toggle properties. Per design §3.6.
3. `CityListViewModel`: inject `FeatureTogglePort`, expose `showSearch` checking `FeatureFlag.CITY_SEARCH_ENABLED`. Per design §3.6.
4. `CityListScreen`: guard search `OutlinedTextField` with `viewModel.showSearch`. Per design §3.6.

**Dependencies:** Task 2 (FeatureTogglePort exists), Phase 1 Tasks 9+10 (feature screens exist).

**Acceptance:**
```bash
./gradlew :feature:weather:compileDebugKotlin
./gradlew :feature:citylist:compileDebugKotlin
```

---

## Task 5: Plant Timber in `:app`

**Files (modify):**
- `app/src/main/java/com/weather/app/WeatherApp.kt`

**Steps:**
1. Add `Timber.plant(Timber.DebugTree())` inside `if (BuildConfig.DEBUG)` in `WeatherApp.onCreate()`. Per design §2.6.

**Dependencies:** Task 1 (Timber dependency available via `:core`).

**Acceptance:**
```bash
./gradlew assembleDebug
# Install and check Logcat for structured logs with [city=London, source=network] style
```

---

## Task 6: Integration Verification

**No files to create.** Validation-only task.

**Steps:**
1. `./gradlew assembleDebug` — must pass.
2. Install on device/emulator, trigger weather load. Check Logcat for `WeatherRepo`, `CityRepo`, `WeatherApi` tags with structured extras.
3. Edit `feature_defaults.json`: set `hourly_forecast_enabled` to `false` → rebuild → hourly section must disappear.
4. Edit `feature_defaults.json`: set `city_search_enabled` to `false` → rebuild → search bar must disappear.
5. Restore `feature_defaults.json` to original values.

**Dependencies:** Task 5.

**Acceptance:**
- Logcat shows `WeatherRepo: Sync success [city=London, source=network]` style output.
- Toggling flags visibly hides/shows UI sections.
- No Phase 1 behavior regression.
