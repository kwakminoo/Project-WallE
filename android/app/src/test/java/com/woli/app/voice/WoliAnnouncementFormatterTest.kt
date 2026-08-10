package com.woli.app.voice

import com.woli.app.call.WoliCallEvent
import com.woli.app.call.WoliCallState
import com.woli.app.notification.WoliNotificationEvent
import com.woli.app.notification.WoliNotificationPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WoliAnnouncementFormatterTest {
    @Test
    fun importantNotificationIsSpokenWithReplyHint() {
        val announcement = WoliAnnouncementFormatter.notificationAnnouncement(
            sampleEvent(priority = WoliNotificationPriority.Important, canReply = true),
        )

        assertEquals(
            "어머니에게 중요한 알림이 왔어요. 내용은, 언제 끝나?. 답장 가능한 알림입니다.",
            announcement,
        )
    }

    @Test
    fun criticalNotificationUsesUrgentWording() {
        val announcement = WoliAnnouncementFormatter.notificationAnnouncement(
            sampleEvent(priority = WoliNotificationPriority.Critical, canReply = false),
        )

        assertTrue(announcement?.contains("긴급 알림") == true)
        assertTrue(announcement?.contains("읽기 전용") == true)
    }

    @Test
    fun normalNotificationIsNotSpoken() {
        assertNull(
            WoliAnnouncementFormatter.notificationAnnouncement(
                sampleEvent(priority = WoliNotificationPriority.Normal, canReply = false),
            ),
        )
    }

    @Test
    fun ringingCallIsSpokenWithoutSensitiveNumber() {
        val announcement = WoliAnnouncementFormatter.callAnnouncement(
            WoliCallEvent(
                id = "call-1",
                state = WoliCallState.Ringing,
                callerLabel = WoliCallEvent.UNKNOWN_CALLER_LABEL,
                startedAtMillis = 0L,
                updatedAtMillis = 0L,
            ),
        )

        assertEquals(
            "전화가 왔어요. 휴대폰 화면에서 받을 수 있습니다.",
            announcement,
        )
    }

    @Test
    fun activeCallIsNotSpokenOverConversation() {
        assertNull(
            WoliAnnouncementFormatter.callAnnouncement(
                WoliCallEvent(
                    id = "call-1",
                    state = WoliCallState.Active,
                    callerLabel = WoliCallEvent.UNKNOWN_CALLER_LABEL,
                    startedAtMillis = 0L,
                    updatedAtMillis = 0L,
                ),
            ),
        )
    }

    private fun sampleEvent(
        priority: WoliNotificationPriority,
        canReply: Boolean,
    ): WoliNotificationEvent {
        return WoliNotificationEvent(
            id = "event-1",
            packageName = "com.example.chat",
            appName = "채팅",
            title = "어머니",
            preview = "언제 끝나?",
            priority = priority,
            canReply = canReply,
            postedAtMillis = 0L,
        )
    }
}
