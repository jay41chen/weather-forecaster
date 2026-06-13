# Weather Forecaster

[English](README.md) | **繁體中文**

Android 天氣應用程式，採用 Clean Architecture、Jetpack Compose 與 OpenWeatherMap API 開發。內含 Socket.IO 推播伺服器，提供即時天氣更新與警報。

## 開始使用

### 事前準備

1. **Android Studio** — 從 [developer.android.com/studio](https://developer.android.com/studio) 下載（Hedgehog 2023.1.1 或更新版本）。安裝後即內建 JDK 17 與 Android SDK，不需額外安裝。
2. **OpenWeatherMap API 金鑰** — 到 [openweathermap.org/api](https://openweathermap.org/api) 免費註冊，從後台複製你的金鑰。
3. **Docker**（選用）— 只有在需要執行即時推播伺服器時才需要。

### 步驟 1 — 下載專案

```bash
git clone https://github.com/jay41chen/weather-forecaster.git
```

### 步驟 2 — 設定 API 金鑰

打開專案資料夾，在根目錄（`build.gradle.kts` 旁邊）新建一個檔案，命名為 `local.properties`，加入以下內容，把 `your_key_here` 換成你的金鑰：

```properties
OPEN_WEATHER_API_KEY=your_key_here
```

> `local.properties` 是 Android 標準設定檔，只存在你的電腦上，不會被 commit 到版本控制。

### 步驟 3 — 建置與執行

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

### 步驟 4 — 推播伺服器（選用）

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

## 技術棧

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

## 架構

### 模組依賴圖

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

### 資料流（UDF）

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

### 專案結構

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

## CI/CD

| 工作流程 | 觸發條件 | 執行內容 |
|---|---|---|
| **PR Check** (`check.yml`) | Pull request → `main` / `develop` | 執行單元測試、建置 debug APK |
| **Release** (`release.yml`) | 推送 `v*` 標籤 | 建置已簽章的 release APK + AAB（R8 壓縮），上傳至 GitHub Release |

**下載建置產出：**
- **正式版** — 到 [Releases](../../releases) 下載 `app-release.apk`（直接安裝）或 `app-release.aab`（上架 Play Store 用）。
- **PR / 正式版建置** — 到 [Actions](../../actions)，點進對應的工作流程執行紀錄，在頁面底部的 **Artifacts** 區塊下載。

正式版簽章透過 repository secrets 設定。未設定 secrets 時，會使用 debug 簽章金鑰。

## 測試

```bash
./gradlew test
```

共 12 個測試檔案，涵蓋 ViewModel、Use Cases 與 Repository。
