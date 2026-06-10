package com.weather.core.data.network

import com.weather.core.logging.LogPortFactory
import com.weather.core.model.City
import com.weather.core.model.CurrentWeather
import com.weather.core.model.DailyForecast
import com.weather.core.model.HourlyForecast
import com.weather.core.network.ForecastResult
import com.weather.core.network.WeatherApiService
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

class RetrofitWeatherApiService @Inject constructor(
    private val client: RetrofitClient,
    private val logFactory: LogPortFactory
) : WeatherApiService {

    private val log = logFactory.create("WeatherApi")

    override suspend fun getCurrentWeather(cityName: String): CurrentWeather {
        val startMs = System.currentTimeMillis()
        val dto = client.getCurrentWeather(cityName)
        log.d("getCurrentWeather", mapOf("city" to cityName, "latency_ms" to (System.currentTimeMillis() - startMs)))
        return CurrentWeather(
            cityName = dto.name,
            country = dto.sys.country,
            temperature = dto.main.temp,
            feelsLike = dto.main.feelsLike,
            description = dto.weather.firstOrNull()?.description ?: "",
            iconCode = dto.weather.firstOrNull()?.icon ?: "",
            humidity = dto.main.humidity,
            windSpeed = dto.wind.speed,
            pressure = dto.main.pressure,
            timestamp = dto.dt
        )
    }

    override suspend fun getForecast(cityName: String): ForecastResult {
        val startMs = System.currentTimeMillis()
        val dto = client.getForecast(cityName)
        log.d("getForecast", mapOf("city" to cityName, "latency_ms" to (System.currentTimeMillis() - startMs)))

        val todayEpoch = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toEpochSecond()
        val tomorrowEpoch = todayEpoch + 86400

        val hourly = dto.list
            .filter { it.dt >= todayEpoch && it.dt < tomorrowEpoch }
            .map { item ->
                HourlyForecast(
                    timestamp = item.dt,
                    temperature = item.main.temp,
                    iconCode = item.weather.firstOrNull()?.icon ?: "",
                    description = item.weather.firstOrNull()?.description ?: ""
                )
            }

        val dailyByDate = dto.list.groupBy { item ->
            Instant.ofEpochSecond(item.dt).atOffset(ZoneOffset.UTC).toLocalDate()
        }

        val daily = dailyByDate.map { (date, items) ->
            val maxTemp = items.maxOf { it.main.tempMax }
            val minTemp = items.minOf { it.main.tempMin }
            val avgHumidity = items.map { it.main.humidity }.average().toInt()
            val avgWindSpeed = items.map { it.wind.speed }.average()

            // Pick entry closest to noon for icon/description
            val noonEpoch = date.atTime(12, 0).toEpochSecond(ZoneOffset.UTC)
            val noonEntry = items.minByOrNull { kotlin.math.abs(it.dt - noonEpoch) } ?: items.first()

            DailyForecast(
                date = date,
                maxTemp = maxTemp,
                minTemp = minTemp,
                description = noonEntry.weather.firstOrNull()?.description ?: "",
                iconCode = noonEntry.weather.firstOrNull()?.icon ?: "",
                humidity = avgHumidity,
                windSpeed = avgWindSpeed
            )
        }.sortedBy { it.date }

        return ForecastResult(hourly = hourly, daily = daily)
    }

    override suspend fun getCurrentWeatherByCoords(lat: Double, lon: Double): CurrentWeather {
        val startMs = System.currentTimeMillis()
        val dto = client.getCurrentWeatherByCoords(lat, lon)
        log.d("getCurrentWeatherByCoords", mapOf("lat" to lat, "lon" to lon, "latency_ms" to (System.currentTimeMillis() - startMs)))
        return CurrentWeather(
            cityName = dto.name,
            country = dto.sys.country,
            temperature = dto.main.temp,
            feelsLike = dto.main.feelsLike,
            description = dto.weather.firstOrNull()?.description ?: "",
            iconCode = dto.weather.firstOrNull()?.icon ?: "",
            humidity = dto.main.humidity,
            windSpeed = dto.wind.speed,
            pressure = dto.main.pressure,
            timestamp = dto.dt
        )
    }

    override suspend fun searchCities(query: String): List<City> {
        val startMs = System.currentTimeMillis()
        val results = client.searchCity(query).map { geo ->
            City(
                name = geo.name,
                country = geo.country,
                state = geo.state,
                latitude = geo.lat,
                longitude = geo.lon
            )
        }
        log.d("searchCities", mapOf("query" to query, "latency_ms" to (System.currentTimeMillis() - startMs)))
        return results
    }
}
