package com.tuusuario.customlauncher

import android.content.Context
import android.graphics.Color
import android.util.Log
import org.json.JSONObject

/**
 * Lee assets/recomendaciones_launcher.json y lo convierte en filas listas para Leanback.
 * Para agregar/quitar canales, cambiar títulos o añadir una fila nueva basta con editar el JSON.
 */
object LauncherContent {

    private const val TAG = "LauncherContent"
    private const val ARCHIVO = "recomendaciones_launcher.json"
    private const val COLOR_POR_DEFECTO = "#37474F"

    fun cargarFilas(contexto: Context): List<ContentRow> {
        return try {
            val texto = contexto.assets.open(ARCHIVO).bufferedReader(Charsets.UTF_8).use { it.readText() }
            val filasJson = JSONObject(texto).optJSONArray("rows") ?: return emptyList()
            (0 until filasJson.length()).map { i -> parsearFila(filasJson.getJSONObject(i)) }
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo leer $ARCHIVO: ${e.message}")
            emptyList()
        }
    }

    private fun parsearFila(o: JSONObject): ContentRow {
        val itemsJson = o.optJSONArray("items")
        val items = if (itemsJson == null) {
            emptyList()
        } else {
            (0 until itemsJson.length()).map { i -> parsearItem(itemsJson.getJSONObject(i)) }
        }
        return ContentRow(
            id = o.optString("id"),
            title = o.optString("title"),
            dinamicaHistorial = o.optBoolean("dynamic_history", false),
            items = items
        )
    }

    private fun parsearItem(o: JSONObject): ContentItem {
        val alias = o.optJSONArray("voice_aliases")
        val listaAlias = if (alias == null) emptyList() else (0 until alias.length()).map { alias.getString(it) }

        val extrasJson = o.optJSONObject("extras")
        val extras = mutableMapOf<String, String>()
        extrasJson?.keys()?.forEach { clave -> extras[clave] = extrasJson.optString(clave) }

        val logo = o.optString("logo", "").ifBlank { null }

        return ContentItem(
            id = o.optString("id"),
            title = o.optString("title"),
            subtitle = o.optString("subtitle"),
            pluginSource = o.optString("plugin_source"),
            targetContent = o.optString("target_content"),
            playbackMode = o.optString("playback_mode", "live"),
            logo = logo,
            accentColor = colorSeguro(o.optString("accent_color", COLOR_POR_DEFECTO)),
            voiceAliases = listaAlias,
            extras = extras,
            esMuestra = o.optBoolean("sample", false)
        )
    }

    private fun colorSeguro(texto: String): Int = try {
        Color.parseColor(texto)
    } catch (e: IllegalArgumentException) {
        Color.parseColor(COLOR_POR_DEFECTO)
    }
}
