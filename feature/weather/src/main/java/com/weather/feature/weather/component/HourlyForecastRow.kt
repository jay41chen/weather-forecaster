package com.weather.feature.weather.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weather.core.model.HourlyForecast
import com.weather.core.ui.component.WeatherIcon
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HourlyForecastRow(forecasts: List<HourlyForecast>, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(forecasts) { item ->
            HourlyItem(item)
        }
    }
}

@Composable
private fun HourlyItem(forecast: HourlyForecast) {
    val time = Instant.ofEpochSecond(forecast.timestamp)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
    Card(modifier = Modifier.width(64.dp)) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = time, style = MaterialTheme.typography.labelSmall)
            WeatherIcon(iconCode = forecast.iconCode, modifier = Modifier.size(32.dp))
            Text(text = "${forecast.temperature.toInt()}°", style = MaterialTheme.typography.bodySmall)
        }
    }
}
