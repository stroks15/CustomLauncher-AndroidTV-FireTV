package com.tuusuario.customlauncher

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.LinearLayout

/**
 * Tarjeta base de todo el launcher.
 *
 * Al recibir el foco del D-Pad:
 *  • se agranda 1.15x (animación de escala),
 *  • se eleva sobre sus vecinas (translationZ),
 *  • y su fondo (bg_card) cambia a borde amarillo brillante de 6dp.
 *
 * Se anima desde onFocusChanged() de la propia vista para no depender de que Leanback
 * conserve o reemplace los listeners de foco.
 */
class BigCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        clipChildren = false
        clipToPadding = false
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        val escala = if (gainFocus) ESCALA_ENFOCADA else 1f
        val elevacion = if (gainFocus) 24f * resources.displayMetrics.density else 0f
        animate()
            .scaleX(escala)
            .scaleY(escala)
            .translationZ(elevacion)
            .setDuration(DURACION_MS)
            .start()
    }

    companion object {
        const val ESCALA_ENFOCADA = 1.15f
        private const val DURACION_MS = 160L
    }
}
