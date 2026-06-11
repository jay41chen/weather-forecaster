package com.weather.core.data.config

import android.content.Context
import com.weather.core.config.FeatureTogglePort
import com.weather.core.logging.LogPortFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteFeatureToggleAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    logFactory: LogPortFactory
) : FeatureTogglePort {

    private val log = logFactory.create("FeatureToggle")

    private val _configs = MutableStateFlow(runBlocking(Dispatchers.IO) { loadLocalFallback() })
    override val configs: StateFlow<Map<String, Any>> = _configs

    override suspend fun refresh() {
        // TODO: fetch from remote config service (Firebase Remote Config, LaunchDarkly, etc.)
        // On success:  _configs.value = parsedRemoteMap
        // On failure:  keep current value (local fallback already loaded in init)
    }

    private fun loadLocalFallback(): Map<String, Any> {
        return try {
            val jsonText = context.assets.open("feature_defaults.json").bufferedReader().readText()
            val jsonObject = Json.decodeFromString<JsonObject>(jsonText)
            jsonObject.entries.associate { (k, v) ->
                val primitive = v.jsonPrimitive
                k to if (primitive.isString) primitive.content else primitive.boolean
            }
        } catch (e: Exception) {
            log.e("Failed to load feature_defaults.json", e)
            emptyMap()
        }
    }
}
