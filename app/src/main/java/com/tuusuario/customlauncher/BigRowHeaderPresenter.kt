package com.tuusuario.customlauncher

import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.widget.TextView
import androidx.leanback.widget.RowHeaderPresenter
import androidx.leanback.widget.Presenter

/** Títulos de fila en 32sp, negrita y blanco puro (jerarquía visual gigante). */
class BigRowHeaderPresenter : RowHeaderPresenter() {

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any?) {
        super.onBindViewHolder(viewHolder, item)
        val titulo = viewHolder.view.findViewById<TextView>(androidx.leanback.R.id.row_header)
            ?: (viewHolder.view as? TextView)
        titulo?.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
        }
    }
}
