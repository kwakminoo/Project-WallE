package com.woli.app.voice

import com.woli.app.notification.WoliNotificationEvent
import com.woli.app.notification.WoliNotificationPriority

object WoliAnnouncementFormatter {
    fun notificationAnnouncement(event: WoliNotificationEvent): String? {
        if (event.priority == WoliNotificationPriority.Normal) return null

        val sender = event.title.ifBlank { event.appName }
        val preview = event.preview
            .takeIf { it.isNotBlank() && it != "내용 미리보기가 없는 알림입니다." }
            ?.let { "내용은, ${it}." }
            .orEmpty()
        val replyHint = if (event.canReply) {
            "답장 가능한 알림입니다."
        } else {
            "읽기 전용 알림입니다."
        }

        return when (event.priority) {
            WoliNotificationPriority.Critical ->
                "${sender}에게 긴급 알림이 왔어요. ${preview} ${replyHint}".trim()
            WoliNotificationPriority.Important ->
                "${sender}에게 중요한 알림이 왔어요. ${preview} ${replyHint}".trim()
            WoliNotificationPriority.Normal -> null
        }
    }
}
