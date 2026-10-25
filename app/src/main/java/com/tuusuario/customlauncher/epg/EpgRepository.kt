package com.tuusuario.customlauncher.epg

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import com.tuusuario.customlauncher.PluginRegistry
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/** XMLTV/JSON EPG asíncrona. No incluye feeds de terceros por defecto. */
object EpgRepository {
    const val ACTION_EPG_UPDATED = "com.tuusuario.customlauncher.EPG_UPDATED"
    private const val TAG = "EpgRepository"
    private val executor = Executors.newSingleThreadExecutor()
    private val currentByChannel = ConcurrentHashMap<String, String>()
    @Volatile private var lastRefreshUrl = ""
    @Volatile private var lastRefreshAt = 0L

    fun currentProgram(channelId: String?): String? =
        channelId?.takeIf { it.isNotBlank() }?.let(currentByChannel::get)

    fun refreshConfigured(context: Context) {
        val feed = PluginRegistry.epgFeedUrl(context)
        if (feed.isNotBlank()) refresh(context.applicationContext, feed)
    }

    /** Descarga y parsea fuera del hilo UI. Sólo acepta URL HTTP(S) configurada explícitamente. */
    fun refresh(context: Context, feedUrl: String) {
        val now = System.currentTimeMillis()
        synchronized(this) {
            if (feedUrl == lastRefreshUrl && now - lastRefreshAt < 15 * 60 * 1000L) return
            lastRefreshUrl = feedUrl
            lastRefreshAt = now
        }
        val uri = runCatching { Uri.parse(feedUrl) }.getOrNull() ?: return
        if (uri.scheme !in setOf("https", "http") || uri.host.isNullOrBlank()) {
            Log.w(TAG, "Feed EPG ignorado: URL inválida")
            return
        }
        executor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = URL(feedUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 5_000
                connection.readTimeout = 8_000
                connection.setRequestProperty("User-Agent", "CustomLauncherTV/2.0")
                connection.connect()
                if (connection.responseCode !in 200..299) throw IllegalStateException("HTTP ${connection.responseCode}")
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val parsed = if (body.trimStart().startsWith("{") || body.trimStart().startsWith("[")) parseJson(body) else parseXmlTv(body)
                currentByChannel.clear()
                currentByChannel.putAll(parsed)
                context.applicationContext.sendBroadcast(Intent(ACTION_EPG_UPDATED).setPackage(context.packageName))
                Log.i(TAG, "EPG cargada: ${parsed.size} canales")
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo actualizar EPG: ${e.message}")
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun parseJson(text: String): Map<String, String> {
        val root = text.trim()
        val programs = if (root.startsWith("[")) JSONArray(root) else JSONObject(root).optJSONArray("programmes") ?: JSONArray()
        val now = System.currentTimeMillis()
        val result = mutableMapOf<String, Pair<Long, String>>()
        for (i in 0 until programs.length()) {
            val p = programs.optJSONObject(i) ?: continue
            val channel = p.optString("channel_id", p.optString("channel"))
            val title = p.optString("title")
            val start = parseTime(p.optString("start"))
            val end = parseTime(p.optString("end"))
            if (channel.isNotBlank() && title.isNotBlank() && start <= now && now < end) result[channel] = start to title
        }
        return result.mapValues { it.value.second }
    }

    private fun parseXmlTv(text: String): Map<String, String> {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(text.reader())
        val now = System.currentTimeMillis()
        val result = mutableMapOf<String, Pair<Long, String>>()
        var event = parser.eventType
        var channel = ""
        var start = 0L
        var end = 0L
        var title = ""
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "programme" -> {
                        channel = parser.getAttributeValue(null, "channel").orEmpty()
                        start = parseTime(parser.getAttributeValue(null, "start").orEmpty())
                        end = parseTime(parser.getAttributeValue(null, "stop").orEmpty())
                        title = ""
                    }
                    "title" -> if (channel.isNotBlank()) title = parser.nextText().trim()
                }
                XmlPullParser.END_TAG -> if (parser.name == "programme") {
                    if (channel.isNotBlank() && title.isNotBlank() && start <= now && now < end) result[channel] = start to title
                    channel = ""
                }
            }
            event = parser.next()
        }
        return result.mapValues { it.value.second }
    }

    private fun parseTime(value: String): Long {
        if (value.isBlank()) return 0L
        val patterns = listOf("yyyyMMddHHmmss Z", "yyyyMMddHHmmss", "yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss'Z'")
        for (pattern in patterns) {
            try {
                val format = SimpleDateFormat(pattern, Locale.ROOT)
                if (pattern.endsWith("'Z'")) format.timeZone = TimeZone.getTimeZone("UTC")
                return format.parse(value.trim())?.time ?: continue
            } catch (_: Exception) { }
        }
        return runCatching { value.toLong() }.getOrDefault(0L).let { if (it in 1..9_999_999_999L) it * 1000 else it }
    }
}
