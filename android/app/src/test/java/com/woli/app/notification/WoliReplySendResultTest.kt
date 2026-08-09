package com.woli.app.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WoliReplySendResultTest {
    @Test
    fun sentResultIsSuccess() {
        assertTrue(WoliReplySendResult.Sent.isSuccess)
        assertEquals(
            "답장 전송 요청을 완료했어요.",
            WoliReplySendResult.Sent.userMessage(),
        )
    }

    @Test
    fun failedResultCarriesUserMessage() {
        val result = WoliReplySendResult.Failed("테스트 실패")

        assertFalse(result.isSuccess)
        assertEquals("테스트 실패", result.userMessage())
    }

    @Test
    fun expiredResultExplainsRetryPath() {
        assertEquals(
            "답장 액션이 만료되었습니다. 새 알림에서 다시 시도하세요.",
            WoliReplySendResult.Expired.userMessage(),
        )
    }
}
