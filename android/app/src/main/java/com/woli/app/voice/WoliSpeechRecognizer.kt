package com.woli.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class WoliSpeechRecognizer(context: Context) {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(appContext)
    }

    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (!isAvailable()) {
            onError("이 기기에서는 음성 인식을 사용할 수 없습니다.")
            return
        }

        destroyRecognizer()
        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit

                override fun onError(error: Int) {
                    onError(friendlyError(error))
                    destroyRecognizer()
                }

                override fun onResults(results: Bundle?) {
                    val spokenText = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.let(WoliReplyDraftPolicy::sanitizeReply)
                        .orEmpty()

                    if (spokenText.isBlank()) {
                        onError("답장 문장을 듣지 못했어요.")
                    } else {
                        onResult(spokenText)
                    }
                    destroyRecognizer()
                }
            })
        }

        recognizer?.startListening(recognitionIntent())
    }

    fun cancel() {
        recognizer?.cancel()
        destroyRecognizer()
    }

    fun shutdown() {
        destroyRecognizer()
    }

    private fun recognitionIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "월이에게 보낼 답장을 말해주세요.")
        }
    }

    private fun destroyRecognizer() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun friendlyError(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "마이크 입력을 처리하지 못했어요."
            SpeechRecognizer.ERROR_CLIENT -> "음성 인식을 시작하지 못했어요."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "마이크 권한이 필요합니다."
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 음성 인식 연결이 불안정합니다."
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "답장 문장을 듣지 못했어요."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "음성 인식이 이미 실행 중입니다."
            SpeechRecognizer.ERROR_SERVER -> "음성 인식 서버가 응답하지 않습니다."
            else -> "음성 인식에 실패했어요."
        }
    }
}
