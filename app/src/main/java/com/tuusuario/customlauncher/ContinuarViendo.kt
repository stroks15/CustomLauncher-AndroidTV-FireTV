package com.tuusuario.customlauncher

import android.content.Context
import android.graphics.Color
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * "Continuar viendo": registro local, automático, de las películas y series que la persona abre.
 *
 * CÓMO FUNCIONA (y su límite):
 *  • Cada vez que se abre una película/serie/documental/novela con stream_url configurada,
 *    MainActivity.abrirContenido() la registra aquí. Los canales en vivo y la música NO se registran.
 *  • HomeFragment muestra la lista en la fila "CONTINUAR VIENDO", debajo de los canales en vivo.
 *  • El launcher no puede saber si un stream remoto terminó. La tarjeta se queda hasta que la
 *    persona la quita (botón central sostenido → "Ya lo terminé") o sale de los últimos [MAX_ELEMENTOS].
 *    Al tocarla se reproduce la misma URL configurada por el catálogo.
 */
object ContinuarViendo {

    private const val TAG = "ContinuarViendo"
    private const val ARCHIVO = "continuar_viendo.json"
    private const val MAX_ELEMENTOS = 12
    private const val COLOR_POR_DEFECTO = "#37474F"

    /** Prefijo del id de las tarjetas de esta fila (así MainActivity sabe que vienen de aquí). */
    const val PREFIJO_ID = "continuar:"
    const val SUBTITULO = "Continuar viendo"


    /** Plugins de Kino TV que reproducen películas / series / documentales (no en vivo, no música). */
    val PLUGINS_DE_VIDEO = setOf(
        "archive", "novelas", "retro-tv", "comedia", "docs",
        "cine-oro-mexicano", "retroseries-latam", "doramaslatino-api",
        "vod-latino-releases", "navidad-total"
    )

    private val NOMBRES_DE_MENU = mapOf(
        "novelas" to "Telenovelas",
        "retro-tv" to "Series de antes",
        "comedia" to "Comedia",
        "docs" to "Documentales"
    )

    private val candado = Any()

    /** true si lo que se está abriendo debe aparecer en "Continuar viendo". */
    fun aplica(plugin: String, modo: String): Boolean =
        plugin in PLUGINS_DE_VIDEO && modo != "live"

    /** Título presentable para lo que llega por voz (p. ej. "novelas" + "telenovelas_list" → "Telenovelas"). */
    fun tituloLegible(plugin: String, target: String): String {
        if (target.endsWith("_list")) return NOMBRES_DE_MENU[plugin] ?: target
        val limpio = target.trim()
        if (limpio.isEmpty()) return NOMBRES_DE_MENU[plugin] ?: plugin
        return limpio.replaceFirstChar { it.uppercase() }
    }

    private fun clave(plugin: String, target: String, extras: Map<String, String>): String =
        "$plugin|$target|" + extras.toSortedMap().entries.joinToString(",") { "${it.key}=${it.value}" }

    /** Guarda (o sube al primer lugar) una película/serie recién abierta. */
    fun registrar(
        contexto: Context,
        titulo: String,
        plugin: String,
        target: String,
        modo: String,
        extras: Map<String, String>,
        acento: Int?,
        logo: String?
    ) {
        synchronized(candado) {
            try {
                val clave = clave(plugin, target, extras)
                val extrasJson = JSONObject()
                extras.forEach { (k, v) -> extrasJson.put(k, v) }
                val nuevo = JSONObject().apply {
                    put("clave", clave)
                    put("title", titulo)
                    put("plugin_source", plugin)
                    put("target_content", target)
                    put("playback_mode", modo)
                    put("accent_color", acento ?: Color.parseColor(COLOR_POR_DEFECTO))
                    put("logo", logo.orEmpty())
                    put("extras", extrasJson)
                    put("fecha", System.currentTimeMillis())
                }
                val lista = mutableListOf(nuevo)
                leerCrudo(contexto).filter { it.optString("clave") != clave }.forEach { lista.add(it) }
                guardar(contexto, lista.take(MAX_ELEMENTOS))
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo registrar en Continuar viendo: ${e.message}")
            }
        }
    }

    /** Lista para pintar la fila (la más reciente primero). Vacía si no hay nada. */
    fun leer(contexto: Context): List<ContentItem> = synchronized(candado) {
        leerCrudo(contexto).map { o ->
            val extrasJson = o.optJSONObject("extras")
            val extras = mutableMapOf<String, String>()
            extrasJson?.keys()?.forEach { k -> extras[k] = extrasJson.optString(k) }
            ContentItem(
                id = PREFIJO_ID + o.optString("clave"),
                title = o.optString("title"),
                subtitle = SUBTITULO,
                pluginSource = o.optString("plugin_source"),
                targetContent = o.optString("target_content"),
                playbackMode = o.optString("playback_mode", "search").ifBlank { "search" },
                logo = o.optString("logo", "").ifBlank { null },
                accentColor = o.optInt("accent_color", Color.parseColor(COLOR_POR_DEFECTO)),
                voiceAliases = emptyList(),
                extras = extras,
                esMuestra = false
            )
        }
    }

    /** Quita una entrada (la persona ya la terminó). [id] es el id de la tarjeta. */
    fun quitar(contexto: Context, id: String) {
        synchronized(candado) {
            try {
                val clave = id.removePrefix(PREFIJO_ID)
                guardar(contexto, leerCrudo(contexto).filter { it.optString("clave") != clave })
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo quitar de Continuar viendo: ${e.message}")
            }
        }
    }

    private fun leerCrudo(contexto: Context): List<JSONObject> {
        return try {
            val archivo = File(contexto.filesDir, ARCHIVO)
            if (!archivo.exists()) return emptyList()
            val arreglo = JSONArray(archivo.readText(Charsets.UTF_8))
            (0 until arreglo.length()).map { arreglo.getJSONObject(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Archivo de Continuar viendo ilegible: ${e.message}")
            emptyList()
        }
    }

    private fun guardar(contexto: Context, lista: List<JSONObject>) {
        val arreglo = JSONArray()
        lista.forEach { arreglo.put(it) }
        File(contexto.filesDir, ARCHIVO).writeText(arreglo.toString(), Charsets.UTF_8)
    }
}
