package com.woli.app.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WoliFocusSessionControllerTest {
    @Test
    fun sessionUsesConfiguredDuration() {
        WoliFocusSessionController.resetForTest()
        WoliFocusSessionController.updateConfig(WoliFocusSessionConfig(durationMinutes = 25))

        val session = WoliFocusSessionController.start(nowMillis = 1_000L)

        assertEquals(25, session.config.durationMinutes)
        assertEquals(25 * 60_000L, session.remainingMillis(1_000L))
        WoliFocusSessionController.resetForTest()
    }

    @Test
    fun completeMovesCurrentSessionToHistory() {
        WoliFocusSessionController.resetForTest()
        WoliFocusSessionController.start(nowMillis = 1_000L)
        WoliFocusSessionController.addHandWarning()

        val completed = WoliFocusSessionController.complete(
            reason = WoliFocusExitReason.Completed,
            nowMillis = 61_000L,
        )

        assertFalse(completed?.isActive ?: true)
        assertEquals(1, completed?.handWarningCount)
        assertEquals(1, WoliFocusSessionController.history.value.size)
        WoliFocusSessionController.resetForTest()
    }

    @Test
    fun missionAttemptTracksSuccessCount() {
        WoliFocusSessionController.resetForTest()
        WoliFocusSessionController.start(nowMillis = 1_000L)

        WoliFocusSessionController.recordMissionAttempt(success = false)
        WoliFocusSessionController.recordMissionAttempt(success = true)

        val current = WoliFocusSessionController.current.value
        assertEquals(2, current?.missionAttemptCount)
        assertEquals(1, current?.missionSuccessCount)
        assertTrue(65_000L.formatDurationClock().startsWith("00:01"))
        WoliFocusSessionController.resetForTest()
    }
}
