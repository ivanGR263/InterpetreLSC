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
    private val MIN_TIME_BETWEEN_WORDS = 2500L // Aumentado a 2.5 segundos para evitar que se trabe

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
                // Velocidad ligeramente más lenta (0.9f) para mayor claridad si se traba
                tts?.setSpeechRate(0.95f) 
                isReady = true
                Log.d("SpeechManager", "Motor TTS configurado correctamente en Español")
            }
        } else {
            Log.e("SpeechManager", "Fallo al inicializar el motor TTS")
        }
    }

    /**
     * Intenta decir el texto proporcionado respetando las reglas de repetición.
     */
    fun speakResult(text: String) {
        if (!isReady || text.isBlank() || isStatusMessage(text)) return

        val currentTime = System.currentTimeMillis()
        val timeElapsed = currentTime - lastSpokenTime
        
        // NUEVA LÓGICA MÁS ESTRICTA:
        // Habla solo si:
        // 1. La letra es distinta a la última que se habló exitosamente.
        // 2. Y ADEMÁS, han pasado al menos 2.5 segundos desde que terminó de hablar.
        
        if (text != lastSpokenText && timeElapsed > MIN_TIME_BETWEEN_WORDS) {
            // QUEUE_FLUSH asegura que si hay algo pendiente, se borre antes de decir lo nuevo
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "LscSpeechID")
            lastSpokenText = text
            lastSpokenTime = currentTime
        }
    }

    private fun isStatusMessage(text: String): Boolean = 
        text == "Buscando mano..." || text == "Identificando..." || text.contains("Error")

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
