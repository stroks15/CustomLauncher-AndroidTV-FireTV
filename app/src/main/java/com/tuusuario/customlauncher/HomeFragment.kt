package com.tuusuario.customlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.RowPresenter

/**
 * Pantalla de inicio con la jerarquía completa de filas:
 *
 *   FILA 0 — 4 aplicaciones favoritas en grande (editables, SharedPreferences)
 *   FILA 1 — Canales en vivo (televisión mexicana)
 *   FILA 2 — CONTINUAR VIENDO (automática: sólo aparece si hay películas/series abiertas; ver [ContinuarViendo])
 *   FILA 3 — Cine clásico y época de oro (+ novelas, series retro, comedia y documentales)
 *   FILA 4 — Historial de YouTube y música del recuerdo
 *
 * Las filas 1-3 vienen de assets/recomendaciones_launcher.json; la fila 3 se completa además
 * con el historial real sincronizado desde Kino TV (si está disponible).
 */
class HomeFragment : BrowseSupportFragment() {

    private lateinit var prefs: LauncherPrefs

    // Fila 0
    private val adaptadorFavoritas = ArrayObjectAdapter(
        AppCardPresenter { slot -> (activity as? MainActivity)?.elegirApp(slot) }
    )

    // Todas las filas
    private val adaptadorFilas = ArrayObjectAdapter(crearPresentadorDeFilas())

    // Fila "Continuar viendo": se inserta/quita sola justo debajo de los canales en vivo
    private val adaptadorContinuar = ArrayObjectAdapter(
        ChannelCardPresenter { item -> (activity as? MainActivity)?.confirmarQuitarContinuar(item) }
    )
    private val filaContinuar: ListRow by lazy {
        ListRow(HeaderItem(ID_CABECERA_CONTINUAR, getString(R.string.fila_continuar)), adaptadorContinuar)
    }

    private var filasJson: List<ContentRow> = emptyList()
    private val adaptadoresPorFila = mutableMapOf<String, ArrayObjectAdapter>()

    /** Se dispara cuando KinoSyncWorker actualiza el caché de historial. */
    private val receptorSync = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            recargarFilasDinamicas()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = LauncherPrefs(requireContext())

        // Sin panel lateral de encabezados: las filas ocupan todo el ancho
        headersState = HEADERS_DISABLED
        isHeadersTransitionOnBackEnabled = false

        // El nombre del launcher se muestra en la barra superior de activity_main.xml (no aquí)
        brandColor = ContextCompat.getColor(requireContext(), R.color.fondo)
        searchAffordanceColor = ContextCompat.getColor(requireContext(), R.color.amarillo_foco)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        construirFilas()
        adapter = adaptadorFilas

        // Orbe de búsqueda → dictado por voz (mismo enrutador que el botón del control)
        setOnSearchClickedListener { (activity as? MainActivity)?.onSearchRequested() }

        // Clic corto (botón central) sobre una tarjeta
        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            val actividad = activity as? MainActivity ?: return@OnItemViewClickedListener
            when (item) {
                is FavoriteApp -> actividad.lanzarApp(item.packageName)
                is ContentItem -> actividad.abrirContenido(item)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            requireContext(),
            receptorSync,
            IntentFilter(KinoSync.ACCION_CACHE_ACTUALIZADA),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        try {
            requireContext().unregisterReceiver(receptorSync)
        } catch (e: IllegalArgumentException) {
            // ya estaba desregistrado
        }
    }

    override fun onResume() {
        super.onResume()
        // Al volver de otra app: reparar defaults (p. ej. instalaron SmartTube) y refrescar historial
        refrescarFavoritas()
        recargarFilasDinamicas()
    }

    /** Regresa el foco a la Fila 0 (se usa al pulsar HOME). */
    fun volverAlInicio() {
        setSelectedPosition(0, true)
    }

    /** Vuelve a leer SharedPreferences y repinta las 4 tarjetas de la Fila 0. */
    fun refrescarFavoritas() {
        val pm = requireContext().packageManager
        prefs.resolverPorDefecto(pm)
        for (slot in 0 until LauncherPrefs.NUM_SLOTS) {
            val paquete = prefs.paqueteDeSlot(slot)
            val app = FavoriteApp(slot, paquete, AppUtils.etiqueta(pm, paquete))
            if (adaptadorFavoritas.size() > slot) {
                adaptadorFavoritas.replace(slot, app) // conserva el foco del D-Pad
            } else {
                adaptadorFavoritas.add(app)
            }
        }
    }

    // -------------------------------------------------------------------------
    //  Construcción de filas
    // -------------------------------------------------------------------------

    private fun construirFilas() {
        adaptadorFilas.clear()
        adaptadoresPorFila.clear()

        // FILA 0 — Aplicaciones favoritas
        adaptadorFilas.add(ListRow(HeaderItem(0L, getString(R.string.fila_apps)), adaptadorFavoritas))
        refrescarFavoritas()

        // FILAS 1..N — desde recomendaciones_launcher.json
        filasJson = LauncherContent.cargarFilas(requireContext())
        filasJson.forEachIndexed { indice, fila ->
            val adaptador = ArrayObjectAdapter(ChannelCardPresenter())
            itemsParaFila(fila).forEach { adaptador.add(it) }
            adaptadoresPorFila[fila.id] = adaptador
            adaptadorFilas.add(ListRow(HeaderItem((indice + 1).toLong(), fila.title), adaptador))
        }

        // FILA "CONTINUAR VIENDO" (sólo si hay algo que continuar)
        actualizarFilaContinuar(ajustarSeleccion = false)
    }

    /** Repinta "Continuar viendo" (se llama al quitar una tarjeta). */
    fun refrescarContinuar() {
        if (isAdded) actualizarFilaContinuar(ajustarSeleccion = true)
    }

    /**
     * Inserta, actualiza o quita la fila "Continuar viendo" según el registro local.
     * Va justo debajo de la fila de canales en vivo. Si se agrega/quita una fila por encima de
     * donde está el foco, se corrige la posición para que el foco no salte de fila.
     */
    private fun actualizarFilaContinuar(ajustarSeleccion: Boolean) {
        val contexto = context ?: return
        val items = ContinuarViendo.leer(contexto)
        val fila = filaContinuar
        val posActual = adaptadorFilas.indexOf(fila)
        val seleccion = if (ajustarSeleccion && view != null) selectedPosition else -1

        if (items.isEmpty()) {
            if (posActual >= 0) {
                adaptadorFilas.remove(fila)
                if (seleccion > posActual) setSelectedPosition(seleccion - 1, false)
            }
            return
        }

        val idsNuevos = items.map { it.id }
        val idsActuales = (0 until adaptadorContinuar.size()).map { (adaptadorContinuar.get(it) as? ContentItem)?.id }
        if (idsNuevos != idsActuales) {
            adaptadorContinuar.clear()
            items.forEach { adaptadorContinuar.add(it) }
        }

        if (posActual < 0) {
            val indiceEnVivo = filasJson.indexOfFirst { it.id == ID_FILA_EN_VIVO }
            // posición 0 = apps; la fila JSON i está en la posición i+1 → debajo de en_vivo = i+2
            val posicion = if (indiceEnVivo >= 0) indiceEnVivo + 2 else 1
            adaptadorFilas.add(posicion, fila)
            if (seleccion >= posicion) setSelectedPosition(seleccion + 1, false)
        }
    }

    /** Si la fila es dinámica y Kino entregó historial real, éste reemplaza a los ejemplos. */
    private fun itemsParaFila(fila: ContentRow): List<ContentItem> {
        if (!fila.dinamicaHistorial) return fila.items
        val historial = KinoSync.leerHistorial(requireContext())
        if (historial.isEmpty()) return fila.items
        return historial + fila.items.filter { !it.esMuestra }
    }

    private fun recargarFilasDinamicas() {
        if (!isAdded) return
        actualizarFilaContinuar(ajustarSeleccion = true)
        filasJson.filter { it.dinamicaHistorial }.forEach { fila ->
            val adaptador = adaptadoresPorFila[fila.id] ?: return@forEach
            adaptador.clear()
            itemsParaFila(fila).forEach { adaptador.add(it) }
        }
    }

    // -------------------------------------------------------------------------
    //  Presentador de filas: sin sombras ni atenuado, con cabeceras gigantes
    // -------------------------------------------------------------------------

    private fun crearPresentadorDeFilas(): ListRowPresenter {
        // ZOOM_FACTOR_NONE: el zoom 1.15x lo aplica BigCardView para tener control total
        val presentador = object : ListRowPresenter(FocusHighlight.ZOOM_FACTOR_NONE, false) {
            override fun initializeRowViewHolder(holder: RowPresenter.ViewHolder) {
                super.initializeRowViewHolder(holder)
                // Evita que el recorte de la fila corte las tarjetas agrandadas
                val fila = holder as ListRowPresenter.ViewHolder
                fila.gridView.clipChildren = false
                fila.gridView.clipToPadding = false
                (fila.view as? ViewGroup)?.clipChildren = false
            }
        }
        presentador.shadowEnabled = false
        presentador.selectEffectEnabled = false // sin atenuar filas no seleccionadas (más contraste)
        presentador.headerPresenter = BigRowHeaderPresenter()
        return presentador
    }

    companion object {
        private const val ID_FILA_EN_VIVO = "en_vivo"
        private const val ID_CABECERA_CONTINUAR = 100L
    }
}
