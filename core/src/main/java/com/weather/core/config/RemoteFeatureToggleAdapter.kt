package com.weather.core.config

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteFeatureToggleAdapter @Inject constructor(
    @ApplicationContext private val context: Context
) : FeatureTogglePort {

    private val _flags = MutableStateFlow(loadLocalFallback())
    override val flags: StateFlow<Map<String, Boolean>> = _flags

    override suspend fun refresh() {
        // TODO: fetch from remote config service (Firebase Remote Config, LaunchDarkly, etc.)
        // On success:  _flags.value = parsedRemoteMap
        // On failure:  keep current value (local fallback already loaded in init)
    }

    private fun loadLocalFallback(): Map<String, Boolean> {
        val jsonText = context.assets.open("feature_defaults.json").bufferedReader().readText()
        val jsonObject = Json.decodeFromString<JsonObject>(jsonText)
        return jsonObject.entries.associate { (k, v) -> k to v.jsonPrimitive.boolean }
    }
}
