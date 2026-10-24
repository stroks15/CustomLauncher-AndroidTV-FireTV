package com.tuusuario.customlauncher

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager

/**
 * Persistencia (SharedPreferences) de las 4 apps de la Fila 0.
 *
 * Nada está "cableado" en las tarjetas: cada ranura guarda su Package Name bajo la clave
 * "fav_slot_N". Los valores por defecto (Kino TV, SmartTube, Settings y Media Player) sólo
 * se usan hasta que la persona elige otra app con el botón central sostenido.
 */
class LauncherPrefs(context: Context) {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    /** Package Name guardado para la ranura (o el primer candidato por defecto si nunca se guardó). */
    fun paqueteDeSlot(slot: Int): String =
        sp.getString(CLAVE_FAV + slot, null) ?: CANDIDATAS_POR_DEFECTO[slot].first()

    /** Guarda la elección manual de la persona (queda marcada para no ser reemplazada por defaults). */
    fun guardar(slot: Int, paquete: String) {
        sp.edit()
            .putString(CLAVE_FAV + slot, paquete)
            .putBoolean(CLAVE_ELEGIDA_POR_USUARIO + slot, true)
            .apply()
    }

    /** Vuelve a la app predeterminada de la ranura. */
    fun restablecer(slot: Int) {
        sp.edit()
            .remove(CLAVE_FAV + slot)
            .remove(CLAVE_ELEGIDA_POR_USUARIO + slot)
            .apply()
    }

    /**
     * Inicialización / reparación de valores por defecto. Se llama en cada onResume():
     *  • Si la ranura fue elegida por la persona → no se toca.
     *  • Si el valor guardado ya está instalado → no se toca.
     *  • Si no, se elige el primer candidato instalado (así, si instalan SmartTube después,
     *    la tarjeta se completa sola) y se persiste.
     */
    fun resolverPorDefecto(pm: PackageManager) {
        val editor = sp.edit()
        for (slot in 0 until NUM_SLOTS) {
            if (sp.getBoolean(CLAVE_ELEGIDA_POR_USUARIO + slot, false)) continue
            val actual = sp.getString(CLAVE_FAV + slot, null)
            if (actual != null && AppUtils.estaInstalada(pm, actual)) continue
            val instalado = CANDIDATAS_POR_DEFECTO[slot].firstOrNull { AppUtils.estaInstalada(pm, it) }
            when {
                instalado != null -> editor.putString(CLAVE_FAV + slot, instalado)
                actual == null -> editor.putString(CLAVE_FAV + slot, CANDIDATAS_POR_DEFECTO[slot].first())
            }
        }
        editor.apply()
    }

    companion object {
        const val NUM_SLOTS = 4
        private const val ARCHIVO = "launcher_prefs"
        private const val CLAVE_FAV = "fav_slot_"
        private const val CLAVE_ELEGIDA_POR_USUARIO = "fav_user_set_"

        /**
         * Candidatos por ranura, en orden de prioridad:
         *  0 = Kino TV · 1 = SmartTube · 2 = Settings · 3 = Media Player
         *
         * IMPORTANTE: los paquetes de SmartTube, Settings (según marca) y Media Player son
         * "mejor esfuerzo". Verifica los reales de tu equipo con:
         *     adb shell pm list packages | grep -i -E "smarttube|teamsmart|liskovsoft|settings|vlc|mxtech"
         * o simplemente elígelos con el botón central sostenido.
         */
        val CANDIDATAS_POR_DEFECTO: List<List<String>> = listOf(
            listOf("com.arkiv.player.light"),
            listOf(
                "com.teamsmart.videomanager.tv",       // SmartTube estable
                "com.liskovsoft.smarttubetv.beta",     // SmartTube beta
                "com.liskovsoft.smarttubetv"
            ),
            listOf(
                "com.android.tv.settings",             // Android TV / Google TV
                "com.amazon.tv.settings.v2",           // Fire OS
                "com.amazon.tv.settings",
                "com.android.settings"
            ),
            listOf(
                "org.videolan.vlc",                    // VLC
                "com.mxtech.videoplayer.ad",           // MX Player
                "com.mxtech.videoplayer.pro",
                "com.google.android.videos",
                "com.amazon.avod"
            )
        )
    }
}
