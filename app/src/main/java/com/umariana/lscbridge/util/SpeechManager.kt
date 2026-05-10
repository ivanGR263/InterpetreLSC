package com.umariana.lscbridge.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class SpeechManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isReady = false
    
    private var lastSpokenText: String? = null
    private var lastSpokenTime: Long = 0L
    private val MIN_TIME_BETWEEN_WORDS = 1500L // 1.5 segundos

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Intentar configurar Español (Colombia)
            var result = tts?.setLanguage(Locale("es", "CO"))
            
            // Si no está disponible, intentar Español genérico
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("SpeechManager", "Español (CO) no disponible, intentando Español genérico...")
                result = tts?.setLanguage(Locale("es"))
            }

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("SpeechManager", "El idioma español no está soportado en este dispositivo")
            } else {
                // Ajustar velocidad si es necesario (1.0 es normal)
                tts?.setSpeedRate(1.1f) 
                isReady = true
                Log.d("SpeechManager", "Motor TTS configurado correctamente en Español")
            }
        } else {
            Log.e("SpeechManager", "Fallo al inicializar el motor TTS")
        }
    }

    private fun TextToSpeech.setSpeedRate(rate: Float) {
        this.setSpeechRate(rate)
    }

    /**
     * Intenta decir el texto proporcionado respetando las reglas de repetición.
     */
    fun speakResult(text: String) {
        if (!isReady || text.isBlank() || text == "Buscando mano..." || text == "Identificando...") return

        val currentTime = System.currentTimeMillis()
        
        // REGLAS DE CONTROL:
        // 1. Que el texto sea diferente al anterior.
        // 2. O que haya pasado el tiempo mínimo de gracia.
        val shouldSpeak = text != lastSpokenText || (currentTime - lastSpokenTime) > MIN_TIME_BETWEEN_WORDS

        if (shouldSpeak) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "GestureSpeechID")
            lastSpokenText = text
            lastSpokenTime = currentTime
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
