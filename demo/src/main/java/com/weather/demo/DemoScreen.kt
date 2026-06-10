package com.weather.demo

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.weather.core.config.FeatureFlag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoScreen(
    locationPermissionGranted: Boolean,
    viewModel: DemoViewModel = hiltViewModel()
) {
    val locationResult by viewModel.locationResult.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val weatherResult by viewModel.weatherResult.collectAsState()
    val featureFlags by viewModel.featureFlags.collectAsState()

    var hasLocationPermission by remember { mutableStateOf(locationPermissionGranted) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    var searchQuery by remember { mutableStateOf("Taipei") }
    var weatherCity by remember { mutableStateOf("Taipei") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Module Test") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- Feature Flags ---
            TestCard(title = "Feature Flags  (FeatureToggleMockAdapter)") {
                FeatureFlag.entries.forEach { flag ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = flag.key,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = featureFlags[flag.key] ?: false,
                            onCheckedChange = { viewModel.toggleFlag(flag, it) }
                        )
                    }
                }
            }

            // --- Live Preview ---
            TestCard(title = "Live Preview  (reacts to flag changes above)") {
                // CITY_SEARCH_ENABLED
                if (featureFlags[FeatureFlag.CITY_SEARCH_ENABLED.key] == true) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        label = { Text("Search cities...") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        singleLine = true
                    )
                } else {
                    Text(
                        "city_search_enabled=false → search bar hidden",
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // OFFLINE_BANNER_ENABLED
                if (featureFlags[FeatureFlag.OFFLINE_BANNER_ENABLED.key] == true) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            "⚠ You are offline",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                } else {
                    Text(
                        "offline_banner_enabled=false → banner hidden",
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // HOURLY_FORECAST_ENABLED
                if (featureFlags[FeatureFlag.HOURLY_FORECAST_ENABLED.key] == true) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("09:00\n22°", "12:00\n25°", "15:00\n24°", "18:00\n21°")) { item ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    item,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        "hourly_forecast_enabled=false → row hidden",
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // --- Location ---
            TestCard(title = "Location  (LocationRepository)") {
                Button(
                    onClick = {
                        if (!hasLocationPermission) {
                            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        } else {
                            viewModel.testLocation()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (hasLocationPermission) "Detect City" else "Request Permission")
                }
                ResultText(locationResult)
            }

            // --- City Search ---
            TestCard(title = "City Search  (CityRepository → /geo/1.0/direct)") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Query") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(onClick = { viewModel.searchCities(searchQuery) }) {
                        Text("Search")
                    }
                }
                ResultText(searchResult)
            }

            // --- Weather Sync ---
            TestCard(title = "Weather Sync  (WeatherRepository → /data/2.5/*)") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = weatherCity,
                        onValueChange = { weatherCity = it },
                        label = { Text("City") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(onClick = { viewModel.syncWeather(weatherCity) }) {
                        Text("Sync")
                    }
                }
                ResultText(weatherResult)
            }
        }
    }
}

@Composable
private fun TestCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title)
            content()
        }
    }
}

@Composable
private fun ResultText(text: String) {
    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    )
}
