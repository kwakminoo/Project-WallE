package com.woli.app.focus

import android.content.Context
import android.media.AudioManager

/** 집중 세션 중 벨소리를 진동으로, 종료 시 이전 모드로 복원한다. */
object FocusSessionAudioMode {
  private var previousRingerMode: Int? = null

  fun enterVibrate(context: Context) {
    val audioManager = context.applicationContext.getSystemService(AudioManager::class.java) ?: return
    if (previousRingerMode == null) {
      previousRingerMode = audioManager.ringerMode
    }
    if (audioManager.ringerMode != AudioManager.RINGER_MODE_VIBRATE) {
      audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
    }
  }

  fun restore(context: Context) {
    val saved = previousRingerMode ?: return
    val audioManager = context.applicationContext.getSystemService(AudioManager::class.java) ?: return
    runCatching { audioManager.ringerMode = saved }
    previousRingerMode = null
  }
}
