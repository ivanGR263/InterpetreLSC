package com.cesmag.nexing.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class SpeechManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isReady = false
    
    private var lastSpokenText: String? = null
    private var lastSpokenTime: Long = 0L
    private val MIN_TIME_BETWEEN_WORDS = 1500L // REQUERIMIENTO: Sincronizado a 1.5s

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
                tts?.setSpeechRate(1.0f) // Velocidad normal para 1.5s
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
        // REQUERIMIENTO: Si hay un error o no está listo, podríamos reportarlo
        if (!isReady) {
            Log.e("SpeechManager", "TTS no listo para: $text")
            return
        }
        
        if (text.isBlank() || isStatusMessage(text)) return

        val currentTime = System.currentTimeMillis()
        val timeElapsed = currentTime - lastSpokenTime
        
        // REQUERIMIENTO: Sincronización estricta de 1.5s
        if (text != lastSpokenText && timeElapsed >= MIN_TIME_BETWEEN_WORDS) {
            // Usamos QUEUE_FLUSH para que el audio actual sea exactamente lo que muestra el texto
            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "NexingSpeechID")
            
            if (result == TextToSpeech.ERROR) {
                Log.e("SpeechManager", "Error al ejecutar speak() para: $text")
            } else {
                lastSpokenText = text
                lastSpokenTime = currentTime
            }
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
