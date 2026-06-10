# Logger Module Enhancement

## Motivation

三個問題驅動這次改動：

1. **Domain purity violation**: `:core`（interface/model 層）依賴 `timber` — 一個 Android logging 框架。`TimberLogAdapter` 和 `TimberLogPortFactory` 放在 `:core/logging/` 裡，跟它們實作的 interface 同一個 package。這和我們在 `LocationRepository` 修的問題一樣 — domain 層不應該知道具體的 framework implementation。

2. **Single consumer, no fan-out**: 目前的 `LogPortFactory` 硬綁 `TimberLogAdapter` 一個 consumer。無法同時掛多個 consumer（例如 local file + remote service）。Logger kit 應該支援 produce/consume 分離 — producer 透過 `LogPort` 寫 log，多個 consumer 各自處理。

3. **HTTP logs bypass LogPort**: `HttpLoggingInterceptor` 直接寫到 `java.util.logging.Logger`，不經過 `LogPort` pipeline。未來加上新 consumer 時，HTTP log 不會被導入。

## Non-Goals

- 實作具體的 remote logging backend（Google CLS、Crashlytics、Datadog）— 另開 spec
- 實作 file-based local logger — 另開 spec
- 改變 `LogPort` interface contract（`.d()` `.i()` `.w()` `.e()` 不變）
- 新增 log level 或 structured log format（JSON）
- Log sampling、rate limiting、buffering

## Current State

```
:core/build.gradle.kts
  └── implementation(libs.timber)          ← framework dependency in domain layer

:core/logging/
  ├── LogPort.kt                          (interface)
  ├── LogPortFactory.kt                   (interface)
  ├── TimberLogAdapter.kt                 (implementation — depends on Timber)
  └── TimberLogPortFactory.kt             (implementation)

:app/di/LogModule.kt
  └── binds LogPortFactory → TimberLogPortFactory

:demo/di/DemoLogModule.kt
  └── binds LogPortFactory → TimberLogPortFactory

:core:data/di/NetworkModule.kt
  └── HttpLoggingInterceptor(Level.BASIC)  → logs to java.util.logging directly
```

Data flow — single consumer, OkHttp 獨立路徑：

```
App code  → LogPort.d("msg", extras)
          → TimberLogAdapter → Timber → Logcat

OkHttp    → HttpLoggingInterceptor → java.util.logging → Logcat
             (independent path, not part of LogPort pipeline)
```

## Proposed State

```
:core/logging/                             (interfaces + composition)
  ├── LogPort.kt                          (unchanged)
  ├── LogPortFactory.kt                   (unchanged)
  ├── CompositeLogPort.kt                 (NEW — fan-out to N consumers)
  └── CompositeLogPortFactory.kt          (NEW — creates CompositeLogPort from N factories)

:core:data/build.gradle.kts
  └── implementation(libs.timber)          ← moved from :core

:core:data/logging/                        (implementations)
  ├── TimberLogAdapter.kt                 (moved from :core, package changed)
  ├── TimberLogPortFactory.kt             (moved from :core, creates plain TimberLogAdapter)
  ├── RedactingLogPort.kt                 (NEW — per-consumer decorator)
  └── LogPortHttpLogger.kt               (NEW — bridges OkHttp → LogPort)

:app/di/LogModule.kt
  └── binds LogPortFactory → TimberLogPortFactory (import path changed)
```

Data flow — 現階段（single consumer，但架構 ready for multi-consumer）：

```
App code  → LogPort.d("msg", extras)
          → TimberLogAdapter → Timber → Logcat

OkHttp    → HttpLoggingInterceptor
          → LogPortHttpLogger → LogPort.d(message)
          → TimberLogAdapter → Timber → Logcat
```

Data flow — 未來掛上多個 consumer 時：

```
App code  → LogPort.d("msg", extras)
          → CompositeLogPort
              ├── TimberLogAdapter              → Logcat (trusted, no redaction)
              ├── FileLogAdapter                → local file on device
              └── RedactingLogPort
                    └── RemoteLogAdapter        → Google CLS (redacted)

OkHttp    → LogPortHttpLogger → same CompositeLogPort → same fan-out
```

### Key Design Decisions

**Produce/consume separation**: `LogPort` 是 producer API — caller 只管寫 log。Consumer 是 `LogPort` 的 implementation（`TimberLogAdapter`、未來的 `FileLogAdapter`、`RemoteLogAdapter` 等）。`CompositeLogPort` 把一個 producer 的 output fan-out 到多個 consumer。

**CompositeLogPort in `:core`**: 純 interface 層的 composition utility — 只依賴 `LogPort`，沒有任何 framework dependency。放在 `:core` 讓任何 module 都能組合 consumer。

**Per-consumer redaction**: `RedactingLogPort` 是 decorator，包在特定 consumer 外面。Logcat 是 trusted channel，不需要 redact。Remote consumer 才需要。Redaction 是 consumer 的決定，不是 producer 的決定。

**OkHttp bridge**: `LogPortHttpLogger` 實作 `HttpLoggingInterceptor.Logger`，把 HTTP log 導入 `LogPort` pipeline。當 `CompositeLogPort` 接上後，HTTP log 自動 fan-out 到所有 consumer。

### New Files

**`CompositeLogPort.kt`** (`:core`)

```kotlin
package com.weather.core.logging

class CompositeLogPort(private val delegates: List<LogPort>) : LogPort {

    override fun d(message: String, extras: Map<String, Any?>) =
        delegates.forEach { it.d(message, extras) }

    override fun i(message: String, extras: Map<String, Any?>) =
        delegates.forEach { it.i(message, extras) }

    override fun w(message: String, throwable: Throwable?, extras: Map<String, Any?>) =
        delegates.forEach { it.w(message, throwable, extras) }

    override fun e(message: String, throwable: Throwable?, extras: Map<String, Any?>) =
        delegates.forEach { it.e(message, throwable, extras) }
}
```

**`CompositeLogPortFactory.kt`** (`:core`)

```kotlin
package com.weather.core.logging

class CompositeLogPortFactory(
    private val factories: List<LogPortFactory>
) : LogPortFactory {
    override fun create(tag: String): LogPort =
        CompositeLogPort(factories.map { it.create(tag) })
}
```

**`RedactingLogPort.kt`** (`:core:data`)

```kotlin
package com.weather.core.data.logging

import com.weather.core.logging.LogPort

class RedactingLogPort(
    private val delegate: LogPort,
    private val patterns: List<Pair<Regex, String>>
) : LogPort {

    override fun d(message: String, extras: Map<String, Any?>) =
        delegate.d(redact(message), redactExtras(extras))

    override fun i(message: String, extras: Map<String, Any?>) =
        delegate.i(redact(message), redactExtras(extras))

    override fun w(message: String, throwable: Throwable?, extras: Map<String, Any?>) =
        delegate.w(redact(message), throwable, redactExtras(extras))

    override fun e(message: String, throwable: Throwable?, extras: Map<String, Any?>) =
        delegate.e(redact(message), throwable, redactExtras(extras))

    private fun redact(text: String): String =
        patterns.fold(text) { acc, (regex, mask) -> regex.replace(acc, mask) }

    private fun redactExtras(extras: Map<String, Any?>): Map<String, Any?> =
        if (extras.isEmpty()) extras
        else extras.mapValues { (_, v) -> if (v is String) redact(v) else v }
}
```

**`LogPortHttpLogger.kt`** (`:core:data`)

```kotlin
package com.weather.core.data.logging

import com.weather.core.logging.LogPort
import okhttp3.logging.HttpLoggingInterceptor

class LogPortHttpLogger(private val log: LogPort) : HttpLoggingInterceptor.Logger {
    override fun log(message: String) {
        log.d(message)
    }
}
```

**Updated `TimberLogPortFactory.kt`** (moved, no redaction wrapping)

```kotlin
package com.weather.core.data.logging

import com.weather.core.logging.LogPort
import com.weather.core.logging.LogPortFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimberLogPortFactory @Inject constructor() : LogPortFactory {
    override fun create(tag: String): LogPort = TimberLogAdapter(tag)
}
```

### Extension Point — 掛新 consumer

未來要加 consumer（例如 Google CLS），只需要：

1. 實作新的 `LogPort`（例如 `CloudLoggingAdapter`）
2. 實作對應的 `LogPortFactory`（需要 redaction 就包 `RedactingLogPort`）
3. 更新 DI — 改用 `CompositeLogPortFactory`：

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object LogModule {
    @Provides
    @Singleton
    fun provideLogPortFactory(
        timberFactory: TimberLogPortFactory,
        remoteFactory: RemoteLogPortFactory  // wraps RedactingLogPort internally
    ): LogPortFactory = CompositeLogPortFactory(listOf(timberFactory, remoteFactory))
}
```

Producer 端（repository、ViewModel 等）零改動。

## Alternatives Considered

### A: Timber Tree 做 multi-consumer

Timber 本身支援 `Timber.plant(tree1, tree2)` — 每個 Tree 是一個 consumer。不需要 `CompositeLogPort`。

**Why not**: Consumer 邏輯綁定在 Timber 的 `Tree` API 上。如果未來換掉 Timber（例如改用 kotlin-logging），所有 consumer 都要重寫。`CompositeLogPort` 讓 consumer 只依賴 `LogPort` interface，跟 Timber 完全解耦。

### B: New `:core:logging` module

建立獨立的 logging module，把 interface、composition utility 和 implementation 都放進去。

**Why not**: 為了幾個 file 開一個 module 太重。Interface + composition（`CompositeLogPort`）放 `:core` 很自然，implementation 放 `:core:data` 跟其他 impl（`LocationRepositoryImpl` 等）一致。只有當多個 feature module 需要各自提供 log backend 時，獨立 module 才值得 — 目前沒有這個需求。

## Migration Plan

每一步獨立 compile 並通過 test。

### Step 1: Move files + dependency (pure refactor, zero behavior change)

- `TimberLogAdapter.kt` → `core/data/src/.../logging/`，package 改為 `com.weather.core.data.logging`
- `TimberLogPortFactory.kt` → 同上
- `implementation(libs.timber)` 從 `:core` 移到 `:core:data`
- 更新 `:app/di/LogModule.kt` 和 `:demo/di/DemoLogModule.kt` 的 import
- 刪除 `:core/logging/` 裡的兩個 implementation file

### Step 2: Add CompositeLogPort (`:core`, no wiring yet)

- 新增 `CompositeLogPort.kt` 和 `CompositeLogPortFactory.kt` 到 `:core/logging/`
- 新增 unit tests
- DI 不變 — 仍然直接 bind `TimberLogPortFactory`

### Step 3: Add RedactingLogPort (`:core:data`, no wiring yet)

- 新增 `RedactingLogPort.kt` 到 `:core:data/logging/`
- 新增 `RedactingLogPortTest.kt`
- 不掛到任何 consumer — 等 remote consumer spec 時才 wire

### Step 4: Route OkHttp logs through LogPort

- 新增 `LogPortHttpLogger.kt`
- `NetworkModule.provideOkHttpClient()` 加入 `logFactory: LogPortFactory` 參數
- 建立 `LogPortHttpLogger(logFactory.create("OkHttp"))` 傳給 `HttpLoggingInterceptor(httpLogger)`

## Risk & Rollback

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Import path 變更漏改某個 module | Low | Low | Compile error 會立刻告訴你。Step 1 明確列出所有需要改的 file。 |
| `CompositeLogPort` 的 `forEach` 其中一個 consumer throw → 後面的 consumer 收不到 | Medium | Medium | Unit test 覆蓋這個 case。目前只有一個 consumer 所以不影響。加第二個 consumer 時再決定是否加 try-catch。 |
| `LogPortHttpLogger` 改變 OkHttp log 格式 | Low | Low | `message` 原封不動傳給 `LogPort.d()`。格式由 `HttpLoggingInterceptor` 決定，不是 logger。 |

Rollback: revert commit。每個 step 是獨立 commit，rollback 粒度跟風險匹配。

## Verification

- `./gradlew :core:assemble` — 確認 `:core` 沒有 Timber import error
- `./gradlew :core:test` — `CompositeLogPortTest` 通過
- `./gradlew :core:data:test` — `RedactingLogPortTest` 通過
- `./gradlew assembleDebug` — full app builds
- Manual: 跑 app，看 Logcat — HTTP request log 出現且 tag 為 `OkHttp`（確認 routing 正常）

## Tasks

### Task 1: Move Timber implementation to `:core:data`

**Files:**
- **New**: `core/data/src/main/java/com/weather/core/data/logging/TimberLogAdapter.kt`
- **New**: `core/data/src/main/java/com/weather/core/data/logging/TimberLogPortFactory.kt`
- **Modify**: `core/build.gradle.kts` — remove `implementation(libs.timber)`
- **Modify**: `core/data/build.gradle.kts` — add `implementation(libs.timber)`
- **Modify**: `app/src/main/java/com/weather/app/di/LogModule.kt` — update import
- **Modify**: `demo/src/main/java/com/weather/demo/di/DemoLogModule.kt` — update import
- **Delete**: `core/src/main/java/com/weather/core/logging/TimberLogAdapter.kt`
- **Delete**: `core/src/main/java/com/weather/core/logging/TimberLogPortFactory.kt`

**Steps:**
1. Create `TimberLogAdapter.kt` in `core/data/.../logging/`，package 改為 `com.weather.core.data.logging`，內容不變
2. Create `TimberLogPortFactory.kt` in same directory，package 改為 `com.weather.core.data.logging`，內容不變
3. `core/build.gradle.kts`: remove `implementation(libs.timber)`
4. `core/data/build.gradle.kts`: add `implementation(libs.timber)`
5. `app/di/LogModule.kt`: import `com.weather.core.data.logging.TimberLogPortFactory`
6. `demo/di/DemoLogModule.kt`: same import change
7. Delete original files from `core/src/.../logging/`

**Acceptance criteria:**
- `./gradlew :core:assemble` — no Timber references
- `./gradlew assembleDebug` — success
- `./gradlew :core:data:test` — existing tests pass

---

### Task 2: Add CompositeLogPort

**Files:**
- **New**: `core/src/main/java/com/weather/core/logging/CompositeLogPort.kt`
- **New**: `core/src/main/java/com/weather/core/logging/CompositeLogPortFactory.kt`
- **New**: `core/src/test/java/com/weather/core/logging/CompositeLogPortTest.kt`

**Steps:**
1. Create `CompositeLogPort` — `LogPort` implementation that delegates to `List<LogPort>`
2. Create `CompositeLogPortFactory` — `LogPortFactory` implementation that delegates to `List<LogPortFactory>`
3. Unit tests:
   - Fan-out: message delivered to all delegates
   - Empty delegates list: no crash
   - Extras and throwable passed through to all delegates

**Acceptance criteria:**
- `./gradlew :core:test` — `CompositeLogPortTest` passes
- `./gradlew assembleDebug` — success

---

### Task 3: Add RedactingLogPort

**Files:**
- **New**: `core/data/src/main/java/com/weather/core/data/logging/RedactingLogPort.kt`
- **New**: `core/data/src/test/java/com/weather/core/data/logging/RedactingLogPortTest.kt`

**Steps:**
1. Create `RedactingLogPort` — decorator wrapping `LogPort`，scrub message + string-typed extras
2. Default pattern constant: `Regex("[a-fA-F0-9]{32,}")` → `"***REDACTED***"`
3. Unit tests:
   - Message containing 32-char hex → redacted
   - Extras value containing hex → redacted
   - Non-matching message → unchanged
   - Empty extras → unchanged (short-circuit, no allocation)
   - Throwable passed through unmodified

**Acceptance criteria:**
- `./gradlew :core:data:test` — `RedactingLogPortTest` passes
- `./gradlew assembleDebug` — success

---

### Task 4: Route HttpLoggingInterceptor through LogPort

**Files:**
- **New**: `core/data/src/main/java/com/weather/core/data/logging/LogPortHttpLogger.kt`
- **Modify**: `core/data/src/main/java/com/weather/core/data/di/NetworkModule.kt`

**Steps:**
1. Create `LogPortHttpLogger` implementing `HttpLoggingInterceptor.Logger`
2. `NetworkModule.provideOkHttpClient()`: add `logFactory: LogPortFactory` parameter
3. Create `val httpLogger = LogPortHttpLogger(logFactory.create("OkHttp"))`
4. Change `HttpLoggingInterceptor()` → `HttpLoggingInterceptor(httpLogger)`

**Acceptance criteria:**
- `./gradlew assembleDebug` — success
- Manual: Logcat 裡 HTTP log 出現且 tag 為 `OkHttp`
