# Weather Push Server

A Node.js + Express + Socket.IO demo server that polls OpenWeatherMap for each subscribed city (default every 60 s), pushes `weather_update` when conditions change and `weather_alert` on dramatic changes (>5°C shift or weather-category change). Listens on port 3000 by default.

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

## Configuration

Set these in `.env` (see `.env.example`):

| Key | Required | Default | Description |
|---|---|---|---|
| `OPEN_WEATHER_API_KEY` | Yes | — | OpenWeatherMap API key. The server refuses to start without it. |
| `PORT` | No | `3000` | HTTP + Socket.IO listen port. |
| `POLL_INTERVAL_MS` | No | `60000` | How often each subscribed city is re-polled, in milliseconds. |

## Android Client Connection
- **Emulator**: use `http://10.0.2.2:3000` (default in `feature_defaults.json`)
- **Physical device (same WiFi)**: replace with your machine's LAN IP, e.g. `http://192.168.x.x:3000`
- **Physical device (USB)**: run `adb reverse tcp:3000 tcp:3000`, then use `http://localhost:3000`

## Admin & Testing

- `GET /admin/status` — returns the current connection count and per-city subscriber counts.
- `POST /admin/test-update` — broadcasts a fake `weather_update`; any field in the JSON body (`cityName`, `temperature`, `feelsLike`, `description`, `iconCode`, `humidity`, `windSpeed`, `pressure`) overrides its default.
- `POST /admin/test-alert` — broadcasts a fake `weather_alert`; the JSON body (`cityName`, `type`, `message`) overrides its default.

A web control panel for these endpoints is served from `server/public/` at `http://localhost:3000/`.

## Events
- Client → Server: `subscribe` with `{ cities: ["London"] }`
- Server → Client: `weather_update` with fields `cityName`, `temperature`, `feelsLike`, `description`, `iconCode`, `humidity`, `windSpeed`, `pressure`, `timestamp`
- Server → Client: `weather_alert` for dramatic changes (>5°C shift or weather category change) with fields `cityName`, `type`, `message`, `timestamp`
