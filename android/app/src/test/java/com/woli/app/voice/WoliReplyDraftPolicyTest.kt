package com.woli.app.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WoliReplyDraftPolicyTest {
    @Test
    fun sanitizeReplyNormalizesWhitespace() {
        assertEquals(
            "20분 뒤에 연락할게요.",
            WoliReplyDraftPolicy.sanitizeReply("  20분   뒤에\n연락할게요. "),
        )
    }

    @Test
    fun sanitizeReplyLimitsLongText() {
        val reply = WoliReplyDraftPolicy.sanitizeReply("답장".repeat(60), maxLength = 30)

        assertEquals(30, reply.length)
        assertTrue(reply.endsWith("…"))
    }
}
