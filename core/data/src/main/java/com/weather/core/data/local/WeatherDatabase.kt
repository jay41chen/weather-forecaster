package com.weather.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.weather.core.data.local.dao.CityDao
import com.weather.core.data.local.dao.WeatherDao
import com.weather.core.data.local.entity.CityEntity
import com.weather.core.data.local.entity.CurrentWeatherEntity
import com.weather.core.data.local.entity.DailyForecastEntity
import com.weather.core.data.local.entity.HourlyForecastEntity

@Database(
    entities = [
        CurrentWeatherEntity::class,
        DailyForecastEntity::class,
        HourlyForecastEntity::class,
        CityEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
    abstract fun cityDao(): CityDao
}
