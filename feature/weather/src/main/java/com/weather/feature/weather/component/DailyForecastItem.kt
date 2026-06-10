package com.weather.feature.weather.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weather.core.model.DailyForecast
import com.weather.core.ui.component.WeatherIcon
import java.time.format.DateTimeFormatter

@Composable
fun DailyForecastItem(forecast: DailyForecast, modifier: Modifier = Modifier) {
    val dayFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = forecast.date.format(dayFormatter),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            WeatherIcon(iconCode = forecast.iconCode, modifier = Modifier.size(32.dp))
            Text(
                text = "${forecast.maxTemp.toInt()}° / ${forecast.minTemp.toInt()}°",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
