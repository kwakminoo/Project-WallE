package com.woli.app

import com.woli.app.navigation.FocusSessionNav
import com.woli.app.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            Routes.FOCUS_NOTIFICATION_PERMISSION,
            Routes.NOTIFICATION_DIAGNOSTICS,
            Routes.HARDWARE_DIAGNOSTICS,
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

    @Test
    fun focusSessionEndClearsEyesFromBackStack() {
        assertEquals(Routes.HOME, FocusSessionNav.POP_UP_TO_ON_SESSION_END)
        assertFalse(FocusSessionNav.POP_INCLUSIVE_ON_SESSION_END)

        val stackAfterSessionEnd = listOf(Routes.HOME, Routes.FOCUS_COMPLETE)
        assertFalse(FocusSessionNav.leavesFocusEyesOnBackStack(stackAfterSessionEnd))

        val buggyStack = listOf(Routes.HOME, Routes.FOCUS_EYES, Routes.FOCUS_COMPLETE)
        assertTrue(FocusSessionNav.leavesFocusEyesOnBackStack(buggyStack))
    }

    @Test
    fun forceHomeAfterSessionExitWhenRestoredToFocus() {
        assertTrue(
            FocusSessionNav.shouldForceHomeAfterSessionExit(true, Routes.FOCUS_EYES),
        )
        assertTrue(
            FocusSessionNav.shouldForceHomeAfterSessionExit(true, Routes.RHYTHM_MISSION),
        )
        assertTrue(
            FocusSessionNav.shouldForceHomeAfterSessionExit(true, Routes.FOCUS_COMPLETE),
        )
        assertFalse(
            FocusSessionNav.shouldForceHomeAfterSessionExit(true, Routes.HOME),
        )
        assertFalse(
            FocusSessionNav.shouldForceHomeAfterSessionExit(false, Routes.FOCUS_EYES),
        )
    }

    @Test
    fun landscapeRoutesMatchFocusSessionSurfaces() {
        assertTrue(FocusSessionNav.isLandscapeRoute(Routes.FOCUS_EYES))
        assertTrue(FocusSessionNav.isLandscapeRoute(Routes.QUIT_CONFIRM))
        assertFalse(FocusSessionNav.isLandscapeRoute(Routes.HOME))
        assertFalse(FocusSessionNav.isLandscapeRoute(Routes.FOCUS_TIME))
    }

    @Test
    fun normalCompletionRedirectsOnlyInProgressFocusRoutes() {
        assertTrue(FocusSessionNav.shouldRedirectToNormalCompletion(Routes.FOCUS_EYES))
        assertTrue(FocusSessionNav.shouldRedirectToNormalCompletion(Routes.HAND_WARNING))
        assertTrue(FocusSessionNav.shouldRedirectToNormalCompletion(Routes.RHYTHM_MISSION))
        assertFalse(FocusSessionNav.shouldRedirectToNormalCompletion(Routes.FOCUS_COMPLETE))
        assertFalse(FocusSessionNav.shouldRedirectToNormalCompletion(Routes.SESSION_REPORT))
        assertFalse(FocusSessionNav.shouldRedirectToNormalCompletion(Routes.HOME))
    }
}
