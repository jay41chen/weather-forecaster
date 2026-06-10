package com.weather.core.config

import kotlinx.coroutines.flow.StateFlow

interface FeatureTogglePort {
    val flags: StateFlow<Map<String, Boolean>>
    suspend fun refresh()
}
