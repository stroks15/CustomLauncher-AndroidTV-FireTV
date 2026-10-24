package com.tuusuario.customlauncher

/** Una de las 4 aplicaciones gigantes de la Fila 0. */
data class FavoriteApp(
    val slot: Int,
    val packageName: String,
    val label: String
)

/**
 * Tarjeta de contenido (canal, cine, música, historial…) leída de recomendaciones_launcher.json
 * o del historial sincronizado con Kino TV.
 *
 * Los tres campos pluginSource / targetContent / playbackMode son exactamente los extras
 * ("plugin_source", "target_content", "playback_mode") que recibe Kino TV.
 */
data class ContentItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val pluginSource: String,
    val targetContent: String,
    val playbackMode: String,
    /** Ruta en assets ("logos/canal5.png") o URL http(s). Si falta, la tarjeta muestra el nombre en grande. */
    val logo: String?,
    val accentColor: Int,
    val voiceAliases: List<String>,
    /** Extras adicionales opcionales (clave → valor) que se agregan al intent. */
    val extras: Map<String, String>,
    /** true = dato de ejemplo que se oculta cuando Kino TV entrega historial real. */
    val esMuestra: Boolean
)

/** Una fila Leanback completa. */
data class ContentRow(
    val id: String,
    val title: String,
    /** true = esta fila se completa con el historial real sincronizado desde Kino TV. */
    val dinamicaHistorial: Boolean,
    val items: List<ContentItem>
)
