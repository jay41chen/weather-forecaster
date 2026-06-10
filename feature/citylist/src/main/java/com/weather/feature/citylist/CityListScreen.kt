package com.weather.feature.citylist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.weather.core.model.City

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityListScreen(
    onNavigateBack: () -> Unit,
    viewModel: CityListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select City") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                placeholder = { Text("Search cities...") },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )

            if (uiState.searchQuery.isNotEmpty()) {
                LazyColumn {
                    items(uiState.searchResults) { city ->
                        SearchResultItem(
                            city = city,
                            onAdd = { viewModel.onCityAdd(city) }
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(uiState.savedCities) { city ->
                        SavedCityItem(
                            city = city,
                            isSelected = city.name == uiState.selectedCityName,
                            onSelect = { viewModel.onCitySelect(city, onNavigateBack) },
                            onRemove = { viewModel.onCityRemove(city) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(city: City, onAdd: () -> Unit) {
    ListItem(
        headlineContent = { Text(city.name) },
        supportingContent = { Text("${city.country}${city.state?.let { ", $it" } ?: ""}") },
        trailingContent = {
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    )
}

@Composable
private fun SavedCityItem(
    city: City,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    ListItem(
        headlineContent = { Text(city.name) },
        supportingContent = { Text(city.country) },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove")
            }
        },
        colors = if (isSelected) ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else ListItemDefaults.colors(),
        modifier = Modifier.clickable(onClick = onSelect)
    )
}
