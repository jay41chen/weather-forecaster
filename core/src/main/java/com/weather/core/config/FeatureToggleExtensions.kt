package com.weather.core.config

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

fun FeatureTogglePort.isEnabled(key: FeatureFlag, defaultValue: Boolean = false): Boolean =
    flags.value[key.key] ?: defaultValue

fun FeatureTogglePort.observeFlag(key: FeatureFlag): Flow<Boolean> =
    flags.map { it[key.key] ?: false }.distinctUntilChanged()
