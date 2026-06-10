package com.weather.core.data.mapper

import com.weather.core.data.local.entity.CurrentWeatherEntity
import com.weather.core.data.local.entity.DailyForecastEntity
import com.weather.core.data.local.entity.HourlyForecastEntity
import com.weather.core.model.CurrentWeather
import com.weather.core.model.DailyForecast
import com.weather.core.model.HourlyForecast
import java.time.LocalDate

fun CurrentWeatherEntity.toDomain(): CurrentWeather = CurrentWeather(
    cityName = cityName,
    country = country,
    temperature = temperature,
    feelsLike = feelsLike,
    description = description,
    iconCode = iconCode,
    humidity = humidity,
    windSpeed = windSpeed,
    pressure = pressure,
    timestamp = timestamp
)

fun CurrentWeather.toEntity(): CurrentWeatherEntity = CurrentWeatherEntity(
    cityName = cityName,
    country = country,
    temperature = temperature,
    feelsLike = feelsLike,
    description = description,
    iconCode = iconCode,
    humidity = humidity,
    windSpeed = windSpeed,
    pressure = pressure,
    timestamp = timestamp,
    lastUpdated = System.currentTimeMillis()
)

fun DailyForecastEntity.toDomain(): DailyForecast = DailyForecast(
    date = LocalDate.parse(date),
    maxTemp = maxTemp,
    minTemp = minTemp,
    description = description,
    iconCode = iconCode,
    humidity = humidity,
    windSpeed = windSpeed
)

fun DailyForecast.toEntity(cityName: String): DailyForecastEntity = DailyForecastEntity(
    cityName = cityName,
    date = date.toString(),
    maxTemp = maxTemp,
    minTemp = minTemp,
    description = description,
    iconCode = iconCode,
    humidity = humidity,
    windSpeed = windSpeed
)

fun HourlyForecastEntity.toDomain(): HourlyForecast = HourlyForecast(
    timestamp = timestamp,
    temperature = temperature,
    iconCode = iconCode,
    description = description
)

fun HourlyForecast.toEntity(cityName: String): HourlyForecastEntity = HourlyForecastEntity(
    cityName = cityName,
    timestamp = timestamp,
    temperature = temperature,
    iconCode = iconCode,
    description = description
)
