package com.woli.app.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class WoliReplyHistoryPolicyTest {
    @Test
    fun createEntryKeepsNotificationContextAndTrimsReplyText() {
        val event = notificationEvent(id = "event-1")

        val entry = WoliReplyHistoryPolicy.createEntry(
            event = event,
            replyText = "  곧 연락할게  ",
            result = WoliReplySendResult.Sent,
            createdAtMillis = 1_000L,
        )

        assertEquals("1000_event-1", entry.id)
        assertEquals("event-1", entry.eventId)
        assertEquals("카카오톡", entry.appName)
        assertEquals("어머니", entry.title)
        assertEquals("곧 연락할게", entry.replyText)
        assertEquals(WoliReplyHistoryResultType.Sent, entry.resultType)
        assertEquals("답장 전송 요청을 완료했어요.", entry.resultMessage)
    }

    @Test
    fun mergeNewestSortsDescendingAndKeepsMaxEntries() {
        val current = (0 until WoliReplyHistoryPolicy.MAX_ENTRIES).map { index ->
            historyEntry(id = "old-$index", createdAtMillis = index.toLong())
        }
        val newest = historyEntry(id = "newest", createdAtMillis = 9_999L)

        val merged = WoliReplyHistoryPolicy.mergeNewest(newest, current)

        assertEquals(WoliReplyHistoryPolicy.MAX_ENTRIES, merged.size)
        assertEquals("newest", merged.first().id)
        assertEquals("old-1", merged.last().id)
    }

    @Test
    fun resultTypeOfMapsReplySendResult() {
        assertEquals(
            WoliReplyHistoryResultType.Sent,
            WoliReplyHistoryPolicy.resultTypeOf(WoliReplySendResult.Sent),
        )
        assertEquals(
            WoliReplyHistoryResultType.EmptyText,
            WoliReplyHistoryPolicy.resultTypeOf(WoliReplySendResult.EmptyText),
        )
        assertEquals(
            WoliReplyHistoryResultType.NoReplyAction,
            WoliReplyHistoryPolicy.resultTypeOf(WoliReplySendResult.NoReplyAction),
        )
        assertEquals(
            WoliReplyHistoryResultType.Expired,
            WoliReplyHistoryPolicy.resultTypeOf(WoliReplySendResult.Expired),
        )
        assertEquals(
            WoliReplyHistoryResultType.Canceled,
            WoliReplyHistoryPolicy.resultTypeOf(WoliReplySendResult.Canceled),
        )
        assertEquals(
            WoliReplyHistoryResultType.Failed,
            WoliReplyHistoryPolicy.resultTypeOf(WoliReplySendResult.Failed("실패")),
        )
    }

    private fun notificationEvent(id: String) = WoliNotificationEvent(
        id = id,
        packageName = "com.kakao.talk",
        appName = "카카오톡",
        title = "어머니",
        preview = "통화 가능해?",
        priority = WoliNotificationPriority.Important,
        canReply = true,
        postedAtMillis = 500L,
    )

    private fun historyEntry(
        id: String,
        createdAtMillis: Long,
    ) = WoliReplyHistoryEntry(
        id = id,
        eventId = id,
        packageName = "com.example",
        appName = "테스트앱",
        title = "제목",
        preview = "내용",
        replyText = "답장",
        resultType = WoliReplyHistoryResultType.Sent,
        resultMessage = "성공",
        createdAtMillis = createdAtMillis,
    )
}
