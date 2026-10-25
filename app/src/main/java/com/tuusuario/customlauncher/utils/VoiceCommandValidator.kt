package com.tuusuario.customlauncher.utils

import android.util.Log

/** Frase de activación literal; NO es firma criptográfica ni autenticación. */
object VoiceCommandValidator {
    private const val TAG = "VoiceCommandValidator"
    private val suffix = Regex("(?i)\\bdexter$")

    fun validateAndCleanCommand(rawInput: String?): String? {
        if (rawInput.isNullOrBlank()) return null
        val trimmed = rawInput.trim()
        if (!suffix.containsMatchIn(trimmed)) {
            Log.w(TAG, "Comando rechazado: falta el sufijo de activación")
            return null
        }
        val cleaned = trimmed.replace(suffix, "").trim()
        if (cleaned.isBlank()) return null
        Log.i(TAG, "Comando válido")
        return cleaned
    }
}
