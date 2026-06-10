# Weather Forecast App — Phase 1: MVP

> **Goal:** 可執行的天氣 App，Clean Architecture + 模組化 + Offline-First。
> **Time budget:** ~4 hours
> **Done criteria:** `./gradlew assembleDebug` passes, install on device, select city & view weather.

---

## 1. Module Structure

```
:app                 → Application entry, Navigation host, Hilt setup
:core                → Models, repository interfaces (contracts only, no infra yet)
:core:data           → Retrofit, Room, DataStore, repository implementations
:core:domain         → Use cases (pure business logic)
:core:ui             → Compose theme (Material3), shared composables
:feature:weather     → Today + weekly forecast screen & ViewModel
:feature:citylist    → City list/search screen & ViewModel
```

### settings.gradle.kts

```kotlin
include(":app")
include(":core")
include(":core:data")
include(":core:domain")
include(":core:ui")
include(":feature:weather")
include(":feature:citylist")
```

### Dependency Graph

```
:app              → :feature:weather, :feature:citylist, :core:data, :core:ui, :core
:feature:weather  → :core:domain, :core:ui, :core
:feature:citylist → :core:domain, :core:ui, :core
:core:domain      → :core
:core:data        → :core
:core:ui          → :core
:core             → (standalone foundation — zero project deps)
```

### build.gradle.kts Rules

| Module | Plugin | Key Dependencies |
|---|---|---|
| `:app` | `android.application`, `hilt`, `ksp`, `compose.compiler` | All modules, Hilt, Navigation Compose |
| `:core` | `android.library`, `kotlin.android` | `kotlinx-coroutines-core`, `javax.inject` |
| `:core:data` | `android.library`, `hilt`, `ksp`, `kotlin.serialization` | Retrofit, Room, DataStore, OkHttp, kotlinx-serialization, `:core` |
| `:core:domain` | `android.library`, `hilt`, `ksp` | `:core` |
| `:core:ui` | `android.library`, `compose.compiler` | Compose BOM, Material3, Coil, `:core` |
| `:feature:weather` | `android.library`, `hilt`, `ksp`, `compose.compiler` | Compose, Hilt Navigation Compose, `:core:domain`, `:core:ui`, `:core` |
| `:feature:citylist` | `android.library`, `hilt`, `ksp`, `compose.compiler` | Compose, Hilt Navigation Compose, `:core:domain`, `:core:ui`, `:core` |

**Shared Android config:** `minSdk = 26`, `targetSdk = 34`, `compileSdk = 34`, `jvmTarget = "17"`, `kotlin.jvmToolchain(17)`

### Version Catalog (gradle/libs.versions.toml)

Use version catalog for all dependencies. Key versions:
- AGP 8.5.2, Kotlin 2.0.10, KSP 2.0.10-1.0.24
- Compose BOM 2024.08.00, Material3
- Hilt 2.51.1, Hilt Navigation Compose 1.2.0
- Retrofit 2.11.0, OkHttp 4.12.0
- kotlinx-serialization 1.7.1, retrofit2-kotlinx-serialization-converter 1.0.0
- Room 2.6.1, DataStore 1.1.1
- Coroutines 1.8.1
- Coil 2.7.0
- Navigation Compose 2.7.7
- Lifecycle 2.8.4

---

## 2. `:core` Module

The foundation. Contains ALL shared models and ALL interfaces. No heavy dependencies.

### 2.1 Package Structure

```
:core/src/main/java/com/weather/core/
├── model/
│   ├── CurrentWeather.kt
│   ├── DailyForecast.kt
│   ├── HourlyForecast.kt
│   ├── City.kt
│   └── Resource.kt
├── network/
│   └── WeatherApiService.kt       → Interface
└── repository/
    ├── WeatherRepository.kt       → Interface
    └── CityRepository.kt          → Interface
```

### 2.2 Models

```kotlin
package com.weather.core.model

data class CurrentWeather(
    val cityName: String,
    val country: String,
    val temperature: Double,       // Celsius
    val feelsLike: Double,
    val description: String,       // e.g. "clear sky"
    val iconCode: String,          // e.g. "01d"
    val humidity: Int,             // percentage
    val windSpeed: Double,         // m/s
    val pressure: Int,             // hPa
    val timestamp: Long            // epoch seconds
)

data class DailyForecast(
    val date: LocalDate,
    val maxTemp: Double,
    val minTemp: Double,
    val description: String,
    val iconCode: String,
    val humidity: Int,
    val windSpeed: Double
)

data class HourlyForecast(
    val timestamp: Long,
    val temperature: Double,
    val iconCode: String,
    val description: String
)

data class City(
    val name: String,
    val country: String,
    val state: String?,
    val latitude: Double,
    val longitude: Double
)

sealed interface Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>
    data object Loading : Resource<Nothing>
}
```

### 2.3 Network Interface

```kotlin
package com.weather.core.network

/**
 * Abstraction over weather API transport.
 * Implementation: RetrofitWeatherApiService in :core:data.
 * Returns domain models — adapter handles DTO mapping internally.
 * 
 * Swap REST → GraphQL: write new impl, change one @Binds.
 */
interface WeatherApiService {
    suspend fun getCurrentWeather(cityName: String): CurrentWeather
    suspend fun getForecast(cityName: String): ForecastResult
    suspend fun searchCities(query: String): List<City>
}

data class ForecastResult(
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>
)
```

### 2.4 Repository Interfaces

```kotlin
package com.weather.core.repository

interface WeatherRepository {
    fun observeCurrentWeather(cityName: String): Flow<CurrentWeather?>
    fun observeDailyForecasts(cityName: String): Flow<List<DailyForecast>>
    fun observeHourlyForecasts(cityName: String): Flow<List<HourlyForecast>>
    /** Sync from remote if cache is stale (>30 min). */
    suspend fun sync(cityName: String): Resource<Unit>
    /** Force sync regardless of staleness (pull-to-refresh). */
    suspend fun forceSync(cityName: String): Resource<Unit>
}

interface CityRepository {
    fun observeSavedCities(): Flow<List<City>>
    fun observeSelectedCityName(): Flow<String?>
    suspend fun searchCities(query: String): Resource<List<City>>
    suspend fun saveCity(city: City)
    suspend fun removeCity(city: City)
    suspend fun selectCity(cityName: String)
}
```

---

## 3. `:core:data` Module

Concrete implementations. All heavy deps (Retrofit, Room, DataStore) isolated here.

### 3.1 Package Structure

```
:core:data/src/main/java/com/weather/core/data/
├── network/
│   ├── RetrofitWeatherApiService.kt   → Implements WeatherApiService
│   ├── RetrofitClient.kt             → Internal Retrofit interface
│   └── dto/
│       ├── CurrentWeatherDto.kt
│       ├── ForecastDto.kt
│       └── GeocodingDto.kt
├── local/
│   ├── WeatherDatabase.kt
│   ├── dao/
│   │   ├── WeatherDao.kt
│   │   └── CityDao.kt
│   └── entity/
│       ├── CurrentWeatherEntity.kt
│       ├── DailyForecastEntity.kt
│       ├── HourlyForecastEntity.kt
│       └── CityEntity.kt
├── repository/
│   ├── WeatherRepositoryImpl.kt
│   └── CityRepositoryImpl.kt
├── mapper/
│   ├── WeatherMappers.kt
│   └── CityMappers.kt
└── di/
    ├── NetworkModule.kt
    ├── DatabaseModule.kt
    └── DataModule.kt
```

### 3.2 API Contract (OpenWeatherMap)

Base URL: `https://api.openweathermap.org`

**Current Weather:**
```
GET /data/2.5/weather?q={cityName}&appid={apiKey}&units=metric
```
Response fields: `name`, `main.temp`, `main.feels_like`, `main.humidity`, `main.pressure`, `weather[0].description`, `weather[0].icon`, `wind.speed`, `dt`, `sys.country`

**5-Day / 3-Hour Forecast:**
```
GET /data/2.5/forecast?q={cityName}&appid={apiKey}&units=metric
```
Response: `list[]` with `dt`, `main.temp`, `main.temp_min`, `main.temp_max`, `main.humidity`, `weather[0]`, `wind.speed`. Plus `city.name`, `city.country`.

**Geocoding:**
```
GET /geo/1.0/direct?q={query}&limit=5&appid={apiKey}
```
Response: `[{ name, country, state, lat, lon }]`

Icon URL: `https://openweathermap.org/img/wn/{icon}@2x.png`

### 3.3 DTOs

Mirror JSON structures. Use `@Serializable` (kotlinx.serialization).
Use `@SerialName` for JSON field name mapping where needed.

### 3.4 Internal Retrofit Interface

```kotlin
internal interface RetrofitClient {
    @GET("/data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("q") cityName: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): CurrentWeatherDto

    @GET("/data/2.5/forecast")
    suspend fun getForecast(
        @Query("q") cityName: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): ForecastDto

    @GET("/geo/1.0/direct")
    suspend fun searchCity(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
        @Query("appid") apiKey: String
    ): List<GeocodingDto>
}
```

### 3.5 Room Schema

```kotlin
@Entity(tableName = "current_weather")
data class CurrentWeatherEntity(
    @PrimaryKey val cityName: String,
    val country: String,
    val temperature: Double,
    val feelsLike: Double,
    val description: String,
    val iconCode: String,
    val humidity: Int,
    val windSpeed: Double,
    val pressure: Int,
    val timestamp: Long,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_forecasts", primaryKeys = ["cityName", "date"])
data class DailyForecastEntity(
    val cityName: String,
    val date: String,          // ISO LocalDate string
    val maxTemp: Double,
    val minTemp: Double,
    val description: String,
    val iconCode: String,
    val humidity: Int,
    val windSpeed: Double
)

@Entity(tableName = "hourly_forecasts", primaryKeys = ["cityName", "timestamp"])
data class HourlyForecastEntity(
    val cityName: String,
    val timestamp: Long,
    val temperature: Double,
    val iconCode: String,
    val description: String
)

@Entity(tableName = "saved_cities")
data class CityEntity(
    @PrimaryKey val id: String,   // "{name},{country}"
    val name: String,
    val country: String,
    val state: String?,
    val latitude: Double,
    val longitude: Double
)
```

### 3.6 DAOs

```kotlin
@Dao
interface WeatherDao {
    @Query("SELECT * FROM current_weather WHERE cityName = :cityName")
    fun observeCurrentWeather(cityName: String): Flow<CurrentWeatherEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrentWeather(entity: CurrentWeatherEntity)

    @Query("SELECT lastUpdated FROM current_weather WHERE cityName = :cityName")
    suspend fun getLastUpdated(cityName: String): Long?

    @Query("SELECT * FROM daily_forecasts WHERE cityName = :cityName ORDER BY date ASC")
    fun observeDailyForecasts(cityName: String): Flow<List<DailyForecastEntity>>

    @Transaction
    suspend fun replaceDailyForecasts(cityName: String, forecasts: List<DailyForecastEntity>) {
        deleteDailyForecasts(cityName)
        insertDailyForecasts(forecasts)
    }

    @Query("DELETE FROM daily_forecasts WHERE cityName = :cityName")
    suspend fun deleteDailyForecasts(cityName: String)

    @Insert
    suspend fun insertDailyForecasts(forecasts: List<DailyForecastEntity>)

    @Query("SELECT * FROM hourly_forecasts WHERE cityName = :cityName ORDER BY timestamp ASC")
    fun observeHourlyForecasts(cityName: String): Flow<List<HourlyForecastEntity>>

    @Transaction
    suspend fun replaceHourlyForecasts(cityName: String, forecasts: List<HourlyForecastEntity>) {
        deleteHourlyForecasts(cityName)
        insertHourlyForecasts(forecasts)
    }

    @Query("DELETE FROM hourly_forecasts WHERE cityName = :cityName")
    suspend fun deleteHourlyForecasts(cityName: String)

    @Insert
    suspend fun insertHourlyForecasts(forecasts: List<HourlyForecastEntity>)
}

@Dao
interface CityDao {
    @Query("SELECT * FROM saved_cities ORDER BY name ASC")
    fun observeAllCities(): Flow<List<CityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCity(city: CityEntity)

    @Delete
    suspend fun deleteCity(city: CityEntity)

    @Query("SELECT COUNT(*) FROM saved_cities")
    suspend fun count(): Int
}
```

### 3.7 Offline-First: WeatherRepositoryImpl

```kotlin
class WeatherRepositoryImpl @Inject constructor(
    private val api: WeatherApiService,     // interface from :core
    private val weatherDao: WeatherDao
) : WeatherRepository {

    override fun observeCurrentWeather(cityName: String): Flow<CurrentWeather?> {
        return weatherDao.observeCurrentWeather(cityName).map { it?.toDomain() }
    }

    override fun observeDailyForecasts(cityName: String): Flow<List<DailyForecast>> {
        return weatherDao.observeDailyForecasts(cityName).map { list -> list.map { it.toDomain() } }
    }

    override fun observeHourlyForecasts(cityName: String): Flow<List<HourlyForecast>> {
        return weatherDao.observeHourlyForecasts(cityName).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun sync(cityName: String): Resource<Unit> {
        val lastUpdated = weatherDao.getLastUpdated(cityName) ?: 0L
        if (System.currentTimeMillis() - lastUpdated < 30 * 60_000) {
            return Resource.Success(Unit) // cache is fresh
        }
        return fetchAndSave(cityName)
    }

    override suspend fun forceSync(cityName: String): Resource<Unit> {
        return fetchAndSave(cityName)
    }

    private suspend fun fetchAndSave(cityName: String): Resource<Unit> {
        return try {
            // Fetch current weather
            val current = api.getCurrentWeather(cityName)
            weatherDao.insertCurrentWeather(current.toEntity())

            // Fetch forecast
            val forecast = api.getForecast(cityName)
            weatherDao.replaceDailyForecasts(cityName, forecast.daily.map { it.toEntity(cityName) })
            weatherDao.replaceHourlyForecasts(cityName, forecast.hourly.map { it.toEntity(cityName) })

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Sync failed", e)
        }
    }
}
```

### 3.8 RetrofitWeatherApiService

Implements `WeatherApiService` from `:core`. Uses internal `RetrofitClient`.
Handles DTO → domain model mapping. Contains forecast aggregation logic:

**Daily aggregation:** Group 3-hour entries by date:
- `maxTemp` = max of all `temp_max` in that day
- `minTemp` = min of all `temp_min` in that day
- `icon/description` = from entry closest to 12:00 noon
- `humidity/windSpeed` = average

**Hourly:** Filter today's entries only, map directly.

### 3.9 CityRepositoryImpl

Uses `WeatherApiService.searchCities()` for search, `CityDao` for persistence, `DataStore<Preferences>` for selected city name.

### 3.10 DI Modules

```kotlin
// NetworkModule — @Provides
// OkHttp with interceptor that appends ?appid={key} to every request
// Retrofit with kotlinx-serialization converter
// RetrofitClient instance

// DatabaseModule — @Provides
// Room database, WeatherDao, CityDao, DataStore<Preferences>

// DataModule — @Binds
@Binds abstract fun bindWeatherApi(impl: RetrofitWeatherApiService): WeatherApiService
@Binds abstract fun bindWeatherRepo(impl: WeatherRepositoryImpl): WeatherRepository
@Binds abstract fun bindCityRepo(impl: CityRepositoryImpl): CityRepository
```

API key: read from `BuildConfig.OPEN_WEATHER_API_KEY`, set via `gradle.properties`:
```
OPEN_WEATHER_API_KEY=your_key_here
```
In `:core:data/build.gradle.kts`:
```kotlin
buildFeatures { buildConfig = true }
defaultConfig {
    buildConfigField("String", "OPEN_WEATHER_API_KEY", "\"${project.findProperty("OPEN_WEATHER_API_KEY")}\"")
}
```

---

## 4. `:core:domain` Module

Pure business logic. Thin use cases that consume `:core` interfaces.

```kotlin
package com.weather.core.domain

class GetCurrentWeatherUseCase @Inject constructor(
    private val repo: WeatherRepository
) {
    operator fun invoke(cityName: String): Flow<Resource<CurrentWeather>> = flow {
        val cached = repo.observeCurrentWeather(cityName).first()
        if (cached != null) emit(Resource.Success(cached)) else emit(Resource.Loading)

        val syncResult = repo.sync(cityName)
        if (syncResult is Resource.Error && cached == null) {
            emit(syncResult)
        }
    }.flatMapLatest { state ->
        repo.observeCurrentWeather(cityName).map { weather ->
            if (weather != null) Resource.Success(weather) else state
        }
    }
}

class GetDailyForecastUseCase @Inject constructor(private val repo: WeatherRepository) {
    operator fun invoke(cityName: String): Flow<Resource<List<DailyForecast>>>
    // Same pattern as above with repo.observeDailyForecasts()
}

class GetHourlyForecastUseCase @Inject constructor(private val repo: WeatherRepository) {
    operator fun invoke(cityName: String): Flow<Resource<List<HourlyForecast>>>
    // Same pattern
}

class RefreshWeatherUseCase @Inject constructor(private val repo: WeatherRepository) {
    suspend operator fun invoke(cityName: String): Resource<Unit> = repo.forceSync(cityName)
}

class GetSavedCitiesUseCase @Inject constructor(private val repo: CityRepository) {
    operator fun invoke(): Flow<List<City>> = repo.observeSavedCities()
}

class SearchCitiesUseCase @Inject constructor(private val repo: CityRepository) {
    suspend operator fun invoke(query: String): Resource<List<City>> = repo.searchCities(query)
}

class SaveCityUseCase @Inject constructor(private val repo: CityRepository) {
    suspend operator fun invoke(city: City) = repo.saveCity(city)
}

class RemoveCityUseCase @Inject constructor(private val repo: CityRepository) {
    suspend operator fun invoke(city: City) = repo.removeCity(city)
}

class SelectCityUseCase @Inject constructor(private val repo: CityRepository) {
    suspend operator fun invoke(cityName: String) = repo.selectCity(cityName)
}

class GetSelectedCityUseCase @Inject constructor(private val repo: CityRepository) {
    operator fun invoke(): Flow<String?> = repo.observeSelectedCityName()
}
```

---

## 5. `:core:ui` Module

### Theme

Material3, sky blue primary `#4FC3F7`. Support light + dark.

```kotlin
@Composable
fun WeatherForecastTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) { MaterialTheme(colorScheme = if (darkTheme) darkScheme else lightScheme, content = content) }
```

### Shared Components

```kotlin
@Composable fun WeatherIcon(iconCode: String, modifier: Modifier = Modifier, size: Dp = 48.dp)
// Coil AsyncImage: "https://openweathermap.org/img/wn/${iconCode}@2x.png"

@Composable fun LoadingContent(modifier: Modifier = Modifier)
// Centered CircularProgressIndicator

@Composable fun ErrorContent(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier)
// Column: error icon + message + "Retry" button
```

---

## 6. `:feature:weather`

### WeatherUiState

```kotlin
data class WeatherUiState(
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val isOffline: Boolean = false,
    val cityName: String = "",
    val currentWeather: CurrentWeather? = null,
    val hourlyForecasts: List<HourlyForecast> = emptyList(),
    val dailyForecasts: List<DailyForecast> = emptyList(),
    val error: String? = null
)
```

### WeatherViewModel

```kotlin
@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val getCurrentWeather: GetCurrentWeatherUseCase,
    private val getDailyForecast: GetDailyForecastUseCase,
    private val getHourlyForecast: GetHourlyForecastUseCase,
    private val refreshWeather: RefreshWeatherUseCase,
    private val getSelectedCity: GetSelectedCityUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getSelectedCity().filterNotNull().collectLatest { cityName ->
                _uiState.update { it.copy(cityName = cityName) }
                loadWeather(cityName)
            }
        }
    }

    private fun loadWeather(cityName: String) {
        // Launch 3 parallel collectors for current, daily, hourly
        // Each updates its portion of _uiState
        // Set isLoading = false after first emission from any
    }

    fun refresh() {
        val city = _uiState.value.cityName
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            refreshWeather(city)
            _uiState.update { it.copy(isSyncing = false) }
        }
    }
}
```

### WeatherScreen Composition

```
Scaffold(topBar: city name + location icon button → navigate to CityList)
└─ PullToRefreshBox(isRefreshing = isSyncing)
   └─ if isLoading && no data → LoadingContent
      if error && no data → ErrorContent
      if data →
        LazyColumn
        ├─ CurrentWeatherCard (big temp, icon, description, feels like)
        ├─ WeatherDetailsRow (humidity, wind, pressure — 3 cards in Row)
        ├─ SectionHeader("Today")
        ├─ HourlyForecastRow (LazyRow of HourlyItem cards)
        ├─ SectionHeader("5-Day Forecast")
        └─ DailyForecastItems (forEach, NOT nested LazyColumn)
```

---

## 7. `:feature:citylist`

### CityListUiState

```kotlin
data class CityListUiState(
    val savedCities: List<City> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<City> = emptyList(),
    val isSearching: Boolean = false,
    val selectedCityName: String? = null
)
```

### CityListViewModel

- Collects `getSavedCitiesUseCase()` flow
- Collects `getSelectedCityUseCase()` flow
- Search: `MutableStateFlow<String>` → `debounce(500)` → `flatMapLatest { searchCitiesUseCase(it) }`
- `onCitySelect(city)`: calls `selectCityUseCase(city.name)`, navigates back
- `onCityAdd(city)`: calls `saveCityUseCase(city)`
- `onCityRemove(city)`: calls `removeCityUseCase(city)`

### CityListScreen Composition

```
Scaffold(topBar = "Select City" + back button)
└─ Column
   ├─ OutlinedTextField (search, trailing clear icon)
   ├─ if searchQuery.isNotEmpty() →
   │    LazyColumn: search results with "+" add button
   └─ else →
        LazyColumn: saved cities (highlight selected, trailing delete icon)
```

---

## 8. `:app` Module

### WeatherApp

`@HiltAndroidApp` Application class.

### MainActivity

`@AndroidEntryPoint`. `setContent` with `WeatherForecastTheme` + `NavHost`.

### Navigation

```kotlin
@Composable
fun WeatherNavHost(navController: NavHostController) {
    NavHost(navController, startDestination = "weather") {
        composable("weather") {
            WeatherScreen(onNavigateToCityList = { navController.navigate("city_list") })
        }
        composable("city_list") {
            CityListScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
```

### Default Cities

On first launch (`CityDao.count() == 0`), pre-populate:
```
London (GB), New York (US), Tokyo (JP), Paris (FR), Sydney (AU),
Berlin (DE), Toronto (CA), Singapore (SG), Dubai (AE), São Paulo (BR)
```
Default selected: London.

Logic in `CityRepositoryImpl.initializeIfNeeded()`, called from coroutine in `App.onCreate()`.

### AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

---

## 9. Configuration

- API key: `gradle.properties` → `OPEN_WEATHER_API_KEY=xxx`
- `:core:data` reads it as `BuildConfig.OPEN_WEATHER_API_KEY`
- OkHttp interceptor appends `appid` query param to every request
- `.gitignore`: add note about API key

---

## 10. Phase 1 Deliverables Checklist

- [ ] `./gradlew assembleDebug` passes
- [ ] App launches, shows London weather by default
- [ ] Can switch city from city list
- [ ] Pull-to-refresh works
- [ ] Offline: shows cached data after first load
- [ ] Search cities via geocoding API
- [ ] Error state shown when no network and no cache
- [ ] `README.md` with setup instructions
- [ ] `AI_TOOLS.md` documenting tool usage

---

## 11. Hooks for Phase 2 & 3

These interfaces will be added to `:core` in later phases. No code changes needed in Phase 1 to support them:

- **Phase 2:** `LogPort`, `LogPortFactory`, `FeatureTogglePort` → added to `:core/logging/` and `:core/config/`
- **Phase 3:** `WeatherRealtimeService` → added to `:core/repository/`, impl in `:core:data`
