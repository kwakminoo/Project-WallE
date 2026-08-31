package com.woli.app.device

import org.junit.Assert.assertTrue
import org.junit.Test

class WoliDeviceCenterTest {
    @Test
    fun disconnectKeepsLastRealHandStateAndSimulationControlsCannotOverwriteIt() {
        try {
            WoliDeviceCenter.connectSimulated()
            WoliDeviceCenter.setHandNear(true)
            WoliDeviceCenter.disconnect()

            WoliDeviceCenter.setHandNear(false)

            assertTrue(WoliDeviceCenter.state.value.isHandNear)
        } finally {
            WoliDeviceCenter.connectSimulated()
            WoliDeviceCenter.setHandNear(false)
            WoliDeviceCenter.disconnect()
        }
    }
}
