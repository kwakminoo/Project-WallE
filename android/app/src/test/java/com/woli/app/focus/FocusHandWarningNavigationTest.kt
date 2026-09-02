package com.woli.app.focus

import org.junit.Assert.assertEquals
import org.junit.Test

class FocusHandWarningNavigationTest {
    @Test
    fun handNearReturnWaitsTwoSeconds() {
        assertEquals(2_000L, handWarningReturnDelayMs(HandWarningCause.HandNear))
    }

    @Test
    fun escapeReturnIsShorterThanHandNear() {
        assertEquals(900L, handWarningReturnDelayMs(HandWarningCause.EscapeAttempt))
    }

    @Test
    fun handApproachNavGraceIsFourSeconds() {
        assertEquals(4_000L, HAND_APPROACH_NAV_GRACE_MS)
    }
}
