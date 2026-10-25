package com.tuusuario.customlauncher

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.tuusuario.customlauncher.epg.EpgRepository
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

/**
 * Pinta las tarjetas de las Filas 1-3 (canales, cine clásico, YouTube/música).
 *
 * Logotipos: este proyecto NO incluye logotipos de canales (son marcas registradas).
 * Coloca tus archivos en app/src/main/assets/logos/ y referencia la ruta en el JSON
 * ("logo": "logos/canal5.png") o usa una URL http(s). Mientras no exista el archivo, la
 * tarjeta muestra el nombre del canal en letra gigante sobre su color de acento.
 */
class ChannelCardPresenter(
    /** Opcional: botón central sostenido sobre la tarjeta (se usa en "Continuar viendo" para quitarla). */
    private val alMantenerPresionado: ((ContentItem) -> Unit)? = null
) : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_canal_card, parent, false)
        return Presenter.ViewHolder(vista)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any?) {
        val contenido = item as? ContentItem ?: return
        val vista = viewHolder.view
        val zona = vista.findViewById<View>(R.id.zona_logo)
        val logo = vista.findViewById<ImageView>(R.id.logo)
        val textoLogo = vista.findViewById<TextView>(R.id.texto_logo)
        val titulo = vista.findViewById<TextView>(R.id.titulo)
        val subtitulo = vista.findViewById<TextView>(R.id.subtitulo)

        zona.setBackgroundColor(contenido.accentColor)
        titulo.text = contenido.title
        subtitulo.text = EpgRepository.currentProgram(contenido.extras["epg_channel_id"])
            ?: contenido.subtitle
        textoLogo.text = contenido.title.uppercase()
        vista.contentDescription = contenido.title

        val accion = alMantenerPresionado
        if (accion != null) {
            vista.setOnLongClickListener {
                accion(contenido)
                true
            }
        }

        if (contenido.logo.isNullOrBlank()) {
            mostrarSoloTexto(logo, textoLogo, titulo)
            return
        }

        mostrarLogo(logo, textoLogo, titulo)
        Glide.with(logo.context.applicationContext)
            .load(rutaParaGlide(contenido.logo))
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    // Logotipo inexistente o sin red → nombre en grande
                    logo.post { mostrarSoloTexto(logo, textoLogo, titulo) }
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean = false
            })
            .into(logo)
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        if (alMantenerPresionado != null) viewHolder.view.setOnLongClickListener(null)
        val logo = viewHolder.view.findViewById<ImageView>(R.id.logo)
        Glide.with(logo.context.applicationContext).clear(logo)
        logo.setImageDrawable(null)
    }

    private fun rutaParaGlide(ruta: String?): String {
        val limpia = ruta.orEmpty()
        return if (limpia.startsWith("http://") || limpia.startsWith("https://")) {
            limpia
        } else {
            "file:///android_asset/$limpia"
        }
    }

    private fun mostrarLogo(logo: View, textoLogo: View, titulo: View) {
        logo.visibility = View.VISIBLE
        textoLogo.visibility = View.GONE
        titulo.visibility = View.VISIBLE
    }

    private fun mostrarSoloTexto(logo: View, textoLogo: View, titulo: View) {
        logo.visibility = View.GONE
        textoLogo.visibility = View.VISIBLE
        titulo.visibility = View.GONE
    }
}
