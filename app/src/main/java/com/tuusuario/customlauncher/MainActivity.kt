package com.tuusuario.customlauncher

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognizerIntent
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.Gravity
import android.graphics.Typeface
import android.widget.TextView
import android.widget.Toast
import java.text.Normalizer
import java.util.Locale
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * MainActivity — Pantalla de inicio (HOME) del Custom Launcher.
 *
 * Responsabilidades:
 *  1. Alojar el HomeFragment (filas Leanback).
 *  2. Procesar y enrutar los comandos de voz (SpeechRecognitionRouter) hacia Kino TV.
 *  3. Lanzar apps (Fila 0), mostrar el selector de apps y abrir contenidos de Kino TV.
 *  4. Barra superior (nombre del launcher + accesos directos a Ajustes y WiFi + hora).
 *  5. Registrar automáticamente películas/series abiertas para la fila "Continuar viendo".
 *
 * NOTA SOBRE EL SCRIPT DE VOZ:
 *  El bloque marcado como "SPEECH RECOGNITION ROUTER" conserva íntegras las reglas
 *  de enrutamiento solicitadas. Los únicos cambios respecto al texto original son:
 *    [CAMBIO 1] "\$query" → "$query" (el "\$" escapado imprimía el texto literal "$query").
 *    [CAMBIO 2] onCreate()/onNewIntent() llaman a despacharVoz() (que a su vez llama a
 *               handleIntent()) para normalizar intents de asistentes y evitar cierres inesperados.
 *    [CAMBIO 3] una línea en el catch de lanzarKinoPlugin(): abrirKinoConExtras() como plan B.
 *    [CAMBIO 4] lanzarKinoPlugin() recibe parámetros opcionales (título, extras…) y registra la
 *               película/serie en "Continuar viendo". Las reglas de voz no se modificaron.
 */
class MainActivity : FragmentActivity() {

    private val KINO_PACKAGE = "com.arkiv.player.light"
    private val TAG = "CustomLauncherVoice"

    // Preferencias persistentes de las 4 apps de la Fila 0
    private lateinit var prefs: LauncherPrefs

    /**
     * Dictado por voz integrado (modo híbrido): si el control remoto no entrega el texto
     * por ACTION_SEARCH, el botón de la orbe de búsqueda o KEYCODE_SEARCH abre el
     * reconocedor del sistema y el resultado entra por el mismo enrutador.
     */
    private val dictadoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        val frase = resultado.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (resultado.resultCode == RESULT_OK && !frase.isNullOrBlank()) {
            despacharVoz(Intent(Intent.ACTION_SEARCH).putExtra(android.app.SearchManager.QUERY, frase))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Lógica de inflado de UI Leanback
        setContentView(R.layout.activity_main)
        prefs = LauncherPrefs(this)
        configurarBarraSuperior()
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.contenedor_home, HomeFragment(), ETIQUETA_HOME)
                .commit()
        }
        // Sincronización periódica en segundo plano con Kino TV (historial de YouTube)
        KinoSync.programar(applicationContext)
        despacharVoz(intent) // [CAMBIO 2] (original: handleIntent(intent))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Al pulsar HOME, regresar siempre a la primera fila (apps favoritas)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            homeFragment()?.volverAlInicio()
        }
        despacharVoz(intent) // [CAMBIO 2] (original: handleIntent(intent))
    }

    // =========================================================================
    //  SPEECH RECOGNITION ROUTER — script solicitado (íntegro)
    // =========================================================================

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(android.app.SearchManager.QUERY) ?: ""
            Log.i(TAG, "Voz capturada en control remoto: $query") // [CAMBIO 1]
            enrutarComandoVoz(query)
        }
    }

    private fun normalizarTexto(texto: String): String {
        // Quita diacríticos de forma general y normaliza espacios sin depender del idioma del equipo.
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .lowercase(Locale.ROOT)
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun enrutarComandoVoz(query: String) {
        val limpia = normalizarTexto(query)
        val launchIntent = Intent(Intent.ACTION_VIEW)
        launchIntent.`package` = KINO_PACKAGE
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        when {
            // 1. Mapeo automático a Plugin Xuper TV (Televisión Abierta Mexicana)
            limpia.contains("estrellas") -> lanzarKinoPlugin(launchIntent, "xuper", "Canal de las Estrellas", "live")
            limpia.contains("canal 5") || limpia.contains("canal cinco") || limpia.contains("pon el 5") -> lanzarKinoPlugin(launchIntent, "xuper", "Canal 5", "live")
            limpia.contains("n+") || limpia.contains("n mas") -> lanzarKinoPlugin(launchIntent, "xuper", "Canal N+", "live")
            limpia.contains("multimedios") -> lanzarKinoPlugin(launchIntent, "xuper", "Canal Multimedios de la CDMX", "live")
            limpia.contains("canal 7") || limpia.contains("canal siete") -> lanzarKinoPlugin(launchIntent, "xuper", "Canal 7", "live")
            limpia.contains("azteca uno") && !limpia.contains("hora") -> lanzarKinoPlugin(launchIntent, "xuper", "Canal Azteca Uno", "live")
            limpia.contains("gourmet") -> lanzarKinoPlugin(launchIntent, "xuper", "Canal El Gourmet", "live")

            // 2. Mapeo automático a Plugin Pluto TV (Retraso Horario y Religioso)
            limpia.contains("enlace") -> lanzarKinoPlugin(launchIntent, "pluto", "Canal Enlace", "live")
            limpia.contains("azteca") && (limpia.contains("menos una hora") || limpia.contains("menos 1 hora") || limpia.contains("1 hora antes")) -> {
                lanzarKinoPlugin(launchIntent, "pluto", "Azteca Uno -1 Hora", "live")
            }

            // 3. Mapeo automático a Culturales y Estatales
            limpia.contains("canal 11") || limpia.contains("canal once") -> lanzarKinoPlugin(launchIntent, "cultural", "Canal 11", "live")
            limpia.contains("canal 22") || limpia.contains("canal veintidos") -> lanzarKinoPlugin(launchIntent, "cultural", "Canal 22", "live")
            limpia.contains("tv unam") || limpia.contains("unam") -> lanzarKinoPlugin(launchIntent, "cultural", "TV UNAM", "live")

            // 4. Mapeo automático a Cine de Oro y Clásicos (Plugin Internet Archive)
            limpia.contains("pedro infante") || limpia.contains("cantinflas") || limpia.contains("jorge negrete") || limpia.contains("cine de oro") || limpia.contains("peliculas viejas") -> {
                lanzarKinoPlugin(launchIntent, "archive", query.replace("ver", "").replace("pon", "").trim(), "search")
            }

            // 5. Mapeo a los Nuevos Plugins de Entretenimiento
            limpia.contains("novela") || limpia.contains("telenovela") || limpia.contains("drama") -> lanzarKinoPlugin(launchIntent, "novelas", "telenovelas_list", "menu")
            limpia.contains("documental") || limpia.contains("historia") || limpia.contains("naturaleza") -> lanzarKinoPlugin(launchIntent, "docs", "documentales_list", "menu")
            limpia.contains("serie vieja") || limpia.contains("retro") || limpia.contains("series de antes") -> lanzarKinoPlugin(launchIntent, "retro-tv", "retro_list", "menu")
            limpia.contains("comedia") || limpia.contains("risa") || limpia.contains("humor") -> lanzarKinoPlugin(launchIntent, "comedia", "comedia_list", "menu")
            limpia.contains("regional") || limpia.contains("canal del estado") -> lanzarKinoPlugin(launchIntent, "regionales", "regionales_list", "menu")
            limpia.contains("musica") || limpia.contains("bolero") || limpia.contains("ranchera") -> lanzarKinoPlugin(launchIntent, "audios", "musica_recuerdo", "stream")

            // 6. Historial continuo de YouTube / Shorts (plugin shorts-yt)
            (limpia.contains("youtube") && (limpia.contains("historial") || limpia.contains("visto") || limpia.contains("videos"))) ||
                limpia.contains("shorts") || limpia.contains("continuar viendo") ->
                lanzarKinoPlugin(launchIntent, "shorts-yt", "historial_youtube", "resume")

            // Fallback: Si no coincide, manda el texto crudo a la búsqueda general de Kino
            else -> {
                launchIntent.putExtra("action", "global_search")
                launchIntent.putExtra("search_query", query)
                startActivity(launchIntent)
                Log.d(TAG, "Comando genérico. Invocando búsqueda global de Kino para: $query") // [CAMBIO 1]
            }
        }
    }

    private fun lanzarKinoPlugin(
        intent: Intent,
        source: String,
        target: String,
        mode: String,
        titulo: String? = null,                    // [CAMBIO 4] datos opcionales para "Continuar viendo"
        extras: Map<String, String> = emptyMap(),
        acento: Int? = null,
        logo: String? = null
    ) {
        // [CAMBIO 4] Películas/series (no canales en vivo ni música) → fila "Continuar viendo"
        if (ContinuarViendo.aplica(source, mode) && AppUtils.estaInstalada(packageManager, KINO_PACKAGE)) {
            ContinuarViendo.registrar(
                this,
                titulo ?: ContinuarViendo.tituloLegible(source, target),
                source, target, mode, extras, acento, logo
            )
        }
        intent.putExtra("plugin_source", source)
        intent.putExtra("target_content", target)
        intent.putExtra("playback_mode", mode)
        try {
            startActivity(intent)
            Log.i(TAG, "Intent despachado con éxito: Source=$source, Target=$target, Mode=$mode") // [CAMBIO 1]
        } catch (e: Exception) {
            Log.e(TAG, "Error al lanzar Kino TV. ¿Está instalada la app?: ${e.message}") // [CAMBIO 1]
            abrirKinoConExtras(intent) // [CAMBIO 3] plan B: abrir Kino por su icono llevando los mismos extras
        }
    }

    // =========================================================================
    //  EXTENSIONES DEL MODO HÍBRIDO (Alexa / Google Assistant / dictado propio)
    // =========================================================================

    /**
     * Punto único de entrada de voz. Normaliza intents de asistentes y protege contra
     * cualquier excepción para que el launcher (pantalla de inicio) nunca se cierre.
     */
    private fun despacharVoz(intent: Intent?) {
        try {
            handleIntent(normalizarIntentDeVoz(intent))
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo procesar el comando de voz: ${e.message}")
            aviso(getString(R.string.no_se_pudo_abrir))
        }
    }

    /**
     * Algunos asistentes envían "android.media.action.MEDIA_PLAY_FROM_SEARCH" en lugar de
     * ACTION_SEARCH. Aquí se convierte al formato que espera el enrutador.
     */
    private fun normalizarIntentDeVoz(origen: Intent?): Intent? {
        if (origen == null) return null
        if (origen.action == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) {
            val frase = origen.getStringExtra(android.app.SearchManager.QUERY) ?: ""
            return Intent(Intent.ACTION_SEARCH).putExtra(android.app.SearchManager.QUERY, frase)
        }
        return origen
    }

    /** Se invoca con la tecla SEARCH del control o al pulsar la orbe de búsqueda del launcher. */
    override fun onSearchRequested(): Boolean {
        iniciarDictado()
        return true
    }

    private fun iniciarDictado() {
        val dictado = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.prompt_voz))
        }
        try {
            dictadoLauncher.launch(dictado)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Este equipo no incluye un servicio de dictado: ${e.message}")
            aviso(getString(R.string.sin_dictado))
        }
    }

    /** Plan B: abre Kino TV mediante su intent de lanzamiento y le pasa los extras originales. */
    private fun abrirKinoConExtras(origen: Intent) {
        val lanzamiento = AppUtils.intentDeLanzamiento(packageManager, KINO_PACKAGE)
        if (lanzamiento == null) {
            aviso(getString(R.string.kino_no_instalada))
            return
        }
        origen.extras?.let { lanzamiento.putExtras(it) }
        lanzamiento.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(lanzamiento)
            Log.i(TAG, "Plan B: Kino TV abierto por su icono con los extras del comando")
        } catch (e: Exception) {
            Log.e(TAG, "Plan B falló: ${e.message}")
            aviso(getString(R.string.no_se_pudo_abrir))
        }
    }

    // =========================================================================
    //  ACCIONES DE LA INTERFAZ (llamadas desde HomeFragment)
    // =========================================================================

    /** Abre una tarjeta de contenido (canal, cine, música…) usando los mismos extras que la voz. */
    fun abrirContenido(item: ContentItem) {
        val abrir = Intent(Intent.ACTION_VIEW)
        abrir.`package` = KINO_PACKAGE
        abrir.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // Extras adicionales opcionales definidos en el JSON (p. ej. genre=bolero)
        item.extras.forEach { (clave, valor) -> abrir.putExtra(clave, valor) }
        // Tarjeta de "Continuar viendo": se avisa a Kino TV que es una reanudación
        if (item.id.startsWith(ContinuarViendo.PREFIJO_ID)) {
            abrir.putExtra(ContinuarViendo.EXTRA_CONTINUAR, true)
        }
        lanzarKinoPlugin(
            abrir, item.pluginSource, item.targetContent, item.playbackMode,
            item.title, item.extras, item.accentColor, item.logo
        )
    }

    /** Botón central sostenido sobre una tarjeta de "Continuar viendo" → quitarla (ya la terminó). */
    fun confirmarQuitarContinuar(item: ContentItem) {
        val titulo = TextView(this).apply {
            text = getString(R.string.quitar_continuar_titulo, item.title)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(40, 32, 40, 16)
        }
        val dialogo = AlertDialog.Builder(this, R.style.DialogoTV)
            .setCustomTitle(titulo)
            .setMessage(R.string.quitar_continuar_mensaje)
            .setPositiveButton(R.string.quitar_continuar_si) { _, _ ->
                ContinuarViendo.quitar(this, item.id)
                homeFragment()?.refrescarContinuar()
            }
            .setNegativeButton(R.string.cancelar, null)
            .create()
        dialogo.setOnShowListener {
            listOf(AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE).forEach { boton ->
                dialogo.getButton(boton)?.apply {
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.amarillo_foco))
                }
            }
            dialogo.getButton(AlertDialog.BUTTON_NEGATIVE)?.requestFocus()
        }
        dialogo.show()
    }

    // =========================================================================
    //  BARRA SUPERIOR: nombre del launcher + accesos directos (Ajustes, WiFi) + hora
    // =========================================================================

    private fun configurarBarraSuperior() {
        findViewById<View>(R.id.btn_ajustes).setOnClickListener { abrirAjustes() }
        findViewById<View>(R.id.btn_wifi).setOnClickListener { abrirWifi() }
        // La hora (TextClock) se actualiza sola y respeta el formato 12/24 h del sistema
    }

    private fun abrirAjustes() {
        val candidatos = listOf(Intent(Settings.ACTION_SETTINGS))
        if (!iniciarPrimeroDisponible(candidatos)) lanzarApp(prefs.paqueteDeSlot(2))
    }

    /**
     * Abre la pantalla de WiFi. Orden: ajuste estándar de Android → pantalla de red de Fire OS
     * (mejor esfuerzo; el nombre interno puede variar según la versión) → Ajustes generales.
     */
    private fun abrirWifi() {
        val candidatos = listOf(
            Intent(Settings.ACTION_WIFI_SETTINGS),
            Intent().setClassName(
                "com.amazon.tv.settings.v2",
                "com.amazon.tv.settings.v2.tv.network.NetworkActivity"
            ),
            Intent(Settings.ACTION_SETTINGS)
        )
        if (!iniciarPrimeroDisponible(candidatos)) aviso(getString(R.string.no_se_pudo_abrir))
    }

    /** Intenta cada intent en orden y se detiene en el primero que se abre. */
    private fun iniciarPrimeroDisponible(candidatos: List<Intent>): Boolean {
        for (candidato in candidatos) {
            try {
                candidato.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(candidato)
                return true
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo abrir ${candidato.action ?: candidato.component}: ${e.message}")
            }
        }
        return false
    }

    /** Lanza una app de la Fila 0. */
    fun lanzarApp(paquete: String) {
        val intentApp = AppUtils.intentDeLanzamiento(packageManager, paquete)
        if (intentApp == null) {
            aviso(getString(R.string.app_no_instalada))
            return
        }
        intentApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intentApp)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo abrir $paquete: ${e.message}")
            aviso(getString(R.string.no_se_pudo_abrir))
        }
    }

    /** OnLongClick (botón central sostenido) → lista de apps → guardar en SharedPreferences. */
    fun elegirApp(slot: Int) {
        AppPickerDialog.mostrar(this, slot) { paqueteElegido ->
            prefs.guardar(slot, paqueteElegido)
            homeFragment()?.refrescarFavoritas()
        }
    }

    private fun homeFragment(): HomeFragment? =
        supportFragmentManager.findFragmentByTag(ETIQUETA_HOME) as? HomeFragment

    /** Mensaje emergente en letra grande (28sp) y alto contraste. */
    private fun aviso(texto: String) {
        val vista = TextView(this).apply {
            text = texto
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.parseColor("#FFEB3B"))
            setPadding(48, 28, 48, 28)
            gravity = Gravity.CENTER
        }
        @Suppress("DEPRECATION")
        Toast(applicationContext).apply {
            view = vista
            duration = Toast.LENGTH_LONG
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 80)
        }.show()
    }

    companion object {
        private const val ETIQUETA_HOME = "home_fragment"
    }
}
