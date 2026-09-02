package com.woli.app.focus

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusSessionRecoveryTest {
    @Test
    fun recoveryRespectsCooldown() {
        assertTrue(FocusSessionRecovery.shouldAttemptRecovery(1_000L, lastAtMs = 0L))
        assertFalse(FocusSessionRecovery.shouldAttemptRecovery(1_200L, lastAtMs = 1_000L))
        assertTrue(FocusSessionRecovery.shouldAttemptRecovery(1_400L, lastAtMs = 1_000L))
    }
}
