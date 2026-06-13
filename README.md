# Weather Forecaster

[English](#english) | [繁體中文](#繁體中文)

---

## English

Android weather app built with Clean Architecture, Jetpack Compose, and the OpenWeatherMap API. Includes a Socket.IO push server for real-time weather updates and alerts.

### Getting Started

#### Prerequisites

1. **Android Studio** — download from [developer.android.com/studio](https://developer.android.com/studio) (Hedgehog 2023.1.1 or later). It bundles JDK 17 and the Android SDK, so no separate install is needed.
2. **OpenWeatherMap API key** — sign up for free at [openweathermap.org/api](https://openweathermap.org/api) and copy your key from the dashboard.
3. **Docker** (optional) — only needed if you want to run the real-time push server.

#### Step 1 — Clone the project

```bash
git clone https://github.com/jay41chen/weather-forecaster.git
```

#### Step 2 — Add your API key

Open the project folder and create a file named `local.properties` in the root (next to `build.gradle.kts`). Add this line, replacing the placeholder with your actual key:

```properties
OPEN_WEATHER_API_KEY=your_key_here
```

> `local.properties` is a standard Android config file that stays on your machine — it is git-ignored and will not be committed.

#### Step 3 — Build & run

1. Open Android Studio → **File → Open** → select the project folder.
2. Wait for Gradle sync to finish (progress bar at the bottom).
3. Select a device or emulator from the toolbar, then click **Run ▶**.

The app should launch and show a weather screen for the default city.

<details>
<summary>Command-line alternative</summary>

```bash
cd weather-forecaster
./gradlew assembleDebug
# The APK is at app/build/outputs/apk/debug/app-debug.apk
```
</details>

#### Step 4 — Push server (optional)

The Socket.IO server provides real-time weather push and alerts. The app works without it — skip this step if you just want to try the app.

```bash
cd server
cp .env.example .env
# Open .env in a text editor and paste your OpenWeatherMap API key

# Start with Docker:
docker compose up

# Or with Node.js (v18+):
npm install && npm start
```

The server runs on port 3000. The app connects automatically when running on an Android emulator. For a physical device on the same WiFi, replace the server URL with your computer's local IP address.

### Tech Stack

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

### Architecture

#### Module Dependency Graph

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

#### Data Flow (UDF)

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

#### Project Structure

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

### CI/CD

| Workflow | Trigger | What it does |
|---|---|---|
| **PR Check** (`check.yml`) | Pull request → `main` / `develop` | Run unit tests, build debug APK |
| **Release** (`release.yml`) | Push tag `v*` | Build signed release APK + AAB (R8 minified), publish to GitHub Release |

**Downloading builds:**
- **Release build** — go to [Releases](../../releases) and download `app-release.apk` (direct install) or `app-release.aab` (for Play Store upload).
- **PR / release build** — go to [Actions](../../actions), click the workflow run, and download artifacts from the **Artifacts** section at the bottom of the page.

Release signing is configured via repository secrets. Without secrets, the build falls back to a debug signing key.

### Testing

```bash
./gradlew test
```

12 test files covering ViewModels, use cases, and repositories.

---

## 繁體中文

Android 天氣應用程式，採用 Clean Architecture、Jetpack Compose 與 OpenWeatherMap API 開發。內含 Socket.IO 推播伺服器，提供即時天氣更新與警報。

### 開始使用

#### 事前準備

1. **Android Studio** — 從 [developer.android.com/studio](https://developer.android.com/studio) 下載（Hedgehog 2023.1.1 或更新版本）。安裝後即內建 JDK 17 與 Android SDK，不需額外安裝。
2. **OpenWeatherMap API 金鑰** — 到 [openweathermap.org/api](https://openweathermap.org/api) 免費註冊，從後台複製你的金鑰。
3. **Docker**（選用）— 只有在需要執行即時推播伺服器時才需要。

#### 步驟 1 — 下載專案

```bash
git clone https://github.com/jay41chen/weather-forecaster.git
```

#### 步驟 2 — 設定 API 金鑰

打開專案資料夾，在根目錄（`build.gradle.kts` 旁邊）新建一個檔案，命名為 `local.properties`，加入以下內容，把 `your_key_here` 換成你的金鑰：

```properties
OPEN_WEATHER_API_KEY=your_key_here
```

> `local.properties` 是 Android 標準設定檔，只存在你的電腦上，不會被 commit 到版本控制。

#### 步驟 3 — 建置與執行

1. 開啟 Android Studio → **File → Open** → 選擇專案資料夾。
2. 等待 Gradle 同步完成（畫面底部會顯示進度條）。
3. 在工具列選擇裝置或模擬器，點選 **Run ▶**。

App 應該會啟動並顯示預設城市的天氣畫面。

<details>
<summary>命令列替代方式</summary>

```bash
cd weather-forecaster
./gradlew assembleDebug
# APK 產出位置：app/build/outputs/apk/debug/app-debug.apk
```
</details>

#### 步驟 4 — 推播伺服器（選用）

Socket.IO 伺服器提供即時天氣推播與警報。App 在沒有伺服器的情況下也能正常運作——如果只是想試用 App，可以跳過這一步。

```bash
cd server
cp .env.example .env
# 用文字編輯器打開 .env，貼上你的 OpenWeatherMap API 金鑰

# 用 Docker 啟動：
docker compose up

# 或用 Node.js（v18 以上）：
npm install && npm start
```

伺服器在 port 3000 運行。在 Android 模擬器上 App 會自動連線。如果是實體裝置且在同一個 WiFi，請把伺服器網址改成你電腦的區域網路 IP。

### 技術棧

- **語言**：Kotlin
- **UI**：Jetpack Compose + Material 3
- **非同步**：Kotlin Coroutines + Flow
- **架構**：Clean Architecture（Ports & Adapters）
- **依賴注入**：Hilt
- **網路**：Retrofit + OkHttp
- **本地儲存**：Room（離線快取）+ DataStore（偏好設定）
- **即時通訊**：Socket.IO
- **定位**：Fused Location Provider
- **測試**：JUnit 4 + MockK + kotlinx-coroutines-test

### 架構

#### 模組依賴圖

```
┌─────────────────────────────────────────────────────┐
│                        app                          │
│                  （組合根 / DI）                      │
└──────┬──────────┬──────────┬──────────┬─────────────┘
       │          │          │          │
       ▼          ▼          ▼          ▼
  ┌─────────┐ ┌─────────┐ ┌────────┐ ┌────────┐
  │feature: │ │feature: │ │ core:  │ │ core:  │
  │weather  │ │citylist │ │  data  │ │   ui   │
  │(畫面 +  │ │(畫面 +  │ │(實作層：│ │(共用   │
  │ VM)     │ │ VM)     │ │ DB,API)│ │Compose)│
  └────┬────┘ └────┬────┘ └───┬────┘ └───┬────┘
       │          │           │          │
       ▼          ▼           ▼          ▼
  ┌──────────────────────────────────────────┐
  │              core:domain                 │
  │          （Use Cases 層）                 │
  └──────────────────┬───────────────────────┘
                     │
                     ▼
  ┌──────────────────────────────────────────┐
  │                 core                     │
  │      （Port 介面 + Domain 模型）          │
  └──────────────────────────────────────────┘
```

所有依賴方向朝內——外層依賴內層，絕不反向。Feature 模組不會 import `core:data`；它們只依賴 `core` 的介面與 `core:domain` 的 Use Cases。具體實作透過 Hilt 在 `app` 模組（組合根）中注入。

#### 資料流（UDF）

```
  ┌──────────────────────────────────────────────────────┐
  │                      畫面                            │
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
  │                 遠端 API        本地 DB (Room)        │
  └──────────────────────────────────────────────────────┘
```

- **狀態**往下流：`Repository → UseCase → ViewModel → 畫面`，透過 `StateFlow`
- **事件**往上流：`畫面 → ViewModel`，透過函式呼叫
- **離線優先**：Use Cases 透過 `Flow` 監聽 Room，同時觸發同步。快取資料立即顯示，API 回應後替換為最新資料。

#### 專案結構

```
app/                        # 組合根：Hilt 模組、導航、Application
core/                       # Port 介面、Domain 模型、日誌
  core:data/                # 實作層（Retrofit、Room、Socket.IO）
  core:domain/              # Use Cases（商業邏輯：TTL、去重、同步）
  core:ui/                  # 共用 Compose 元件（ErrorContent、LoadingContent）
feature/
  feature:weather/          # 天氣詳情畫面 + ViewModel
  feature:citylist/         # 城市列表 / 選擇畫面 + ViewModel
demo/                       # 獨立 Demo App，用於測試 Feature Toggle
server/                     # Socket.IO 推播伺服器（Node.js + Docker）
```

### CI/CD

| 工作流程 | 觸發條件 | 執行內容 |
|---|---|---|
| **PR Check** (`check.yml`) | Pull request → `main` / `develop` | 執行單元測試、建置 debug APK |
| **Release** (`release.yml`) | 推送 `v*` 標籤 | 建置已簽章的 release APK + AAB（R8 壓縮），上傳至 GitHub Release |

**下載建置產出：**
- **正式版** — 到 [Releases](../../releases) 下載 `app-release.apk`（直接安裝）或 `app-release.aab`（上架 Play Store 用）。
- **PR / 正式版建置** — 到 [Actions](../../actions)，點進對應的工作流程執行紀錄，在頁面底部的 **Artifacts** 區塊下載。

正式版簽章透過 repository secrets 設定。未設定 secrets 時，會使用 debug 簽章金鑰。

### 測試

```bash
./gradlew test
```

共 12 個測試檔案，涵蓋 ViewModel、Use Cases 與 Repository。
