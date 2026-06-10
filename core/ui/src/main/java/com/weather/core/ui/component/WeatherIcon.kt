package com.weather.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun WeatherIcon(
    iconCode: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    AsyncImage(
        model = "https://openweathermap.org/img/wn/${iconCode}@2x.png",
        contentDescription = null,
        modifier = modifier
    )
}
