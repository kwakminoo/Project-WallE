package com.woli.app.voice

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class WoliTtsSpeaker(context: Context) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingText: String? = null
    private var pendingOnDone: (() -> Unit)? = null

    init {
        tts = TextToSpeech(appContext) { status ->
            ready = status == TextToSpeech.SUCCESS && configureLanguage()
            if (ready) {
                tts?.setOnUtteranceProgressListener(
                    object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) = Unit

                        override fun onDone(utteranceId: String?) {
                            pendingOnDone?.invoke()
                            pendingOnDone = null
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            pendingOnDone?.invoke()
                            pendingOnDone = null
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            pendingOnDone?.invoke()
                            pendingOnDone = null
                        }
                    },
                )
                pendingText?.let { text ->
                    speakNow(text, pendingOnDone)
                    pendingText = null
                    pendingOnDone = null
                }
            }
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        val announcement = text.trim()
        if (announcement.isBlank()) {
            onDone?.invoke()
            return
        }

        if (ready) {
            speakNow(announcement, onDone)
        } else {
            pendingText = announcement
            pendingOnDone = onDone
        }
    }

    fun stop() {
        tts?.stop()
        pendingText = null
        pendingOnDone?.invoke()
        pendingOnDone = null
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

    private fun speakNow(text: String, onDone: (() -> Unit)? = null) {
        pendingOnDone = onDone
        val params = Bundle()
        val utteranceId = "woli-${SystemClock.uptimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }
}
