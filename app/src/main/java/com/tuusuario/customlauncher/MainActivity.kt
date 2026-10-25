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
import com.tuusuario.customlauncher.utils.VoiceCommandValidator
import com.tuusuario.customlauncher.player.PlayerActivity

/**
 * Launcher Android TV / Fire TV.
 * Los comandos de voz se aceptan sólo si terminan en "dexter" y se resuelven contra
 * recomendaciones_launcher.json. El vídeo se reproduce con Media3 cuando hay una URL
 * HLS/DASH HTTP(S) explícita; no se inventan endpoints ni se depende de un reproductor externo.
 */
class MainActivity : FragmentActivity() {

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
        if (intent == null || intent.action != Intent.ACTION_SEARCH) return
        val raw = intent.getStringExtra(android.app.SearchManager.QUERY).orEmpty()
        // La frase de activación se exige para órdenes de voz recibidas por el sistema.
        val command = VoiceCommandValidator.validateAndCleanCommand(raw)
        if (command == null) {
            Log.w(TAG, "Orden de voz ignorada: sufijo de activación ausente")
            return
        }
        Log.i(TAG, "Orden de voz validada")
        enrutarComandoVoz(command)
    }

    private fun normalizarTexto(texto: String): String {
        // Quita diacríticos de forma general y normaliza espacios sin depender del idioma del equipo.
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .lowercase(Locale.ROOT)
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    /**
     * Resuelve el texto únicamente contra el catálogo local. La reproducción directa requiere
     * que el elemento tenga extras.stream_url con una URL HTTP(S) autorizada/configurada.
     */
    private fun enrutarComandoVoz(query: String) {
        val command = normalizarTexto(query)
            .replace(Regex("^(abre|pon|reproduce|reproducir|busca|buscar|quiero ver|ponme|dame)\\s+"), "")
            .trim()
        val catalog = LauncherContent.cargarFilas(this).flatMap { it.items }
        val item = catalog.firstOrNull { candidate ->
            val names = listOf(candidate.title) + candidate.voiceAliases
            names.any { alias ->
                val normalizedAlias = normalizarTexto(alias)
                command == normalizedAlias || command.endsWith(" " + normalizedAlias)
            }
        }
        if (item == null) {
            aviso("No encontré «$query» en el catálogo. Agrega el elemento y su stream_url autorizado al JSON.")
            return
        }
        abrirContenido(item)
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
            val frase = origen.getStringExtra(android.app.SearchManager.QUERY)
                ?: origen.getStringExtra("android.intent.extra.MEDIA_TITLE")
                ?: origen.getStringExtra(Intent.EXTRA_TEXT)
                ?: ""
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

    /** Reproduce en el motor Media3 integrado; no delega el video a otra aplicación. */
    fun abrirContenido(item: ContentItem) {
        val streamUrl = item.extras["stream_url"]?.trim().takeUnless { it.isNullOrBlank() }
            ?: item.targetContent.takeIf { it.startsWith("https://") || it.startsWith("http://") }
        if (streamUrl.isNullOrBlank()) {
            aviso("«${item.title}» no tiene stream_url configurada. Añade una URL HLS/DASH autorizada al catálogo.")
            return
        }
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_STREAM_URL, streamUrl)
            putExtra(PlayerActivity.EXTRA_TITLE, item.title)
        })
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
