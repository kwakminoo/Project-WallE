package com.woli.app.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertFalse(status.sessionActive)
    }

    @Test
    fun encodeStatusRoundTrips() {
        val original = WoliDeviceHardwareStatus(
            mounted = true,
            locked = false,
            handNear = true,
            batteryPercent = 55,
            sessionActive = true,
        )

        val parsed = WoliDeviceProtocol.parseStatus(WoliDeviceProtocol.encodeStatus(original))

        assertTrue(parsed.mounted)
        assertFalse(parsed.locked)
        assertTrue(parsed.handNear)
        assertEquals(55, parsed.batteryPercent)
        assertTrue(parsed.sessionActive)
    }

    @Test
    fun calibrationCommandsAreClamped() {
        assertEquals("CAL_LOCK=180", WoliDeviceProtocol.commandCalibrateLock(300))
        assertEquals("CAL_UNLOCK=0", WoliDeviceProtocol.commandCalibrateUnlock(-20))
    }

    @Test
    fun parseStatusSupportsSessionAndBooleanValues() {
        val status = WoliDeviceProtocol.parseStatus(
            "mount=true;lock=false;hand=yes;battery=82;session=true",
        )

        assertTrue(status.mounted)
        assertFalse(status.locked)
        assertTrue(status.handNear)
        assertEquals(82, status.batteryPercent)
        assertTrue(status.sessionActive)
    }

    @Test
    fun unknownAndInvalidFieldsDoNotBreakStatusParsing() {
        val status = WoliDeviceProtocol.parseStatus("hand=1;session=1;future=value;battery=not-a-number;broken")

        assertFalse(status.mounted)
        assertFalse(status.locked)
        assertTrue(status.handNear)
        assertTrue(status.sessionActive)
        assertNull(status.batteryPercent)
    }

    @Test
    fun batteryIsClampedToProtocolRange() {
        assertEquals(0, WoliDeviceProtocol.parseStatus("battery=-10").batteryPercent)
        assertEquals(100, WoliDeviceProtocol.parseStatus("battery=170").batteryPercent)
    }

    @Test
    fun handApproachCommandsMatchFirmwareProtocol() {
        assertEquals("HAND_NEAR", WoliDeviceProtocol.COMMAND_HAND_NEAR)
        assertEquals("HAND_FAR", WoliDeviceProtocol.COMMAND_HAND_FAR)
    }

    @Test
    fun sessionEndCommandMatchesFirmwareProtocol() {
        assertEquals("SESSION_END", WoliDeviceProtocol.COMMAND_SESSION_END)
    }
}
