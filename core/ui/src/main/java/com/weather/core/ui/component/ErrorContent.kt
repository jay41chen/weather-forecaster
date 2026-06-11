package com.weather.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weather.core.model.ApiError

@Composable
fun ErrorContent(
    error: ApiError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = error.userMessage(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (error.isRetryable()) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

fun ApiError.userMessage(): String = when (this) {
    is ApiError.NetworkUnavailable -> "No internet connection. Check your network and try again."
    is ApiError.InvalidApiKey -> "Configuration error. Please reinstall the app."
    is ApiError.CityNotFound -> "City not found. Try a different search."
    is ApiError.RateLimited -> "Too many requests. Please wait and try again."
    is ApiError.ServerError -> "Weather service is temporarily unavailable. Try again later."
    is ApiError.ParseError -> "Something went wrong. Please update the app."
    is ApiError.Unknown -> "Something went wrong. Try again."
}

fun ApiError.isRetryable(): Boolean = when (this) {
    is ApiError.NetworkUnavailable, is ApiError.RateLimited,
    is ApiError.ServerError, is ApiError.Unknown -> true
    else -> false
}

fun ApiError.snackbarDuration(): SnackbarDuration = when (this) {
    is ApiError.NetworkUnavailable -> SnackbarDuration.Indefinite
    is ApiError.CityNotFound -> SnackbarDuration.Short
    else -> SnackbarDuration.Long
}
