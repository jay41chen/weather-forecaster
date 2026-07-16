# Progress

> Last updated: 2026-07-16

## Status

| Phase | Status | Notes |
|---|---|---|
| Phase 1: MVP | Complete | All 12 tasks done |
| Phase 2: Log Kit + Feature Toggle | Complete | All 6 tasks done; extras beyond spec: `RemoteFeatureToggleAdapter`, `FeatureToggleMockAdapter`, `RetryInterceptor`, `ApiErrorMapper`, `LocationRepository`, `DetectAndSelectCityUseCase`, `:demo` module |
| Phase 3: Socket.IO Realtime | Complete | All 6 tasks done |

## Session Log

### 2026-07-16 (backfill & corrections)

This is a backfill entry: the log below stopped at "Next: code refinement"
(2026-06-11, session 3) although ~15 more commits landed since. Recorded
here, append-only, without editing the prior entries.

- Clean Architecture refactor (`3a6764c`): moved cache TTL and per-city
  request dedup out of `WeatherRepositoryImpl` and into a dedicated
  `SyncWeatherUseCase` (`cityMutexes` ConcurrentHashMap +
  `computeIfAbsent` + `lastSynced` TTL).
- Test suite grown from 5 to 12 test files (`4e4285a`).
- CI/CD workflows added (`3ebd2b4`): PR checks + tagged-release build.
- Review follow-ups PR #1–#4: LazyColumn item keys (`371c8cd`), dead
  `isOffline`/`showOfflineBanner` removal (`be46246`), `ApplicationScope`
  dispatcher `Main.immediate` → `Default` (`71d4193`), `BACKOFF_DELAYS`
  index-out-of-bounds guard (`268cb3d`).
- Release signing config + R8 minification + AAB output (`7ff0115`,
  `ab96dda`).
- Feature flag properties made reactive via `StateFlow` (`4270cbd`).

Explicit supersession notes (prior entries below are left unedited):
- The dedup test noted on 2026-06-11 ("`inFlight + mapMutex`" in
  `WeatherRepositoryImplTest`) now lives in `SyncWeatherUseCaseTest`
  ('concurrent calls for same city dedup via mutex'); the mechanism is
  `cityMutexes` + `computeIfAbsent` + `lastSynced` TTL in
  `SyncWeatherUseCase`, not `inFlight+mapMutex` in the repository.
- "Deferred: Docker" (2026-06-11, session 2) is done —
  `server/Dockerfile` + `server/docker-compose.yml` exist.
- `usesCleartextTraffic="true"` (2026-06-11, session 3) was later
  replaced by `android:networkSecurityConfig="@xml/network_security_config"`
  (`app/src/main/AndroidManifest.xml`).
- Clock-injection deferral (2026-06-11): the TTL moved to
  `SyncWeatherUseCase`; a clock is still not injected (uses
  `System.currentTimeMillis()`).

### 2026-06-11 (session 3)

- Phase 3 Task 6 (e2e verification) completed:
  - Added `android:usesCleartextTraffic="true"` to `AndroidManifest.xml` for local HTTP Socket.IO connection (Android 9+ blocks cleartext by default).
  - Added error handling + button feedback to control panel (`server/public/index.html`).
  - Verified: Socket.IO connects, weather updates propagate to UI, alert snackbar displays.
  - Remaining optional: flag guard test (`socket_io_enabled=false` → no crash).
- **All three phases development complete.** Next: code refinement.

### 2026-06-11 (session 2)

- Phase 3 Tasks 1–5 completed:
  - Task 1: Node.js server (`server/index.js`, Express + Socket.IO, OpenWeatherMap polling).
  - Task 2: `WeatherAlert` model + `WeatherRealtimeService` interface in `:core`.
  - Task 3: `SocketIORealtimeServiceImpl` in `:core:data` — Socket.IO client with reconnection, weather update/alert handlers, DAO write-through.
  - Task 4: `WeatherViewModel` + `WeatherScreen` realtime integration — feature-flag-guarded connect/subscribe, alert snackbar.
  - Task 5: Enabled `socket_io_enabled` and `weather_alerts_enabled` in `feature_defaults.json`.
- Moved `SOCKET_URL` from `gradle.properties`/`BuildConfig` into `feature_defaults.json` as `socket_url` string config — centralizes all runtime config.
- Refactored `FeatureTogglePort` from `Map<String, Boolean>` to `Map<String, Any>` — single map for both boolean flags and string configs. Extension functions `isEnabled()` / `getString()` handle type casting.
- `SocketIORealtimeServiceImpl` now reads URL from `FeatureTogglePort` at runtime instead of compile-time `BuildConfig`.
- Deferred: Docker container for server.

### 2026-06-11

- Audited codebase against spec: Phase 1 + Phase 2 fully implemented.
- Added `SeedDefaultCitiesUseCase.DEFAULT_CITIES`: Taipei moved to first position, Dubai removed, Rome/IT added; `selectCity("London")` unchanged.
- Added deduplication test to `WeatherRepositoryImplTest`: `concurrent sync calls for same city deduplicate to single network request` — verifies `inFlight + mapMutex` mechanism via `CompletableDeferred` latch + `coVerify(exactly = 1)`.
- Deferred: clock injection into `WeatherRepositoryImpl` — architectural decision pending on whether 30-min staleness threshold belongs in domain or data layer.
