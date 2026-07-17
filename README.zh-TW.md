# Weather Forecaster

[English](README.md) | **繁體中文**

Android 天氣應用程式，採用 Clean Architecture、Jetpack Compose 與 OpenWeatherMap API 開發。內含 Socket.IO 推播伺服器，提供即時天氣更新與警報。

## 功能特色

- 每個城市的目前天氣、今日每小時預報與 5 天預報
- 透過 OpenWeatherMap geocoding 搜尋城市；可儲存、移除、選擇城市（首次啟動會預先建立預設城市清單）
- 依定位偵測城市（Fused Location Provider）
- 離線優先：Room 快取立即顯示，資料過期時（30 分鐘 TTL）才同步，並依城市做請求去重
- 下拉刷新
- 透過 Socket.IO 推播即時天氣更新與警報通知（選用的搭配伺服器）
- 執行期功能開關，內建本地 JSON 預設值
- Material 3 UI，支援亮／暗主題（Jetpack Compose）

## 開始使用

### 事前準備

1. **Android Studio** — 從 [developer.android.com/studio](https://developer.android.com/studio) 下載（Koala 2024.1.1 或更新版本；AGP 8.5.2 所需）。安裝後即內建 JDK 17 與 Android SDK，不需額外安裝。
2. **OpenWeatherMap API 金鑰** — 到 [openweathermap.org/api](https://openweathermap.org/api) 免費註冊，從後台複製你的金鑰。
3. **Docker 或 Node.js 18+**（選用）— 只有在需要執行即時推播伺服器時才需要。

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

App 應該會啟動並顯示預設城市的天氣畫面。第一次使用模擬器？可以在 Android Studio 的 **Tools → Device Manager** 建立一個。

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

伺服器在 port 3000 運行。在 Android 模擬器上 App 會自動連線。如果是實體裝置且在同一個 WiFi，請把伺服器網址改成你電腦的區域網路 IP。App 使用的伺服器網址是 `core/src/main/assets/feature_defaults.json` 中的 `socket_url` 這個 key。所有連線方式（包含透過 `adb reverse tcp:3000 tcp:3000` 使用 USB）請見 [server/README.md](server/README.md)。

<details>
<summary>更換伺服器 port</summary>

如果 port 3000 已被佔用，可以在兩個地方修改：

1. **伺服器端** — 修改 `server/.env` 中的 `PORT`（例如 `PORT=4000`），或啟動時指定：`PORT=4000 npm start`
2. **App 端** — 修改 `core/src/main/assets/feature_defaults.json` 中的 `socket_url`（例如 `http://10.0.2.2:4000`）。如果是實體裝置，請把 `10.0.2.2` 換成你電腦的區域網路 IP。

</details>

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
- **圖片載入**：Coil
- **序列化**：kotlinx-serialization（JSON）
- **導航**：Navigation Compose
- **日誌**：Timber（透過 `LogPort` 抽象層）

## 架構

### 模組依賴圖

```
┌─────────────────────────────────────────────────────┐
│                         app                         │
│           （Application、導覽、DI 組裝）            │
│                 依賴：以下所有模組                  │
└─────────────────────────────────────────────────────┘
┌──────────────────┐  ┌──────────────────┐
│ feature:weather  │  │ feature:citylist │
│  （畫面 + VM）   │  │  （畫面 + VM）   │
│      → core:domain, core:ui, core      │
└──────────────────┘  └──────────────────┘
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ core:domain  │ │  core:data   │ │   core:ui    │
│（Use Cases） │ │ （Retrofit、 │ │    （共用    │
│              │ │Room、Socket）│ │  Compose）   │
│    → core    │ │    → core    │ │    → core    │
└──────────────┘ └──────────────┘ └──────────────┘
┌─────────────────────────────────────────────────────┐
│                        core                         │
│          （Port 介面 + Domain 模型 + 共用           │
│            config/log 實作）— 無專案相依            │
└─────────────────────────────────────────────────────┘
```

所有依賴方向朝內——外層依賴內層，絕不反向。Feature 模組不會 import `core:data`；它們只依賴 `core` 的介面與 `core:domain` 的 Use Cases。Adapter 綁定實際位於 `core:data` 自己的 Hilt 模組（`DataModule`、`NetworkModule`、`DatabaseModule`……）；`app` 模組只負責注入依應用而異的兩個替換點：`ConfigModule`（功能開關來源）與 `LogModule`（日誌輸出）。

### 資料流（UDF）

```
┌──────────────────────────────────────────────┐
│                     畫面                     │
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

- **事件**沿左側單向往下流：`畫面 → ViewModel → UseCase → Repository`，透過函式呼叫。
- **狀態**沿右側單向往上流：`Room → Repository → UseCase → ViewModel → 畫面`，透過 `Flow`/`StateFlow`。
- **離線優先**：Use Cases 透過 `Flow` 監聽 Room，同時觸發同步。快取資料立即顯示，API 回應後替換為最新資料。

### 專案結構

```
app/                        # 組合根：Hilt 模組、導航、Application
core/                       # Port 介面、Domain 模型、日誌
  core/data/                # 實作層（Retrofit、Room、Socket.IO）
  core/domain/              # Use Cases（商業邏輯：TTL、去重、同步）
  core/ui/                  # 共用 Compose 元件（ErrorContent、LoadingContent）
feature/
  feature/weather/          # 天氣詳情畫面 + ViewModel
  feature/citylist/         # 城市列表 / 選擇畫面 + ViewModel
demo/                       # 獨立 Demo App，用於功能開關與日誌實驗；用 `./gradlew :demo:installDebug` 或 `demo` run configuration 執行
server/                     # Socket.IO 推播伺服器（Node.js + Docker）
```

## CI/CD

| 工作流程 | 觸發條件 | 執行內容 |
|---|---|---|
| **PR Check** (`check.yml`) | Pull request → `main` / `develop` | 執行單元測試、建置 debug APK |
| **Release** (`release.yml`) | 推送 `v*` 標籤 | 建置已簽章的 release APK + AAB（R8 壓縮），上傳至 GitHub Release |

**下載建置產出：**
- **標籤釋出版本** — 到 [Releases](../../releases) 下載 `app-release.apk`（直接安裝）或 `app-release.aab`（上架 Play Store 用）。
- **其他任何建置（包含 PR）** — 到 [Actions](../../actions)，開啟對應的工作流程執行紀錄，在頁面底部的 **Artifacts** 區塊下載。

正式版簽章透過 repository secrets 設定（`KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`）。未設定時，會使用 debug 簽章金鑰，因此標籤建置仍會成功。

## 測試

```bash
./gradlew test
```

共 12 個測試檔案，涵蓋 ViewModel、Use Cases 與 Repository（測試套件如何成長見 [TOOLS.md](TOOLS.md)）。

## AI 輔助開發

本專案使用 [Claude Code](https://docs.anthropic.com/en/docs/claude-code)
以結構化的 Think → Do 工作流程開發：先討論並排序發現的問題，才動手寫程式，
再以最小、聚焦的 commit 逐一實作。過程涵蓋完整的多維度程式碼審查
（正確性、並行處理、架構、測試覆蓋率）、race condition 修復、
Clean Architecture 重構。

完整的工作流程與審查發現摘要請見 [TOOLS.md](TOOLS.md)。
