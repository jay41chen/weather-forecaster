# Proposal: Documentation Accuracy Fixes (+ signing-fallback bug fix)

> Type: Enhancement (documentation accuracy). One PR against `develop`.
> Status: Approved — implementing.

## Motivation

Two independent documentation reviews (clarity lens + code-accuracy lens)
found that the project docs have drifted from the code:

- The root READMEs make one **false claim with a real bug behind it**
  (release signing "falls back to a debug key" — it actually fails),
  draw a module graph with edges that do not exist, name a
  too-old Android Studio version, and never describe what the app does.
- `TOOLS.md` is a 21-line summary although both READMEs promise
  "the complete workflow and findings".
- `server/README.md` omits what the server is, its env vars, and its
  admin/test API.
- `specs/` historical docs contain statements that are now false
  (mechanisms moved, decisions superseded) with no supersession markers,
  which misleads anyone using them to navigate the code.

Every finding below was re-verified against the code before being turned
into a task. One review claim was found to be **wrong and is corrected
here**: the Clean Architecture refactor commit is `3a6764c`
("refactor repository to clean architecture"), not `492917a` as one
review stated — `492917a` does not exist in this repository's history.

## Non-Goals

- **No LICENSE file** this round (explicitly deferred).
- **No screenshots** (deferred) — the new Features section is text-only.
- **No app code changes** beyond the single signing-config check in
  `app/build.gradle.kts` (T1). No refactoring, renaming, or behavior
  changes anywhere else.
- **No rewriting of historical spec content.** Existing entries in
  `specs/phase*/` and `specs/progress.md` are annotated with
  supersession/drift notes, never edited to pretend they said something
  else.
- No expansion of test coverage, CI matrix, or server functionality.

## Current State

Verified facts (file:line references are as of `develop` @ `aa31dfe`):

1. **Signing fallback is broken, and the README claim is false.**
   `.github/workflows/release.yml:33` sets
   `KEYSTORE_PATH: ${{ secrets.KEYSTORE_BASE64 != '' && format(...) || '' }}`
   — when the secret is absent, the env var is set to the **empty
   string**, not left unset. `app/build.gradle.kts:22-23` guards with
   `if (keystorePath != null)`, so `""` passes the check and a `release`
   signing config with `storeFile = file("")` is created;
   `signingConfigs.findByName("release")` then wins over the debug
   fallback at line 41. A tag push without secrets fails at signing.
   `README.md:174` / `README.zh-TW.md:173` claim it "falls back to a
   debug signing key".
2. **Module graph edges are wrong.** `README.md:94-120` (and zh-TW
   95-119) draw `core:data → core:domain` and `core:ui → core:domain`.
   Actual: `core/data/build.gradle.kts:41` and
   `core/ui/build.gradle.kts:28` each depend only on `:core`. The
   English diagram's `core:data` box borders are also misaligned.
   `specs/phase1/design.md:35-43` has the correct edge list, except it
   omits `app → core:domain` which exists (`app/build.gradle.kts:60`).
3. **DI wording overstates the app module.** `README.md:122` says
   concrete implementations are wired via Hilt in `app`. Actual: main
   bindings live in `core:data`'s own Hilt modules
   (`core/data/.../di/DataModule.kt`, `NetworkModule.kt`,
   `DatabaseModule.kt`, `LocationModule.kt`, `CoroutineModule.kt`); the
   app module only binds `ConfigModule` (RemoteFeatureToggleAdapter) and
   `LogModule` (TimberLogPortFactory).
4. **Android Studio version too old.** AGP is 8.5.2
   (`gradle/libs.versions.toml:2`), which requires Android Studio
   Koala 2024.1.1+; README says "Hedgehog 2023.1.1 or later".
5. **UDF diagram incoherent.** `README.md:126-142`: the outer box is
   titled "Screen" but encloses ViewModel/UseCase/Repository/DB; prose
   says state flows `Repository → UseCase → ViewModel → Screen`, which
   contradicts the diagram's layout.
6. `TOOLS.md` is 21 lines, titled "Tools Used", covering one tool;
   README links promise "complete workflow and findings".
7. `server/README.md` has no opening description, no env-var docs
   (`.env.example` defines `PORT`, `POLL_INTERVAL_MS`), no mention of
   the admin endpoints (`server/index.js:129-164`: `GET /admin/status`,
   `POST /admin/test-update`, `POST /admin/test-alert`) or the
   `server/public/index.html` control panel, and does not name the
   `weather_update` payload fields (`server/index.js:41-53`).
8. **Stale specs statements** (all verified):
   - `specs/progress.md:41` describes an "`inFlight + mapMutex`" dedup
     test in `WeatherRepositoryImplTest` — the mechanism now lives in
     `SyncWeatherUseCase` (`cityMutexes` ConcurrentHashMap +
     `computeIfAbsent` + `lastSynced` TTL) and the test is
     `SyncWeatherUseCaseTest.'concurrent calls for same city dedup via
     mutex'`.
   - `specs/progress.md:35` "Deferred: Docker" — `server/Dockerfile`
     and `server/docker-compose.yml` exist.
   - `specs/progress.md:18` says `usesCleartextTraffic="true"` was
     added — the manifest now uses
     `android:networkSecurityConfig="@xml/network_security_config"`
     (`app/src/main/AndroidManifest.xml:10`).
   - `specs/progress.md:42` "Deferred: clock injection into
     `WeatherRepositoryImpl`" — the 30-min TTL moved out of the
     repository into `SyncWeatherUseCase` (clock still not injected).
   - The log stops at "Next: code refinement" (2026-06-11) with ~15
     later commits (refactor, tests 5→12, CI/CD, R8, signing) unlogged.
   - `specs/phase3/design.md` §0.7 and §4.2: SOCKET_URL via
     `gradle.properties`/`BuildConfig` — superseded by runtime
     `socket_url` in `core/src/main/assets/feature_defaults.json`.
   - `specs/phase1/design.md` §3.10 (lines 486-496) and §9: API key via
     `gradle.properties` + `findProperty` — actual is env var +
     `local.properties` (`core/data/build.gradle.kts:11-26`).
   - Interface drift phase1/phase2 vs code: `WeatherRepository` now has
     `sync` + `getCurrentWeatherByCoords`, no `forceSync`
     (`RefreshWeatherUseCase` calls `repo.sync`); `LogPort` methods
     have no `tag` parameter (tag is bound at
     `LogPortFactory.create(tag)`); `FeatureTogglePort` is
     `val configs: StateFlow<Map<String, Any>>` + `suspend refresh()`
     (not `isEnabled(key, default)` over `Map<String, Boolean>`);
     `LocalJsonFeatureToggleAdapter` became `RemoteFeatureToggleAdapter`
     (`core:data`) + `FeatureToggleMockAdapter` (`core`); `LogModule`
     lives in `app/src/main/java/com/weather/app/di/`, not
     `:core/logging/di/`.
   - `specs/phase1/design.md:773` and `specs/phase3/design.md:444`
     reference `AI_TOOLS.md`; the actual file is `TOOLS.md`.
     Deliverables checklists (phase1 §10, phase3 §8/§9) are all
     unchecked although the phases are complete.
     `specs/phase3/tasks.md:64` pins socket.io-client `2.1.0` while
     design §0.5/§4.1 and `core/data/build.gradle.kts:64` use `2.1.2`.
   - `specs/proposals/logger-module/proposal.md` has no status line.
     Verified **implemented**: `TimberLogAdapter`/`TimberLogPortFactory`
     moved to `core/data/.../logging/`, `RedactingLogPort` and
     `LogPortHttpLogger` exist there, `CompositeLogPort`/
     `CompositeLogPortFactory` exist in `:core`, `:core` has no Timber
     dependency, `RedactingLogPortTest`/`CompositeLogPortTest` exist.

All other review findings (Features section missing, Tech Stack
omissions of Coil/kotlinx-serialization/Navigation Compose/Timber,
buried physical-device guidance, Docker-or-Node prerequisites, emulator
pointer, overlapping download bullets, mixed structure notation,
unexplained `demo/` module, duplicated 5→12 stat, `:core` label) were
verified true as stated.

## Proposed State

- `app/build.gradle.kts` guards the release signing config with
  `isNullOrEmpty()`, so an empty `KEYSTORE_PATH` genuinely falls back to
  debug signing; the README claim becomes true as written.
- `README.md` and `README.zh-TW.md`: accurate module graph and UDF
  diagram, correct Android Studio version, a text-only Features section,
  complete Tech Stack, corrected DI wording, surfaced physical-device
  guidance, and the smaller clarity fixes — the two files staying 1:1.
- `TOOLS.md`: a genuine workflow + findings document with a
  commit-hash-backed findings table.
- `server/README.md`: describes what the server is, its env vars, admin
  API, control panel, and event payloads.
- `specs/`: append-only supersession/drift notes; a short
  `specs/README.md` index; logger proposal gains a status line.

## Alternatives Considered

### Signing fix approach (finding 1) — decision embedded in T1

- **A. Gradle-side guard `!keystorePath.isNullOrEmpty()` (CHOSEN).**
  Minimal one-line change at the single point where the decision is
  made. Handles every caller that sets an empty value — the current
  workflow ternary, and local shells (`KEYSTORE_PATH="" ./gradlew ...`).
  The workflow file stays untouched.
- **B. Workflow-side: leave `KEYSTORE_PATH` unset when the secret is
  absent.** GitHub Actions cannot conditionally omit a key from a
  step-level `env:` map with an expression; it would require either a
  prior step writing to `$GITHUB_ENV` under an `if:`, or duplicated
  build steps. More YAML, and it still leaves the Gradle script fragile
  against any other caller exporting an empty var. Rejected.
- **C. Both A and B.** Belt-and-braces, but once A lands, B adds
  complexity with no behavioral difference. Rejected for minimal-change
  discipline.

### Handling stale specs (findings on `specs/`)

- **A. Append-only annotations (CHOSEN).** Add dated supersession /
  drift notes and one backfill session entry; never edit what past
  sessions recorded. Preserves the docs as an honest historical record.
- **B. Rewrite the design docs to match current code.** Rejected: the
  phase docs are point-in-time design artifacts; rewriting them
  falsifies history and loses the record of what was originally decided.
- **C. Delete stale docs and keep only current-state docs.** Rejected:
  the specs are part of the project's demonstrated workflow (referenced
  from TOOLS.md/README) and their history has value.

### PR granularity

- **A. One PR for everything (CHOSEN, per prior scope decision).** All
  changes are doc-accuracy driven; the single riding code change (T1) is
  one line and is what makes the README claim true.
- **B. Separate PR for the signing fix.** Cleaner separation, but the
  README signing wording and the Gradle fix are two halves of the same
  finding; splitting them risks landing the doc claim before it is true.
  Rejected per the already-made scope decision.

## Migration Plan

No migration concerns: T1 is the only behavior-affecting change and only
affects release builds' signing-config selection. Tasks are ordered so
each lands independently buildable; docs tasks are pure content changes.
Order: T1 (code) → T2/T3 (READMEs, T3 depends on T2 text) → T4-T6
(independent) — each step leaves the repo working.

## Risk & Rollback

- **T1 risk:** With secrets present, `KEYSTORE_PATH` is a non-empty
  path — behavior unchanged. With empty/absent value, behavior changes
  from *build failure* to *debug-signed release build*, which is the
  documented intent. Rollback: revert the one-line change.
- **Hash-drift risk (T4):** The commit hashes baked into the TOOLS.md
  findings table were verified on `develop` @ `aa31dfe`
  (2026-07-16). If `develop` history is rewritten before
  implementation, the hashes must be re-verified in Think mode and this
  proposal updated — the implementation task itself must not "look
  them up".
- **Docs risk:** none at runtime. Worst case is a wording regression;
  rollback is `git revert`.

## Verification

- **T1 (the only behavior change):**
  - Testing the "no secrets" path in real CI is **not feasible without
    side effects**: `release.yml` only triggers on a `v*` tag push, and
    exercising the secret-absent branch would require deleting the
    repository secrets and pushing a throwaway tag (which would also
    create a GitHub Release). Local env-var simulation is equivalent,
    because the Gradle script consumes only `System.getenv` values —
    exactly what the workflow provides.
  - Local check (fallback path):
    `KEYSTORE_PATH="" ./gradlew :app:assembleRelease` must succeed, and
    `keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk`
    must show the debug certificate (`CN=Android Debug`).
  - Local check (signed path, proves no regression): generate a
    throwaway keystore in a temp dir
    (`keytool -genkeypair -keystore /tmp/test.keystore -alias test
    -storepass testpass -keypass testpass -keyalg RSA -dname CN=Test`),
    run `KEYSTORE_PATH=/tmp/test.keystore KEYSTORE_PASSWORD=testpass
    KEY_ALIAS=test KEY_PASSWORD=testpass ./gradlew :app:assembleRelease`,
    and `keytool -printcert -jarfile ...app-release.apk` must show
    `CN=Test`.
  - If the implementation environment cannot run Gradle/Android builds,
    the report must say so explicitly (honest acceptance reporting) —
    no claiming an unrun build passed.
- **Docs tasks:** acceptance is content-presence checks (grep) listed
  per task, plus the PR-wide checklist at the end.
- Full suite `./gradlew test` must still pass (no source changes besides
  T1, so this is a smoke check).

## Tasks

Cross-task contract values (verbatim, not to be altered):
- Env var names: `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
  `KEY_PASSWORD`, `OPEN_WEATHER_API_KEY`.
- Config file + key: `core/src/main/assets/feature_defaults.json`,
  key `socket_url`.
- Socket events: `subscribe`, `weather_update`, `weather_alert`.
- Actual commit hashes (verified via `git log` on `develop` @
  `aa31dfe`): see table in T4.

### T1 — Fix release signing fallback (code change)

- **File:** `app/build.gradle.kts` (modify)
- **Change:** In the `signingConfigs` block, replace the guard
  `if (keystorePath != null)` with `if (!keystorePath.isNullOrEmpty())`.
  No other change. Do not touch `.github/workflows/release.yml`.
- **Why (bake-in):** the workflow sets `KEYSTORE_PATH` to `""` (not
  unset) when the `KEYSTORE_BASE64` secret is absent; `""` passes a
  null-only check and creates a broken `release` signing config with
  `storeFile = file("")`, which `findByName("release")` then selects
  instead of the debug fallback.
- **Acceptance:**
  - `KEYSTORE_PATH="" ./gradlew :app:assembleRelease` succeeds;
    `keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk`
    shows `CN=Android Debug`.
  - Throwaway-keystore signed-path check as specified in Verification
    shows `CN=Test`.
  - If builds cannot run in the implementation environment, state that
    explicitly in the report instead.

### T2 — README.md fixes

- **File:** `README.md` (modify). All edits below; keep existing tone
  and section order unless stated.
1. **Features section (new):** insert a `## Features` section between
   the intro paragraph and `## Getting Started`, with exactly these
   bullets (all verified against code):
   - Current conditions, hourly (today) and 5-day forecasts per city
   - City search via OpenWeatherMap geocoding; save, remove, and select
     cities (default city list seeded on first launch)
   - Location-based city detection (Fused Location Provider)
   - Offline-first: Room cache renders immediately; syncs when stale
     (30-minute TTL) with per-city request deduplication
   - Pull-to-refresh
   - Real-time weather updates and alert notifications pushed over
     Socket.IO (optional companion server)
   - Runtime feature toggles with local JSON defaults
   - Material 3 UI with light/dark theme (Jetpack Compose)
2. **Android Studio version:** in Prerequisites item 1, replace
   "Hedgehog 2023.1.1 or later" with "Koala 2024.1.1 or later (required
   by AGP 8.5.2)".
3. **Prerequisites item 3:** replace "**Docker** (optional)" with
   "**Docker or Node.js 18+** (optional) — only needed if you want to
   run the real-time push server."
4. **Step 3:** append one sentence: "First time using an emulator?
   Create one via **Tools → Device Manager** in Android Studio."
5. **Step 4:** in the paragraph after the code block (currently line
   65), after the sentence about physical devices, add: "The server URL
   the app uses is the `socket_url` key in
   `core/src/main/assets/feature_defaults.json`. See
   [server/README.md](server/README.md) for all connection options,
   including USB via `adb reverse tcp:3000 tcp:3000`."
6. **Tech Stack:** add four bullets: "**Image Loading**: Coil",
   "**Serialization**: kotlinx-serialization (JSON)", "**Navigation**:
   Navigation Compose", "**Logging**: Timber (behind a `LogPort`
   abstraction)".
7. **Module dependency graph:** replace the ASCII diagram (lines
   94-120) with the following (fixes the false `core:data → core:domain`
   and `core:ui → core:domain` edges and the misaligned borders):

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

   Keep the explanatory paragraph but apply edit 8 below.
8. **DI wording:** replace "Concrete implementations are wired via Hilt
   in the `app` module (composition root)." with "Adapter bindings live
   in `core:data`'s own Hilt modules (`DataModule`, `NetworkModule`,
   `DatabaseModule`, ...); the `app` module wires only the two
   swap-points that differ per app: `ConfigModule` (feature toggle
   source) and `LogModule` (log sink)."
9. **UDF diagram:** replace the diagram and the two bullets (lines
   126-145) with:

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

   Bullets: "**Events** flow one way down the left side:
   `Screen → ViewModel → UseCase → Repository` via function calls." /
   "**State** flows one way back up the right side:
   `Room → Repository → UseCase → ViewModel → Screen` via
   `Flow`/`StateFlow`." Keep the offline-first bullet unchanged.
10. **Project Structure block:** use directory notation throughout —
    `core/data/`, `core/domain/`, `core/ui/`, `feature/weather/`,
    `feature/citylist/` (matching disk layout). Keep comments.
11. **demo/ line:** extend to "`demo/` — Standalone demo app for
    feature-toggle and logging experiments; run with
    `./gradlew :demo:installDebug` or the `demo` run configuration."
12. **CI/CD download bullets:** replace the two bullets with:
    - "**Tagged releases** — go to [Releases](../../releases) and
      download `app-release.apk` (direct install) or `app-release.aab`
      (Play Store upload)."
    - "**Any other run (including PRs)** — go to
      [Actions](../../actions), open the workflow run, and download
      from the **Artifacts** section at the bottom."
13. **Signing sentence** (line 174): keep the fallback claim (true
    after T1) but make it precise: "Release signing is configured via
    repository secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
    `KEY_ALIAS`, `KEY_PASSWORD`). When they are absent, the build falls
    back to the debug signing key, so tag builds still succeed."
14. **Testing section dedupe:** change "12 test files covering
    ViewModels, use cases, and repositories." to "12 test files
    covering ViewModels, use cases, and repositories (see
    [TOOLS.md](TOOLS.md) for how the suite grew)." and in the
    AI-Assisted Development paragraph drop the trailing ", and growing
    the test suite from 5 to 12 test files" clause (keep the rest).
15. **TOOLS.md link wording** (last line): change to "See
    [TOOLS.md](TOOLS.md) for the workflow and a findings summary."
- **Acceptance:** `grep -n "Koala 2024.1.1" README.md`,
  `grep -n "## Features" README.md`,
  `grep -n "adb reverse tcp:3000" README.md`,
  `grep -n "core:data" README.md` shows no `core:data → core:domain`
  edge in the diagram; `grep -cn "5 to 12" README.md` returns 0.

### T3 — README.zh-TW.md 1:1 sync

- **File:** `README.zh-TW.md` (modify)
- **Change:** apply every T2 edit as a Traditional Chinese translation,
  same sections, same order, same diagrams (diagrams may keep the
  existing zh label style, e.g. 「畫面」, but must show the corrected
  edges/arrows identical to T2's diagrams). File names, module names,
  code identifiers, env var names, and commands stay in English
  verbatim. The zh-TW diagram's box alignment is currently fine — the
  replacement must be equally aligned.
- **Acceptance:** section-heading count and order match `README.md`
  (`grep -c "^## " README.md README.zh-TW.md` equal;
  `grep -c "^### " README.md README.zh-TW.md` equal);
  `grep -n "Koala 2024.1.1" README.zh-TW.md`;
  `grep -n "adb reverse tcp:3000" README.zh-TW.md`.

### T4 — Expand TOOLS.md

- **File:** `TOOLS.md` (rewrite in place)
- **Content requirements:**
  1. Retitle to `# AI-Assisted Development` with a short intro naming
     Claude Code as the tool (keeps finding 17 honest: the doc is about
     one tool used across the whole workflow, so structure by workflow,
     not by tool list).
  2. Keep/expand the **Workflow** section: Think → Do description
     (discuss + prioritize findings before code; minimal focused
     commits; each fix an independent commit).
  3. Keep the "How It Helped" areas (review, refactoring, concurrency,
     tests, ports & adapters compliance).
  4. Add a **Findings & Fixes** table with these verified rows
     (issue → severity → commit). Hashes verified on `develop` @
     `aa31dfe`; if history is rewritten before implementation, refresh
     in Think mode first:

     | Finding | Severity | Fix commit |
     |---|---|---|
     | Concurrency & lifecycle issues (non-atomic `getOrPut`, feature-toggle startup race) | major | `b001399` |
     | Socket.IO reliability & data integrity | major | `cd559f2` |
     | Business rules (TTL, per-city dedup) living in the repository layer | major | `3a6764c` (refactor to Clean Architecture, extracted `SyncWeatherUseCase`) |
     | Test coverage gaps — suite grown 5 → 12 test files | major | `4e4285a` |
     | Feature-flag properties not reactive | minor | `4270cbd` |
     | Missing LazyColumn item keys (recomposition stability) | minor | `371c8cd` (PR #1) |
     | Dead `isOffline` field / `showOfflineBanner` | minor | `be46246` (PR #2) |
     | `ApplicationScope` on `Main.immediate` instead of `Default` | minor | `71d4193` (PR #3) |
     | `BACKOFF_DELAYS` index out of bounds | minor | `268cb3d` (PR #4) |
  5. One closing line noting CI/CD (`3ebd2b4`) and release
     signing/R8/AAB (`7ff0115`, `ab96dda`) were also built in this
     workflow.
- **Acceptance:** `grep -c "3a6764c\|4e4285a\|b001399" TOOLS.md` ≥ 3;
  every hash in the table resolves:
  `git cat-file -e b001399 && git cat-file -e cd559f2 && git cat-file -e 3a6764c && git cat-file -e 4e4285a && git cat-file -e 4270cbd && git cat-file -e 371c8cd && git cat-file -e be46246 && git cat-file -e 71d4193 && git cat-file -e 268cb3d`.

### T5 — Expand server/README.md

- **File:** `server/README.md` (modify)
- **Content requirements:**
  1. Opening paragraph before `## Setup`: a Node.js + Express +
     Socket.IO demo server that polls OpenWeatherMap for each
     subscribed city (default every 60 s), pushes `weather_update` when
     conditions change and `weather_alert` on dramatic changes (>5°C
     shift or weather-category change); listens on port 3000 by
     default.
  2. New `## Configuration` section documenting `.env.example` keys:
     `OPEN_WEATHER_API_KEY` (required), `PORT` (default 3000),
     `POLL_INTERVAL_MS` (default 60000).
  3. New `## Admin & Testing` section: `GET /admin/status` (connection
     count + per-city subscriber counts), `POST /admin/test-update`
     (broadcast a fake `weather_update`; JSON body fields override
     defaults), `POST /admin/test-alert` (broadcast a fake
     `weather_alert`), plus the web control panel served from
     `server/public/` at `http://localhost:3000/`.
  4. In `## Events`, expand the `weather_update` line to name the
     payload fields verbatim: `cityName`, `temperature`, `feelsLike`,
     `description`, `iconCode`, `humidity`, `windSpeed`, `pressure`,
     `timestamp` — and the `weather_alert` fields: `cityName`, `type`,
     `message`, `timestamp`.
- **Acceptance:** `grep -n "POLL_INTERVAL_MS" server/README.md`;
  `grep -n "/admin/test-update" server/README.md`;
  `grep -n "feelsLike" server/README.md`.

### T6 — specs/ annotations (append-only, no history rewriting)

- **Files:** `specs/progress.md`, `specs/phase1/design.md`,
  `specs/phase2/design.md`, `specs/phase3/design.md`,
  `specs/phase3/tasks.md`, `specs/proposals/logger-module/proposal.md`,
  `specs/README.md` (new).
1. **`specs/progress.md`:** update the "Last updated" date and prepend
   a new session entry `### 2026-07-16 (backfill & corrections)` above
   session 3, containing:
   - A backfill summary of the post-"code refinement" arc with commit
     refs: Clean Architecture refactor moving TTL + per-city dedup from
     `WeatherRepositoryImpl` into `SyncWeatherUseCase` (`3a6764c`);
     test suite 5 → 12 files (`4e4285a`); CI/CD workflows (`3ebd2b4`);
     review follow-ups PR #1-#4 (`371c8cd`, `be46246`, `71d4193`,
     `268cb3d`); release signing + R8/AAB (`7ff0115`, `ab96dda`);
     reactive feature flags (`4270cbd`).
   - Explicit supersession bullets (do not edit the old entries):
     "the dedup test noted on 2026-06-11 now lives in
     `SyncWeatherUseCaseTest` ('concurrent calls for same city dedup
     via mutex'); mechanism is `cityMutexes` + `computeIfAbsent` +
     `lastSynced` TTL in `SyncWeatherUseCase`, not `inFlight+mapMutex`
     in the repository" / "'Deferred: Docker' is done —
     `server/Dockerfile` + `docker-compose.yml` exist" /
     "`usesCleartextTraffic` was later replaced by
     `networkSecurityConfig` (`app/src/main/AndroidManifest.xml`)" /
     "clock-injection deferral: the TTL moved to `SyncWeatherUseCase`;
     a clock is still not injected (uses `System.currentTimeMillis()`)".
2. **`specs/phase3/design.md`:** directly under decision §0.7 and again
   at §4.2, insert a one-line blockquote:
   `> **Superseded:** SOCKET_URL moved from compile-time BuildConfig to the runtime "socket_url" key in core/src/main/assets/feature_defaults.json (see specs/progress.md, 2026-06-11 session 2).`
3. **`specs/phase1/design.md`:** insert one `> **Implementation
   drift**` blockquote directly after the `# ...Phase 1: MVP` title
   block, listing (verbatim facts): API key comes from the
   `OPEN_WEATHER_API_KEY` env var with `local.properties` fallback
   (`core/data/build.gradle.kts`), not `gradle.properties`/
   `findProperty` (§3.10, §9); `WeatherRepository` has no `forceSync` —
   staleness/TTL moved to `SyncWeatherUseCase`, and
   `getCurrentWeatherByCoords` was added; `app` additionally depends on
   `core:domain` (graph §"Dependency Graph"); the deliverable file is
   `TOOLS.md`, not `AI_TOOLS.md`. Then append `— all Phase 1
   deliverables were completed` as a note above the §10 checklist and
   tick its boxes (`[x]`).
4. **`specs/phase2/design.md`:** insert one `> **Implementation
   drift**` blockquote after the title, listing: `LogPort` methods take
   no `tag` parameter (tag bound at `LogPortFactory.create(tag)`);
   `LogModule` lives in `app/src/main/java/com/weather/app/di/`, and
   Timber adapters were later moved to `core:data`
   (see `specs/proposals/logger-module/proposal.md`);
   `FeatureTogglePort` is now `val configs: StateFlow<Map<String, Any>>`
   + `suspend refresh()` with `isEnabled()`/`getString()` extension
   functions; `LocalJsonFeatureToggleAdapter` was replaced by
   `RemoteFeatureToggleAdapter` (`core:data`) and
   `FeatureToggleMockAdapter` (`core`).
5. **`specs/phase3/design.md` + tasks:** in `specs/phase3/tasks.md`
   line with `socket.io-client:2.1.0`, add trailing note
   `<!-- actual: 2.1.2, matching design §0.5 -->`; in
   `specs/phase3/design.md` §8 and §9 checklists, tick the boxes and
   fix the `AI_TOOLS.md` reference to `TOOLS.md` (annotate as
   `TOOLS.md` — a rename note is acceptable: "`TOOLS.md` (originally
   planned as `AI_TOOLS.md`)"). Same `AI_TOOLS.md` fix in
   `specs/phase1/design.md` §10.
6. **`specs/proposals/logger-module/proposal.md`:** add directly under
   the title: `> Status: Implemented — TimberLogAdapter/`
   `TimberLogPortFactory moved to core:data/logging with RedactingLogPort`
   `and LogPortHttpLogger; CompositeLogPort/CompositeLogPortFactory added`
   `to :core (not yet wired in DI — single-consumer stage as planned).`
7. **`specs/README.md` (new):** one short paragraph + list: layout
   (`phase1-3` design/tasks = point-in-time design docs, annotated with
   drift notes rather than rewritten; `progress.md` = session log;
   `proposals/` = refactor/enhancement proposals), suggested reading
   order (progress.md → phase designs → proposals), and a language note
   (docs are English except the Phase 1 Goal line and the logger
   proposal, which are in Chinese).
- **Acceptance:** `grep -n "Superseded" specs/phase3/design.md` returns
  2+ hits; `grep -n "Implementation drift" specs/phase1/design.md
  specs/phase2/design.md` returns 1 hit each;
  `grep -n "Status: Implemented" specs/proposals/logger-module/proposal.md`;
  `grep -rn "AI_TOOLS.md" specs/ | grep -v "originally planned"`
  returns 0 rows; `test -f specs/README.md`;
  `grep -n "2026-07-16" specs/progress.md`.

## PR Acceptance Checklist

- [ ] `KEYSTORE_PATH="" ./gradlew :app:assembleRelease` succeeds and
      the APK is debug-signed (`keytool -printcert -jarfile ...` shows
      `CN=Android Debug`) — or the report explicitly states builds
      could not run in the environment.
- [ ] Throwaway-keystore signed build shows `CN=Test` (same caveat).
- [ ] `./gradlew test` passes.
- [ ] `git diff --stat develop` touches only: `app/build.gradle.kts`,
      `README.md`, `README.zh-TW.md`, `TOOLS.md`, `server/README.md`,
      `specs/progress.md`, `specs/phase1/design.md`,
      `specs/phase2/design.md`, `specs/phase3/design.md`,
      `specs/phase3/tasks.md`,
      `specs/proposals/logger-module/proposal.md`, `specs/README.md`,
      and `specs/proposals/doc-fixes/` (this doc + progress.md).
- [ ] README.md and README.zh-TW.md have identical heading structure
      (`grep -c "^## \|^### "` equal) and both contain the corrected
      module graph (no `core:data → core:domain` or
      `core:ui → core:domain` edge), the Features section, and
      "Koala 2024.1.1".
- [ ] All commit hashes cited in TOOLS.md and specs/progress.md resolve
      via `git cat-file -e <hash>`.
- [ ] All per-task grep acceptance checks above pass.
- [ ] Independent review via `/review-diff` before push (per working
      principles); re-audit any post-review commits.
