package com.weather.core.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class FeatureToggleMockAdapter(
    initial: Map<String, Any> = emptyMap()
) : FeatureTogglePort {

    private val _configs = MutableStateFlow(initial)
    override val configs: StateFlow<Map<String, Any>> = _configs

    override suspend fun refresh() { /* no-op */ }

    fun setFlag(key: FeatureFlag, enabled: Boolean) {
        _configs.update { it + (key.key to enabled) }
    }
}
