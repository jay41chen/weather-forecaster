# Weather Forecaster

Android weather app built with Clean Architecture, Jetpack Compose, and the OpenWeatherMap API.

## Setup

1. Clone the repo
2. Add the following to `local.properties` (create it in the project root if it doesn't exist):

```
OPEN_WEATHER_API_KEY=<your_api_key_here>
```

3. Sync Gradle and run the app

## Tech Stack

- Kotlin + Jetpack Compose
- Clean Architecture (app / feature / core:domain / core:data / core)
- Hilt (DI)
- Retrofit + OkHttp
- Room (local cache)
- DataStore (selected city preference)
- Fused Location Provider (auto-detect current city)
