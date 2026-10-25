package com.tuusuario.customlauncher

import android.content.Context
import android.util.Log
import org.json.JSONObject

data class PluginDefinition(
    val id: String,
    val name: String,
    val kind: String,
    val enabled: Boolean
)

/** Registro declarativo de plugins; no scrapea sitios ni inventa URLs de reproducción. */
object PluginRegistry {
    private const val FILE = "plugin_sources.json"
    private const val TAG = "PluginRegistry"

    fun definitions(context: Context): List<PluginDefinition> = runCatching {
        val root = JSONObject(context.assets.open(FILE).bufferedReader(Charsets.UTF_8).use { it.readText() })
        val array = root.optJSONArray("plugins") ?: org.json.JSONArray()
        (0 until array.length()).mapNotNull { i ->
            val item = array.optJSONObject(i) ?: return@mapNotNull null
            PluginDefinition(
                id = item.optString("id"),
                name = item.optString("name"),
                kind = item.optString("kind"),
                enabled = item.optBoolean("enabled", false)
            )
        }
    }.onFailure { Log.w(TAG, "No se pudo leer $FILE: ${it.message}") }.getOrDefault(emptyList())

    fun epgFeedUrl(context: Context): String = runCatching {
        val root = JSONObject(context.assets.open(FILE).bufferedReader(Charsets.UTF_8).use { it.readText() })
        root.optString("epg_feed_url").trim()
    }.getOrDefault("")
}
