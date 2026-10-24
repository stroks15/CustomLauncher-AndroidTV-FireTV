package com.tuusuario.customlauncher

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

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

    /** Programa la sincronización periódica y lanza una inmediata. Es seguro llamarlo varias veces. */
    fun programar(contexto: Context) {
        val gestor = WorkManager.getInstance(contexto)
        val periodica = PeriodicWorkRequestBuilder<KinoSyncWorker>(15, TimeUnit.MINUTES).build()
        gestor.enqueueUniquePeriodicWork(NOMBRE_TRABAJO, ExistingPeriodicWorkPolicy.KEEP, periodica)
        gestor.enqueue(OneTimeWorkRequestBuilder<KinoSyncWorker>().build())
    }

    /** Lee el provider de Kino TV y guarda un caché local. Devuelve true si se obtuvieron datos. */
    fun sincronizar(contexto: Context): Boolean {
        return try {
            val cursor = contexto.contentResolver.query(KinoContract.URI_HISTORIAL, null, null, null, null)
            if (cursor == null) {
                Log.w(TAG, "Kino TV no expone el historial (provider no disponible); se usan datos de muestra")
                return false
            }
            val registros = mutableListOf<Pair<Long, JSONObject>>()
            cursor.use { c ->
                val iTitulo = c.getColumnIndex(KinoContract.COL_TITULO)
                val iPlugin = c.getColumnIndex(KinoContract.COL_PLUGIN)
                val iTarget = c.getColumnIndex(KinoContract.COL_TARGET)
                val iModo = c.getColumnIndex(KinoContract.COL_MODO)
                val iMini = c.getColumnIndex(KinoContract.COL_MINIATURA)
                val iFecha = c.getColumnIndex(KinoContract.COL_FECHA)
                if (iTitulo < 0 || iTarget < 0) {
                    Log.w(TAG, "El provider no trae las columnas esperadas (title / target_content)")
                    return false
                }
                while (c.moveToNext()) {
                    val o = JSONObject()
                    o.put("title", c.getString(iTitulo).orEmpty())
                    o.put("plugin_source", if (iPlugin >= 0) c.getString(iPlugin).orEmpty() else "shorts-yt")
                    o.put("target_content", c.getString(iTarget).orEmpty())
                    o.put("playback_mode", if (iModo >= 0) c.getString(iModo).orEmpty() else "resume")
                    o.put("thumbnail_url", if (iMini >= 0) c.getString(iMini).orEmpty() else "")
                    registros.add((if (iFecha >= 0) c.getLong(iFecha) else 0L) to o)
                }
            }
            if (registros.isEmpty()) return false

            val salida = JSONArray()
            registros.sortedByDescending { it.first }.take(MAX_ELEMENTOS).forEach { salida.put(it.second) }
            File(contexto.filesDir, ARCHIVO_CACHE).writeText(salida.toString(), Charsets.UTF_8)
            true
        } catch (e: Exception) {
            // SecurityException (falta permiso de lectura), IllegalArgumentException, etc.
            Log.w(TAG, "No se pudo sincronizar con Kino TV: ${e.message}")
            false
        }
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
