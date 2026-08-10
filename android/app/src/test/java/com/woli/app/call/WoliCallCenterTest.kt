package com.woli.app.call

import android.telephony.TelephonyManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WoliCallCenterTest {
    @Test
    fun telephonyStateMapsToCallState() {
        assertEquals(
            WoliCallState.Ringing,
            WoliCallStateMapper.fromTelephonyState(TelephonyManager.CALL_STATE_RINGING),
        )
        assertEquals(
            WoliCallState.Active,
            WoliCallStateMapper.fromTelephonyState(TelephonyManager.CALL_STATE_OFFHOOK),
        )
        assertEquals(
            WoliCallState.Idle,
            WoliCallStateMapper.fromTelephonyState(TelephonyManager.CALL_STATE_IDLE),
        )
        assertEquals(WoliCallState.Unknown, WoliCallStateMapper.fromTelephonyState(-1))
    }

    @Test
    fun ringingActiveIdleCycleIsStoredInHistory() {
        WoliCallCenter.clear()

        WoliCallCenter.updateState(WoliCallState.Ringing, nowMillis = 1_000L)
        assertEquals(WoliCallState.Ringing, WoliCallCenter.current.value?.state)

        WoliCallCenter.updateState(WoliCallState.Active, nowMillis = 2_000L)
        assertEquals(WoliCallState.Active, WoliCallCenter.current.value?.state)

        WoliCallCenter.updateState(WoliCallState.Idle, nowMillis = 3_000L)

        assertNull(WoliCallCenter.current.value)
        assertEquals(1, WoliCallCenter.history.value.size)
        assertEquals(WoliCallState.Idle, WoliCallCenter.history.value.first().state)
        assertEquals(1_000L, WoliCallCenter.history.value.first().startedAtMillis)
        assertEquals(3_000L, WoliCallCenter.history.value.first().updatedAtMillis)

        WoliCallCenter.clear()
    }

    @Test
    fun idleWithoutCurrentCallDoesNotCreateHistory() {
        WoliCallCenter.clear()

        WoliCallCenter.updateState(WoliCallState.Idle, nowMillis = 1_000L)

        assertNull(WoliCallCenter.current.value)
        assertEquals(emptyList<WoliCallEvent>(), WoliCallCenter.history.value)
    }
}
