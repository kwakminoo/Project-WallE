package com.woli.app.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WoliDeviceProtocolTest {
    @Test
    fun parseStatusPayloadSupportsExpectedKeys() {
        val status = WoliDeviceProtocol.parseStatus("mount=1;lock=true;hand=yes;battery=87")

        assertTrue(status.mounted)
        assertTrue(status.locked)
        assertTrue(status.handNear)
        assertEquals(87, status.batteryPercent)
    }

    @Test
    fun encodeStatusRoundTrips() {
        val original = WoliDeviceHardwareStatus(
            mounted = true,
            locked = false,
            handNear = true,
            batteryPercent = 55,
        )

        val parsed = WoliDeviceProtocol.parseStatus(WoliDeviceProtocol.encodeStatus(original))

        assertTrue(parsed.mounted)
        assertFalse(parsed.locked)
        assertTrue(parsed.handNear)
        assertEquals(55, parsed.batteryPercent)
    }

    @Test
    fun calibrationCommandsAreClamped() {
        assertEquals("CAL_LOCK=180", WoliDeviceProtocol.commandCalibrateLock(300))
        assertEquals("CAL_UNLOCK=0", WoliDeviceProtocol.commandCalibrateUnlock(-20))
    }
}
