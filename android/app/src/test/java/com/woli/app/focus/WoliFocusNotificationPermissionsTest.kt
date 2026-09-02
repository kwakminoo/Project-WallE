package com.woli.app.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WoliFocusNotificationPermissionsTest {
    @Test
    fun allGrantedRequiresEveryPermission() {
        val complete = FocusNotificationPermissionState(
            notificationAccess = true,
            postNotifications = true,
            batteryOptimizationIgnored = true,
            phoneState = true,
            contacts = true,
            callerId = true,
            callControl = true,
            camera = true,
        )
        assertTrue(complete.allGranted)
        assertTrue(complete.missingItems.isEmpty())
    }

    @Test
    fun missingItemsGroupsPhonePermissions() {
        val partialPhone = FocusNotificationPermissionState(
            notificationAccess = true,
            postNotifications = true,
            batteryOptimizationIgnored = true,
            phoneState = true,
            contacts = true,
            callerId = false,
            callControl = true,
            camera = true,
        )
        assertFalse(partialPhone.allGranted)
        assertEquals(
            listOf(FocusNotificationPermissionItem.PhoneBundle),
            partialPhone.missingItems,
        )
    }

    @Test
    fun missingItemsListsEveryPendingStep() {
        val pending = FocusNotificationPermissionState(
            notificationAccess = false,
            postNotifications = false,
            batteryOptimizationIgnored = false,
            phoneState = false,
            contacts = false,
            callerId = false,
            callControl = false,
            camera = false,
        )
        assertEquals(
            listOf(
                FocusNotificationPermissionItem.NotificationAccess,
                FocusNotificationPermissionItem.PostNotifications,
                FocusNotificationPermissionItem.BatteryOptimization,
                FocusNotificationPermissionItem.PhoneBundle,
                FocusNotificationPermissionItem.Contacts,
                FocusNotificationPermissionItem.Camera,
            ),
            pending.missingItems,
        )
    }
}
