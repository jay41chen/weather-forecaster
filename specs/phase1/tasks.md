# Phase 1 Tasks

> Reference: [design.md](design.md) for full code snippets and contracts.

---

## Task 1: Project Scaffold & Version Catalog

**Files (new):**
- `settings.gradle.kts`
- `build.gradle.kts` (root)
- `gradle/libs.versions.toml`
- Each module: `build.gradle.kts` + minimal source dir

**Steps:**
1. Create root `settings.gradle.kts` with all 7 module includes (`:app`, `:core`, `:core:data`, `:core:domain`, `:core:ui`, `:feature:weather`, `:feature:citylist`).
2. Create `gradle/libs.versions.toml` with versions listed in design §1 (AGP 8.5.2, Kotlin 2.0.10, KSP, Compose BOM 2024.08.00, Hilt 2.51.1, etc.).
3. Create root `build.gradle.kts` applying AGP + Kotlin + Hilt + KSP plugins with `apply false`.
4. Create each module's `build.gradle.kts` per the plugin/dependency table in design §1.
5. Shared android config: `minSdk 26`, `targetSdk 34`, `compileSdk 34`, `jvmTarget "17"`.
6. Create minimal `src/main/AndroidManifest.xml` for each library module and the app module.

**Dependencies:** None (first task).

**Acceptance:**
```bash
./gradlew projects
# Must list all 7 modules
./gradlew assembleDebug
# May fail on missing source — that's OK, build config itself must parse
```

---

## Task 2: `:core` — Models & Interfaces

**Files (new):**
- `core/src/main/java/com/weather/core/model/CurrentWeather.kt`
- `core/src/main/java/com/weather/core/model/DailyForecast.kt`
- `core/src/main/java/com/weather/core/model/HourlyForecast.kt`
- `core/src/main/java/com/weather/core/model/City.kt`
- `core/src/main/java/com/weather/core/model/Resource.kt`
- `core/src/main/java/com/weather/core/network/WeatherApiService.kt`
- `core/src/main/java/com/weather/core/network/ForecastResult.kt`
- `core/src/main/java/com/weather/core/repository/WeatherRepository.kt`
- `core/src/main/java/com/weather/core/repository/CityRepository.kt`

**Steps:**
1. Create data classes exactly per design §2.2.
2. Create `WeatherApiService` interface + `ForecastResult` per design §2.3.
3. Create `WeatherRepository` and `CityRepository` interfaces per design §2.4.

**Dependencies:** Task 1 (`:core` module exists with `build.gradle.kts`).

**Acceptance:**
```bash
./gradlew :core:compileDebugKotlin
```

---

## Task 3: `:core:data` — DTOs & Retrofit

**Files (new):**
- `core/data/src/main/java/com/weather/core/data/network/dto/CurrentWeatherDto.kt`
- `core/data/src/main/java/com/weather/core/data/network/dto/ForecastDto.kt`
- `core/data/src/main/java/com/weather/core/data/network/dto/GeocodingDto.kt`
- `core/data/src/main/java/com/weather/core/data/network/RetrofitClient.kt`
- `core/data/src/main/java/com/weather/core/data/network/RetrofitWeatherApiService.kt`

**Steps:**
1. Create DTO classes with `@Serializable` and `@SerialName` mapping to OpenWeatherMap JSON per design §3.2–3.3.
2. Create `RetrofitClient` internal interface per design §3.4.
3. Create `RetrofitWeatherApiService` implementing `WeatherApiService` from `:core`. Contains DTO→domain mapping + daily aggregation logic (design §3.8): group 3-hour entries by date, pick noon entry for icon/description, average humidity/windSpeed.

**Dependencies:** Task 2 (`:core` models and `WeatherApiService` interface).

**Acceptance:**
```bash
./gradlew :core:data:compileDebugKotlin
```

---

## Task 4: `:core:data` — Room Database

**Files (new):**
- `core/data/src/main/java/com/weather/core/data/local/entity/CurrentWeatherEntity.kt`
- `core/data/src/main/java/com/weather/core/data/local/entity/DailyForecastEntity.kt`
- `core/data/src/main/java/com/weather/core/data/local/entity/HourlyForecastEntity.kt`
- `core/data/src/main/java/com/weather/core/data/local/entity/CityEntity.kt`
- `core/data/src/main/java/com/weather/core/data/local/dao/WeatherDao.kt`
- `core/data/src/main/java/com/weather/core/data/local/dao/CityDao.kt`
- `core/data/src/main/java/com/weather/core/data/local/WeatherDatabase.kt`

**Steps:**
1. Create entity classes per design §3.5.
2. Create `WeatherDao` and `CityDao` per design §3.6.
3. Create `WeatherDatabase` as `@Database(entities = [...], version = 1)` with abstract dao getters.

**Dependencies:** Task 1 (`:core:data` module exists). No dependency on Task 3 — can run in parallel.

**Acceptance:**
```bash
./gradlew :core:data:compileDebugKotlin
```

---

## Task 5: `:core:data` — Mappers & Repositories

**Files (new):**
- `core/data/src/main/java/com/weather/core/data/mapper/WeatherMappers.kt`
- `core/data/src/main/java/com/weather/core/data/mapper/CityMappers.kt`
- `core/data/src/main/java/com/weather/core/data/repository/WeatherRepositoryImpl.kt`
- `core/data/src/main/java/com/weather/core/data/repository/CityRepositoryImpl.kt`

**Steps:**
1. Create mapper extension functions: `CurrentWeatherEntity.toDomain()`, `CurrentWeather.toEntity()`, `DailyForecastEntity.toDomain()`, etc.
2. Create `WeatherRepositoryImpl` per design §3.7 — offline-first with 30-min staleness check via `weatherDao.getLastUpdated()`.
3. Create `CityRepositoryImpl` per design §3.9 — uses `CityDao` for persistence, `DataStore<Preferences>` for selected city name, `WeatherApiService.searchCities()` for search.
4. Add `initializeIfNeeded()` to `CityRepositoryImpl` — pre-populate 10 default cities when `CityDao.count() == 0`, select "London" as default (design §8).

**Dependencies:** Tasks 3 + 4 (DTOs, Room entities, DAOs all exist).

**Acceptance:**
```bash
./gradlew :core:data:compileDebugKotlin
```

---

## Task 6: `:core:data` — DI Modules

**Files (new):**
- `core/data/src/main/java/com/weather/core/data/di/NetworkModule.kt`
- `core/data/src/main/java/com/weather/core/data/di/DatabaseModule.kt`
- `core/data/src/main/java/com/weather/core/data/di/DataModule.kt`

**Steps:**
1. `NetworkModule` (`@Provides`): OkHttpClient with interceptor appending `appid` query param from `BuildConfig.OPEN_WEATHER_API_KEY`, Retrofit instance with kotlinx-serialization converter, `RetrofitClient` instance.
2. `DatabaseModule` (`@Provides`): `WeatherDatabase` via `Room.databaseBuilder()`, `WeatherDao`, `CityDao`, `DataStore<Preferences>`.
3. `DataModule` (`@Binds`): bind `RetrofitWeatherApiService` → `WeatherApiService`, `WeatherRepositoryImpl` → `WeatherRepository`, `CityRepositoryImpl` → `CityRepository`.
4. Set up `BuildConfig.OPEN_WEATHER_API_KEY` in `:core:data/build.gradle.kts` per design §3.10.

**Dependencies:** Task 5 (all implementations exist to bind).

**Acceptance:**
```bash
./gradlew :core:data:compileDebugKotlin
```

---

## Task 7: `:core:domain` — Use Cases

**Files (new):**
- `core/domain/src/main/java/com/weather/core/domain/GetCurrentWeatherUseCase.kt`
- `core/domain/src/main/java/com/weather/core/domain/GetDailyForecastUseCase.kt`
- `core/domain/src/main/java/com/weather/core/domain/GetHourlyForecastUseCase.kt`
- `core/domain/src/main/java/com/weather/core/domain/RefreshWeatherUseCase.kt`
- `core/domain/src/main/java/com/weather/core/domain/GetSavedCitiesUseCase.kt`
- `core/domain/src/main/java/com/weather/core/domain/SearchCitiesUseCase.kt`
- `core/domain/src/main/java/com/weather/core/domain/SaveCityUseCase.kt`
- `core/domain/src/main/java/com/weather/core/domain/RemoveCityUseCase.kt`
- `core/domain/src/main/java/com/weather/core/domain/SelectCityUseCase.kt`
- `core/domain/src/main/java/com/weather/core/domain/GetSelectedCityUseCase.kt`

**Steps:**
1. Implement all use cases per design §4. Each is a single-method class with `@Inject constructor`.
2. `GetCurrentWeatherUseCase.invoke()` uses the cache-first-then-sync pattern: emit cached data immediately, call `repo.sync()`, then `flatMapLatest` to observe Room updates.
3. `GetDailyForecastUseCase` and `GetHourlyForecastUseCase` follow the same pattern.
4. Remaining use cases are thin delegations to repository methods.

**Dependencies:** Task 2 (`:core` interfaces). Can run in parallel with Tasks 3–6.

**Acceptance:**
```bash
./gradlew :core:domain:compileDebugKotlin
```

---

## Task 8: `:core:ui` — Theme & Shared Components

**Files (new):**
- `core/ui/src/main/java/com/weather/core/ui/theme/Theme.kt`
- `core/ui/src/main/java/com/weather/core/ui/theme/Color.kt`
- `core/ui/src/main/java/com/weather/core/ui/component/WeatherIcon.kt`
- `core/ui/src/main/java/com/weather/core/ui/component/LoadingContent.kt`
- `core/ui/src/main/java/com/weather/core/ui/component/ErrorContent.kt`

**Steps:**
1. Define color scheme with primary `#4FC3F7`, generate light + dark `ColorScheme`.
2. `WeatherForecastTheme` composable with `isSystemInDarkTheme()` per design §5.
3. `WeatherIcon`: Coil `AsyncImage` loading `https://openweathermap.org/img/wn/{iconCode}@2x.png`.
4. `LoadingContent`: centered `CircularProgressIndicator`.
5. `ErrorContent`: column with error icon + message + "Retry" button.

**Dependencies:** Task 1 (`:core:ui` module exists).

**Acceptance:**
```bash
./gradlew :core:ui:compileDebugKotlin
```

---

## Task 9: `:feature:weather` — ViewModel & Screen

**Files (new):**
- `feature/weather/src/main/java/com/weather/feature/weather/WeatherUiState.kt`
- `feature/weather/src/main/java/com/weather/feature/weather/WeatherViewModel.kt`
- `feature/weather/src/main/java/com/weather/feature/weather/WeatherScreen.kt`
- `feature/weather/src/main/java/com/weather/feature/weather/component/CurrentWeatherCard.kt`
- `feature/weather/src/main/java/com/weather/feature/weather/component/WeatherDetailsRow.kt`
- `feature/weather/src/main/java/com/weather/feature/weather/component/HourlyForecastRow.kt`
- `feature/weather/src/main/java/com/weather/feature/weather/component/DailyForecastItem.kt`

**Steps:**
1. `WeatherUiState` data class per design §6.
2. `WeatherViewModel`: inject 5 use cases, observe selected city via `getSelectedCity().filterNotNull().collectLatest`, launch 3 parallel collectors (current, daily, hourly), expose `refresh()` per design §6.
3. `WeatherScreen` composable: `Scaffold` + `PullToRefreshBox` + `LazyColumn` layout per design §6. Loading/error states use `:core:ui` components.
4. Extract sub-composables: `CurrentWeatherCard` (big temp + icon + description), `WeatherDetailsRow` (3 detail cards), `HourlyForecastRow` (horizontal `LazyRow`), `DailyForecastItem`.

**Dependencies:** Tasks 7 + 8 (use cases + UI components).

**Acceptance:**
```bash
./gradlew :feature:weather:compileDebugKotlin
```

---

## Task 10: `:feature:citylist` — ViewModel & Screen

**Files (new):**
- `feature/citylist/src/main/java/com/weather/feature/citylist/CityListUiState.kt`
- `feature/citylist/src/main/java/com/weather/feature/citylist/CityListViewModel.kt`
- `feature/citylist/src/main/java/com/weather/feature/citylist/CityListScreen.kt`

**Steps:**
1. `CityListUiState` per design §7.
2. `CityListViewModel`: collect saved cities flow, collect selected city flow, search with `debounce(500)` → `flatMapLatest`, expose `onCitySelect`, `onCityAdd`, `onCityRemove` per design §7.
3. `CityListScreen`: `Scaffold` with back button, search `OutlinedTextField` with clear icon, conditional content (search results with "+" button vs. saved cities with delete icon, highlight selected).

**Dependencies:** Tasks 7 + 8 (use cases + UI components). Can run in parallel with Task 9.

**Acceptance:**
```bash
./gradlew :feature:citylist:compileDebugKotlin
```

---

## Task 11: `:app` — Wiring & Entry Point

**Files (new):**
- `app/src/main/java/com/weather/app/WeatherApp.kt`
- `app/src/main/java/com/weather/app/MainActivity.kt`
- `app/src/main/java/com/weather/app/navigation/WeatherNavHost.kt`

**Files (modify):**
- `app/src/main/AndroidManifest.xml` — add `INTERNET` permission, declare `MainActivity` + `WeatherApp`.
- `gradle.properties` — add `OPEN_WEATHER_API_KEY=` placeholder.

**Steps:**
1. `WeatherApp`: `@HiltAndroidApp`, call `CityRepositoryImpl.initializeIfNeeded()` in `onCreate()` via injected `CityRepository` (add `suspend fun initializeIfNeeded()` to interface if needed, or use a dedicated initializer).
2. `MainActivity`: `@AndroidEntryPoint`, `setContent { WeatherForecastTheme { WeatherNavHost(...) } }`.
3. `WeatherNavHost`: two routes — `"weather"` → `WeatherScreen`, `"city_list"` → `CityListScreen` — per design §8.
4. `AndroidManifest.xml`: `INTERNET` permission.
5. `gradle.properties`: `OPEN_WEATHER_API_KEY=your_key_here`.

**Dependencies:** Tasks 6, 9, 10 (DI wired, both feature screens exist).

**Acceptance:**
```bash
./gradlew assembleDebug
# APK builds successfully
```

---

## Task 12: Integration Test on Device

**No files to create.** Validation-only task.

**Steps:**
1. Set a valid API key in `gradle.properties`.
2. `./gradlew installDebug` on emulator or device.
3. Verify: app launches → shows London weather → can switch city → pull-to-refresh works → kill network → shows cached data → search cities via geocoding.

**Dependencies:** Task 11.

**Acceptance:**
- App launches showing London weather with current temp, icon, description.
- Tap city name → city list screen → tap another city → returns with new city's weather.
- Pull down → refresh indicator → data updates.
- Enable airplane mode → reopen app → cached weather shown.
- Search "Tokyo" → results appear → tap "+" → city saved.
