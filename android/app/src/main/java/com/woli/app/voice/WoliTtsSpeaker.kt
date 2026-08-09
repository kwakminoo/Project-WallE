package com.woli.app.voice

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import java.util.Locale

class WoliTtsSpeaker(context: Context) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingText: String? = null

    init {
        tts = TextToSpeech(appContext) { status ->
            ready = status == TextToSpeech.SUCCESS && configureLanguage()
            if (ready) {
                pendingText?.let(::speakNow)
                pendingText = null
            }
        }
    }

    fun speak(text: String) {
        val announcement = text.trim()
        if (announcement.isBlank()) return

        if (ready) {
            speakNow(announcement)
        } else {
            pendingText = announcement
        }
    }

    fun stop() {
        tts?.stop()
        pendingText = null
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        ready = false
    }

    private fun configureLanguage(): Boolean {
        val result = tts?.setLanguage(Locale.KOREAN) ?: TextToSpeech.LANG_NOT_SUPPORTED
        return result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    private fun speakNow(text: String) {
        val params = Bundle()
        val utteranceId = "woli-${SystemClock.uptimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }
}
