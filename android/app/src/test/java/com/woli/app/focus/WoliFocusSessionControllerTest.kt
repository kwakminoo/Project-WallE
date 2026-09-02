package com.woli.app.focus

import com.woli.app.device.WoliDeviceProtocol
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun handWarningsOnlyApplyToAnActiveSessionWithTimeRemaining() {
        WoliFocusSessionController.resetForTest()
        assertFalse(WoliFocusSessionController.addHandWarningIfActive(nowMillis = 1_000L))

        WoliFocusSessionController.updateConfig(WoliFocusSessionConfig(durationMinutes = 5))
        WoliFocusSessionController.start(nowMillis = 1_000L)

        assertTrue(WoliFocusSessionController.addHandWarningIfActive(nowMillis = 1_000L))
        assertFalse(WoliFocusSessionController.addHandWarningIfActive(nowMillis = 301_000L))
        assertEquals(1, WoliFocusSessionController.current.value?.handWarningCount)
        WoliFocusSessionController.resetForTest()
    }

    @Test
    fun hardwareCommandTracksTheSessionUntilBleAcknowledgesIt() {
        WoliFocusSessionController.resetForTest()
        WoliFocusSessionController.start(nowMillis = 1_000L)
        assertEquals(
            WoliDeviceProtocol.COMMAND_START,
            WoliFocusSessionController.pendingHardwareCommand.value,
        )

        WoliFocusSessionController.markHardwareCommandDelivered(WoliDeviceProtocol.COMMAND_START)
        assertNull(WoliFocusSessionController.pendingHardwareCommand.value)

        WoliFocusSessionController.completeIfActive(
            reason = WoliFocusExitReason.Completed,
            nowMillis = 2_000L,
        )
        assertEquals(
            WoliDeviceProtocol.COMMAND_SESSION_END,
            WoliFocusSessionController.pendingHardwareCommand.value,
        )
        WoliFocusSessionController.resetForTest()
    }

    @Test
    fun focusedElapsedMillisUsesActualFocusWindow() {
        WoliFocusSessionController.resetForTest()
        WoliFocusSessionController.updateConfig(WoliFocusSessionConfig(durationMinutes = 90))
        WoliFocusSessionController.start(nowMillis = 1_000L)

        val earlyExit = WoliFocusSessionController.complete(
            reason = WoliFocusExitReason.MissionUnlocked,
            nowMillis = 1_800_000L,
        )
        assertEquals(29 * 60_000L + 59_000L, earlyExit?.focusedElapsedMillis())

        WoliFocusSessionController.resetForTest()
        WoliFocusSessionController.updateConfig(WoliFocusSessionConfig(durationMinutes = 5))
        WoliFocusSessionController.start(nowMillis = 10_000L)
        val completed = WoliFocusSessionController.complete(
            reason = WoliFocusExitReason.Completed,
            nowMillis = 310_500L,
        )
        assertEquals(5 * 60_000L, completed?.focusedElapsedMillis())
        WoliFocusSessionController.resetForTest()
    }

    @Test
    fun queueHardwareCommandRefreshesStartAfterDelivery() {
        WoliFocusSessionController.resetForTest()
        WoliFocusSessionController.start(nowMillis = 1_000L)
        WoliFocusSessionController.markHardwareCommandDelivered(WoliDeviceProtocol.COMMAND_START)
        assertNull(WoliFocusSessionController.pendingHardwareCommand.value)

        WoliFocusSessionController.queueHardwareCommand(WoliDeviceProtocol.COMMAND_START)
        assertEquals(
            WoliDeviceProtocol.COMMAND_START,
            WoliFocusSessionController.pendingHardwareCommand.value,
        )
        WoliFocusSessionController.resetForTest()
    }

    @Test
    fun dailyFocusStatsBucketsFocusWarningsAndEarlyUnlocksByDay() {
        val zone = ZoneId.of("Asia/Seoul")
        val today = LocalDate.of(2026, 9, 2)
        val history = listOf(
            WoliFocusSession(
                id = "a",
                config = WoliFocusSessionConfig(durationMinutes = 90),
                startedAtMillis = today.atStartOfDay(zone).toInstant().toEpochMilli(),
                endsAtMillis = today.atStartOfDay(zone).plusMinutes(90).toInstant().toEpochMilli(),
                completedAtMillis = today.atStartOfDay(zone).plusMinutes(30).toInstant().toEpochMilli(),
                exitReason = WoliFocusExitReason.MissionUnlocked,
                handWarningCount = 2,
            ),
            WoliFocusSession(
                id = "b",
                config = WoliFocusSessionConfig(durationMinutes = 30),
                startedAtMillis = today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
                endsAtMillis = today.minusDays(1).atStartOfDay(zone).plusMinutes(30).toInstant().toEpochMilli(),
                completedAtMillis = today.minusDays(1).atStartOfDay(zone).plusMinutes(20).toInstant().toEpochMilli(),
                exitReason = WoliFocusExitReason.Completed,
                handWarningCount = 1,
            ),
        )

        val stats = history.dailyFocusStats(days = 2, zone = zone, today = today)

        assertEquals(2, stats.size)
        assertEquals(30, stats.last().focusMinutes)
        assertEquals(2, stats.last().handWarnings)
        assertEquals(1, stats.last().earlyUnlocks)
        assertEquals(20, stats.first().focusMinutes)
        assertEquals(1, stats.first().handWarnings)
        assertEquals(0, stats.first().earlyUnlocks)
    }

    @Test
    fun handWarningEdgeGateCountsOnlyRearmedRisingEdges() {
        val gate = WoliHandWarningEdgeGate(initialHandNear = false)
        assertFalse(gate.onHandStatus(false))
        assertTrue(gate.onHandStatus(true))
        assertFalse(gate.onHandStatus(true))
        assertFalse(gate.onHandStatus(false))
        assertTrue(gate.onHandStatus(true))

        val reconnectWithHandAlreadyNear = WoliHandWarningEdgeGate(initialHandNear = true)
        assertFalse(reconnectWithHandAlreadyNear.onHandStatus(true))
    }
}
