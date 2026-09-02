package com.woli.app.focus

/** 세션 단위 TTS 중복 방지 — 화면/FGS가 나뉘어도 한 번만 읽는다. */
object FocusAnnouncementTracker {
  private var lastSpokenNotificationId: String? = null
  private var lastSpokenCallId: String? = null

  fun reset() {
    lastSpokenNotificationId = null
    lastSpokenCallId = null
  }

  fun shouldAnnounceNotification(eventId: String): Boolean =
    eventId != lastSpokenNotificationId

  fun markNotificationAnnounced(eventId: String) {
    lastSpokenNotificationId = eventId
  }

  /** 전화는 세션당 통화 ID로 한 번만 안내한다. */
  fun shouldAnnounceCall(callId: String): Boolean =
    callId != lastSpokenCallId

  fun markCallAnnounced(callId: String) {
    lastSpokenCallId = callId
  }
}
