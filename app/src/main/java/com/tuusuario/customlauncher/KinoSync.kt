package com.tuusuario.customlauncher

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.Log
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONArray
import java.io.File

/**
 * ============================================================================
 *  CONTRATO ASUMIDO CON KINO TV  —  VERIFICAR ANTES DE USAR
 * ============================================================================
 * No existe documentación pública de cómo com.arkiv.player.light expone su historial.
 * Este archivo asume un ContentProvider de sólo lectura con estas características.
 * Si el provider real es distinto, ajusta ÚNICAMENTE las constantes de este objeto.
 * Si no existe, el launcher sigue funcionando: la Fila 3 muestra los datos de muestra
 * del JSON y el resto de la app no se ve afectada.
 */
object KinoContract {
    const val AUTORIDAD = "com.arkiv.player.light.provider"
    val URI_HISTORIAL: Uri = Uri.parse("content://$AUTORIDAD/history")

    // Columnas esperadas
    const val COL_TITULO = "title"
    const val COL_PLUGIN = "plugin_source"
    const val COL_TARGET = "target_content"
    const val COL_MODO = "playback_mode"
    const val COL_MINIATURA = "thumbnail_url"
    const val COL_FECHA = "watched_at"
}

/** Sincronización en segundo plano (WorkManager, cada 15 min) del historial de Kino TV. */
object KinoSync {

    private const val TAG = "KinoSync"
    private const val ARCHIVO_CACHE = "kino_historial.json"
    private const val MAX_ELEMENTOS = 6
    private const val NOMBRE_TRABAJO = "kino_sync_periodico"

    /** Broadcast interno (sólo dentro de esta app) que avisa a la UI que el caché cambió. */
    const val ACCION_CACHE_ACTUALIZADA = "com.tuusuario.customlauncher.HISTORIAL_ACTUALIZADO"

    /**
     * No programa consultas hasta confirmar un ContentProvider real de Kino.
     * La autoridad antigua no existe en el dispositivo probado y generaba errores cada 15 min.
     */
    fun programar(contexto: Context) {
        WorkManager.getInstance(contexto).cancelUniqueWork(NOMBRE_TRABAJO)
        Log.i(TAG, "Sincronización automática desactivada: el proveedor de Kino no está verificado")
    }

    /**
     * Contrato aún no verificado. No consultar una URI inventada ni declarar éxito ficticio.
     * Se conserva el caché existente, si lo hubiera, hasta implementar una fuente real.
     */
    fun sincronizar(contexto: Context): Boolean {
        Log.d(TAG, "Historial de Kino no sincronizado: no hay un proveedor confirmado")
        return false
    }

    /** Historial guardado en caché, convertido a tarjetas. Lista vacía si nunca hubo sincronización. */
    fun leerHistorial(contexto: Context): List<ContentItem> {
        return try {
            val archivo = File(contexto.filesDir, ARCHIVO_CACHE)
            if (!archivo.exists()) return emptyList()
            val arreglo = JSONArray(archivo.readText(Charsets.UTF_8))
            (0 until arreglo.length()).map { i ->
                val o = arreglo.getJSONObject(i)
                ContentItem(
                    id = "historial_$i",
                    title = o.optString("title", "Video visto"),
                    subtitle = "Visto recientemente",
                    pluginSource = o.optString("plugin_source", "shorts-yt").ifBlank { "shorts-yt" },
                    targetContent = o.optString("target_content"),
                    playbackMode = o.optString("playback_mode", "resume").ifBlank { "resume" },
                    logo = o.optString("thumbnail_url", "").ifBlank { null },
                    accentColor = Color.parseColor("#B71C1C"),
                    voiceAliases = emptyList(),
                    extras = emptyMap(),
                    esMuestra = false
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Caché de historial ilegible: ${e.message}")
            emptyList()
        }
    }
}

/** Tarea de WorkManager: sincroniza y, si hubo cambios, avisa a la pantalla abierta. */
class KinoSyncWorker(contexto: Context, parametros: WorkerParameters) : Worker(contexto, parametros) {
    override fun doWork(): Result {
        if (KinoSync.sincronizar(applicationContext)) {
            applicationContext.sendBroadcast(
                Intent(KinoSync.ACCION_CACHE_ACTUALIZADA).setPackage(applicationContext.packageName)
            )
        }
        // Siempre success(): un fallo de sincronización no debe reintentar en bucle
        return Result.success()
    }
}
