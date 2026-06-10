package com.weather.core.config

enum class FeatureFlag(val key: String) {
    SOCKET_IO_ENABLED("socket_io_enabled"),
    CITY_SEARCH_ENABLED("city_search_enabled"),
    HOURLY_FORECAST_ENABLED("hourly_forecast_enabled"),
    OFFLINE_BANNER_ENABLED("offline_banner_enabled"),
    WEATHER_ALERTS_ENABLED("weather_alerts_enabled")
}
