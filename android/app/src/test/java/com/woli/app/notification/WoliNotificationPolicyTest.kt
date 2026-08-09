package com.woli.app.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class WoliNotificationPolicyTest {
    @Test
    fun messageCategoryIsImportant() {
        assertEquals(
            WoliNotificationPriority.Important,
            WoliNotificationPolicy.priorityFor(category = "msg", canReply = false),
        )
    }

    @Test
    fun replyActionMakesNotificationImportant() {
        assertEquals(
            WoliNotificationPriority.Important,
            WoliNotificationPolicy.priorityFor(category = null, canReply = true),
        )
    }

    @Test
    fun safePreviewNormalizesWhitespaceAndLimitsLength() {
        val preview = WoliNotificationPolicy.safePreview(
            "  집중   중에\n읽을 긴 메시지입니다. ".repeat(6),
            maxLength = 24,
        )

        assertEquals(24, preview.length)
        assertEquals(true, preview.endsWith("…"))
    }
}
