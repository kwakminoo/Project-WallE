package com.woli.app

import com.woli.app.navigation.Routes
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellSmokeCheckTest {
    @Test
    fun routesAreDefined() {
        val routes = listOf(
            Routes.HOME,
            Routes.FOCUS_TIME,
            Routes.DEVICE_CONNECT,
            Routes.IMPORTANT_CONTACTS,
            Routes.MOUNT_GUIDE,
            Routes.FOCUS_EYES,
            Routes.REMAINING_TIME,
            Routes.IMPORTANT_CALL,
            Routes.HAND_WARNING,
            Routes.FOCUS_COMPLETE,
            Routes.QUIT_CONFIRM,
            Routes.RHYTHM_MISSION,
            Routes.SESSION_REPORT,
        )
        assertTrue(ShellSmokeCheck.assertRoutesNonEmpty(routes))
    }
}
