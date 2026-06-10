package com.weather.core.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FeatureToggleMockAdapter(
    initialFlags: Map<String, Boolean> = emptyMap()
) : FeatureTogglePort {

    private val _flags = MutableStateFlow(initialFlags)
    override val flags: StateFlow<Map<String, Boolean>> = _flags

    override suspend fun refresh() { /* no-op */ }

    fun setFlag(key: FeatureFlag, enabled: Boolean) {
        _flags.value = _flags.value + (key.key to enabled)
    }
}
