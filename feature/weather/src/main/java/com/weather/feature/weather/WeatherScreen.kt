package com.weather.feature.weather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.weather.core.ui.component.ErrorContent
import com.weather.core.ui.component.LoadingContent
import com.weather.core.ui.component.isRetryable
import com.weather.core.ui.component.snackbarDuration
import com.weather.core.ui.component.userMessage
import com.weather.feature.weather.component.CurrentWeatherCard
import com.weather.feature.weather.component.DailyForecastItem
import com.weather.feature.weather.component.HourlyForecastRow
import com.weather.feature.weather.component.WeatherDetailsRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    onNavigateToCityList: () -> Unit,
    viewModel: WeatherViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val error = uiState.error
    val hasCache = uiState.currentWeather != null
    LaunchedEffect(error, hasCache) {
        if (error != null && hasCache) {
            val result = snackbarHostState.showSnackbar(
                message = error.userMessage(),
                actionLabel = if (error.isRetryable()) "Retry" else null,
                duration = error.snackbarDuration()
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.refresh()
            }
            viewModel.dismissError()
        }
    }

    LaunchedEffect(uiState.alertMessage) {
        uiState.alertMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissAlert()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.cityName.ifBlank { "Weather" },
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToCityList) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Select City"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isSyncing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.currentWeather == null -> LoadingContent()
                uiState.error != null && uiState.currentWeather == null -> ErrorContent(
                    error = uiState.error!!,
                    onRetry = viewModel::refresh
                )
                else -> {
                    val weather = uiState.currentWeather
                    if (weather != null) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item { CurrentWeatherCard(weather = weather, modifier = Modifier.padding(top = 8.dp)) }
                            item { WeatherDetailsRow(weather = weather) }
                            if (viewModel.showHourlyForecast) {
                                item {
                                    Text(
                                        text = "Today",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                item { HourlyForecastRow(forecasts = uiState.hourlyForecasts) }
                            }
                            item {
                                Text(
                                    text = "5-Day Forecast",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            items(uiState.dailyForecasts) { forecast ->
                                DailyForecastItem(
                                    forecast = forecast,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            item { androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = 16.dp)) }
                        }
                    }
                }
            }
        }
    }
}
