# Weather Push Server

## Setup

### Option A: Docker (recommended)
1. `cp .env.example .env`
2. Add your OpenWeatherMap API key to `.env`
3. `docker compose up`

### Option B: Node.js
1. `cp .env.example .env`
2. Add your OpenWeatherMap API key to `.env`
3. `npm install`
4. `npm start`

## Android Client Connection
- **Emulator**: use `http://10.0.2.2:3000` (default in `feature_defaults.json`)
- **Physical device (same WiFi)**: replace with your machine's LAN IP, e.g. `http://192.168.x.x:3000`
- **Physical device (USB)**: run `adb reverse tcp:3000 tcp:3000`, then use `http://localhost:3000`

## Events
- Client → Server: `subscribe` with `{ cities: ["London"] }`
- Server → Client: `weather_update` with current weather data
- Server → Client: `weather_alert` for dramatic changes (>5°C shift or weather category change)
