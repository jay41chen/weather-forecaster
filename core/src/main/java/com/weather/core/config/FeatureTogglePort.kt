package com.weather.core.config

import kotlinx.coroutines.flow.StateFlow

interface FeatureTogglePort {
    val configs: StateFlow<Map<String, Any>>
    suspend fun refresh()
}
