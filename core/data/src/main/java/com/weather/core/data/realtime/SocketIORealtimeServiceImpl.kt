package com.weather.core.data.realtime

import com.weather.core.config.FeatureTogglePort
import com.weather.core.config.getString
import com.weather.core.data.local.dao.WeatherDao
import com.weather.core.data.mapper.toEntity
import com.weather.core.logging.LogPortFactory
import com.weather.core.model.CurrentWeather
import com.weather.core.model.WeatherAlert
import com.weather.core.repository.WeatherRealtimeService
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketIORealtimeServiceImpl @Inject constructor(
    private val weatherDao: WeatherDao,
    private val featureToggle: FeatureTogglePort,
    logFactory: LogPortFactory
) : WeatherRealtimeService {

    private val log = logFactory.create("SocketIO")
    private val socketMutex = Mutex()
    private var socket: Socket? = null
    private val _updates = MutableSharedFlow<CurrentWeather>(extraBufferCapacity = 64)
    private val _alerts = MutableSharedFlow<WeatherAlert>(extraBufferCapacity = 64)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeWeatherUpdates(): Flow<CurrentWeather> = _updates.asSharedFlow()
    override fun observeWeatherAlerts(): Flow<WeatherAlert> = _alerts.asSharedFlow()

    override suspend fun connect() {
        socketMutex.withLock {
            if (socket?.connected() == true) return
            val url = featureToggle.getString("socket_url", "http://10.0.2.2:3000")
            log.i("Connecting", mapOf("url" to url))

            val opts = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 2000
            }
            socket = IO.socket(url, opts)

            socket?.on(Socket.EVENT_CONNECT) {
                log.i("Connected")
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                val error = args.firstOrNull()
                log.w("Connection error", extras = mapOf("error" to error.toString()))
            }

            socket?.on("weather_update") { args ->
                val json = args[0] as JSONObject
                scope.launch {
                    try {
                        val weather = CurrentWeather(
                            cityName = json.getString("cityName"),
                            country = json.optString("country", ""),
                            temperature = json.getDouble("temperature"),
                            feelsLike = json.optDouble("feelsLike", 0.0),
                            description = json.getString("description"),
                            iconCode = json.getString("iconCode"),
                            humidity = json.getInt("humidity"),
                            windSpeed = json.getDouble("windSpeed"),
                            pressure = json.optInt("pressure", 0),
                            timestamp = json.getLong("timestamp")
                        )
                        weatherDao.insertCurrentWeather(weather.toEntity())
                        _updates.emit(weather)
                        log.d("Update saved", mapOf("city" to weather.cityName, "temp" to weather.temperature))
                    } catch (e: Exception) {
                        log.e("Parse error", e)
                    }
                }
            }

            socket?.on("weather_alert") { args ->
                val json = args[0] as JSONObject
                scope.launch {
                    try {
                        val alert = WeatherAlert(
                            cityName = json.getString("cityName"),
                            type = json.getString("type"),
                            message = json.getString("message"),
                            timestamp = json.getLong("timestamp")
                        )
                        _alerts.emit(alert)
                        log.i("Alert received", mapOf("city" to alert.cityName, "type" to alert.type))
                    } catch (e: Exception) {
                        log.e("Alert parse error", e)
                    }
                }
            }

            socket?.connect()
        }
    }

    override suspend fun disconnect() {
        socketMutex.withLock {
            log.i("Disconnecting")
            socket?.disconnect()
            socket?.off()
            socket = null
        }
    }

    override suspend fun subscribeCities(cityNames: List<String>) {
        socketMutex.withLock {
            val payload = JSONObject().put("cities", JSONArray(cityNames))
            socket?.emit("subscribe", payload)
            log.d("Subscribed", mapOf("cities" to cityNames.joinToString(",")))
        }
    }

    override suspend fun unsubscribeCities(cityNames: List<String>) {
        socketMutex.withLock {
            val payload = JSONObject().put("cities", JSONArray(cityNames))
            socket?.emit("unsubscribe", payload)
            log.d("Unsubscribed", mapOf("cities" to cityNames.joinToString(",")))
        }
    }
}
