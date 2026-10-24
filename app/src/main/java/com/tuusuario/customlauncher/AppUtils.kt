package com.tuusuario.customlauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings

/** Utilidades para consultar y lanzar aplicaciones instaladas. */
object AppUtils {

    /** Paquetes conocidos de "Ajustes" en Android TV / Google TV / Fire OS. */
    val PAQUETES_AJUSTES = setOf(
        "com.android.tv.settings",
        "com.amazon.tv.settings.v2",
        "com.amazon.tv.settings",
        "com.android.settings"
    )

    /**
     * Intent para abrir una app. Prioriza el lanzador de TV (LEANBACK_LAUNCHER), luego el
     * lanzador normal y, para "Ajustes", cae a Settings.ACTION_SETTINGS (porque en TV esa
     * pantalla no siempre expone un icono de lanzamiento).
     */
    fun intentDeLanzamiento(pm: PackageManager, paquete: String): Intent? {
        pm.getLeanbackLaunchIntentForPackage(paquete)?.let { return it }
        pm.getLaunchIntentForPackage(paquete)?.let { return it }
        if (paquete in PAQUETES_AJUSTES || paquete == paqueteDeAjustes(pm)) {
            val ajustes = Intent(Settings.ACTION_SETTINGS)
            if (ajustes.resolveActivity(pm) != null) return ajustes
        }
        return null
    }

    fun estaInstalada(pm: PackageManager, paquete: String): Boolean =
        intentDeLanzamiento(pm, paquete) != null

    /** Paquete que atiende Settings.ACTION_SETTINGS en este equipo (puede variar entre marcas). */
    fun paqueteDeAjustes(pm: PackageManager): String? =
        Intent(Settings.ACTION_SETTINGS).resolveActivity(pm)?.packageName

    /** Nombre visible de la app; si no está instalada devuelve el nombre del paquete. */
    fun etiqueta(pm: PackageManager, paquete: String): String {
        return try {
            @Suppress("DEPRECATION")
            pm.getApplicationLabel(pm.getApplicationInfo(paquete, 0)).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            paquete
        }
    }
}
