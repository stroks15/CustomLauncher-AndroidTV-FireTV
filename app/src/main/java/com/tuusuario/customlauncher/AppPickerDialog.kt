package com.tuusuario.customlauncher

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat

/** Fila de la lista: paquete + nombre + icono. */
private data class EntradaApp(
    val paquete: String,
    val etiqueta: String,
    val icono: Drawable
)

/**
 * AlertDialog con todas las apps instaladas (packageManager.getInstalledApplications()).
 * Se navega con el D-Pad; al elegir una, se devuelve su Package Name para guardarlo en
 * SharedPreferences desde MainActivity.elegirApp().
 */
object AppPickerDialog {

    fun mostrar(actividad: Activity, slot: Int, alElegir: (paquete: String) -> Unit) {
        val pm = actividad.packageManager

        // 1) Todas las apps instaladas que se pueden abrir (se excluye el propio launcher)
        @Suppress("DEPRECATION")
        val instaladas = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val entradas: List<EntradaApp> = instaladas
            .asSequence()
            .filter { it.packageName != actividad.packageName }
            .filter { AppUtils.intentDeLanzamiento(pm, it.packageName) != null }
            .distinctBy { it.packageName }
            .map { EntradaApp(it.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm)) }
            .sortedBy { it.etiqueta.lowercase() }
            .toList()

        // 2) Adaptador con filas grandes (icono 64dp + texto 28sp)
        val adaptador = object : ArrayAdapter<EntradaApp>(actividad, 0, entradas) {
            override fun getView(posicion: Int, convertView: View?, parent: ViewGroup): View {
                val fila = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_app_picker, parent, false)
                val entrada = getItem(posicion)
                fila.findViewById<ImageView>(R.id.picker_icono).setImageDrawable(entrada?.icono)
                fila.findViewById<TextView>(R.id.picker_nombre).text = entrada?.etiqueta
                return fila
            }
        }

        // 3) Título en 32sp
        val titulo = TextView(actividad).apply {
            text = actividad.getString(R.string.dialogo_titulo, slot + 1)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(40, 32, 40, 24)
        }

        // 4) Diálogo
        val dialogo = AlertDialog.Builder(actividad, R.style.DialogoTV)
            .setCustomTitle(titulo)
            .setAdapter(adaptador) { _, indice -> alElegir(entradas[indice].paquete) }
            .setNegativeButton(R.string.cancelar, null)
            .create()

        dialogo.setOnShowListener {
            // Resaltado amarillo de la fila enfocada y botón Cancelar grande
            dialogo.listView.selector = ContextCompat.getDrawable(actividad, R.drawable.bg_picker_foco)
            dialogo.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
                setTextColor(ContextCompat.getColor(actividad, R.color.amarillo_foco))
            }
            dialogo.listView.requestFocus()
        }
        dialogo.show()
    }
}
