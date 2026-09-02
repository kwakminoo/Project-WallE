package com.woli.app.focus

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 집중 중 홈/탭/뒤로가기 등 이탈 시도 신호. 0이면 아직 없음. */
object FocusEscapeAttemptCenter {
    private val _nonce = MutableStateFlow(0L)
    val nonce: StateFlow<Long> = _nonce.asStateFlow()

    fun trigger() {
        if (FocusHandWarningState.entryCause != HandWarningCause.HandNear) {
            FocusHandWarningState.entryCause = HandWarningCause.EscapeAttempt
        }
        _nonce.value = System.currentTimeMillis()
    }
}
