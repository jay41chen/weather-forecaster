package com.weather.core.config

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

fun FeatureTogglePort.isEnabled(key: FeatureFlag, defaultValue: Boolean = false): Boolean =
    configs.value[key.key] as? Boolean ?: defaultValue

fun FeatureTogglePort.observeFlag(key: FeatureFlag): Flow<Boolean> =
    configs.map { it[key.key] as? Boolean ?: false }.distinctUntilChanged()

fun FeatureTogglePort.getString(key: String, default: String = ""): String =
    configs.value[key] as? String ?: default
