package com.woli.app.focus

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 알림·통화 직후 STT를 켤지 여부. 기본 집중 화면에서는 false. */
object FocusVoiceInterruptState {
  private val _listeningRequested = MutableStateFlow(false)
  val listeningRequested: StateFlow<Boolean> = _listeningRequested.asStateFlow()

  fun requestListening() {
    _listeningRequested.value = true
  }

  fun stopListening() {
    _listeningRequested.value = false
  }
}
