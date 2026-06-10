package com.weather.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.weather.core.data.local.entity.CurrentWeatherEntity
import com.weather.core.data.local.entity.DailyForecastEntity
import com.weather.core.data.local.entity.HourlyForecastEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {

    @Query("SELECT * FROM current_weather WHERE cityName = :cityName")
    fun observeCurrentWeather(cityName: String): Flow<CurrentWeatherEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrentWeather(entity: CurrentWeatherEntity)

    @Query("SELECT lastUpdated FROM current_weather WHERE cityName = :cityName")
    suspend fun getLastUpdated(cityName: String): Long?

    @Query("SELECT * FROM daily_forecasts WHERE cityName = :cityName ORDER BY date ASC")
    fun observeDailyForecasts(cityName: String): Flow<List<DailyForecastEntity>>

    @Transaction
    suspend fun replaceDailyForecasts(cityName: String, forecasts: List<DailyForecastEntity>) {
        deleteDailyForecasts(cityName)
        insertDailyForecasts(forecasts)
    }

    @Query("DELETE FROM daily_forecasts WHERE cityName = :cityName")
    suspend fun deleteDailyForecasts(cityName: String)

    @Insert
    suspend fun insertDailyForecasts(forecasts: List<DailyForecastEntity>)

    @Query("SELECT * FROM hourly_forecasts WHERE cityName = :cityName ORDER BY timestamp ASC")
    fun observeHourlyForecasts(cityName: String): Flow<List<HourlyForecastEntity>>

    @Transaction
    suspend fun replaceHourlyForecasts(cityName: String, forecasts: List<HourlyForecastEntity>) {
        deleteHourlyForecasts(cityName)
        insertHourlyForecasts(forecasts)
    }

    @Query("DELETE FROM hourly_forecasts WHERE cityName = :cityName")
    suspend fun deleteHourlyForecasts(cityName: String)

    @Insert
    suspend fun insertHourlyForecasts(forecasts: List<HourlyForecastEntity>)
}
