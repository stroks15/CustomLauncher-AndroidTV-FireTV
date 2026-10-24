package com.tuusuario.customlauncher

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.Presenter

/**
 * Pinta cada tarjeta gigante de la Fila 0.
 *
 * El OnLongClickListener (mantener presionado el botón central del control) llama a
 * [alMantenerPresionado] con el número de ranura, que abre el AlertDialog con la lista
 * de apps obtenida de packageManager.getInstalledApplications().
 */
class AppCardPresenter(
    private val alMantenerPresionado: (slot: Int) -> Unit
) : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_card, parent, false)
        return Presenter.ViewHolder(vista)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any?) {
        val app = item as? FavoriteApp ?: return
        val vista = viewHolder.view
        val icono = vista.findViewById<ImageView>(R.id.icono)
        val etiqueta = vista.findViewById<TextView>(R.id.etiqueta)
        val pm = vista.context.packageManager

        if (AppUtils.estaInstalada(pm, app.packageName)) {
            try {
                icono.setImageDrawable(pm.getApplicationIcon(app.packageName))
            } catch (e: Exception) {
                icono.setImageResource(R.drawable.ic_agregar)
            }
            etiqueta.text = AppUtils.etiqueta(pm, app.packageName)
        } else {
            // App no instalada: se invita a elegir otra
            icono.setImageResource(R.drawable.ic_agregar)
            etiqueta.text = vista.context.getString(R.string.elegir_app)
        }
        vista.contentDescription = etiqueta.text

        // Botón central sostenido → cambiar la app de esta ranura
        vista.setOnLongClickListener {
            alMantenerPresionado(app.slot)
            true
        }
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        viewHolder.view.setOnLongClickListener(null)
    }
}
