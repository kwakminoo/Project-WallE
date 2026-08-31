package com.woli.app.focus

import android.Manifest
import android.content.Context
import com.woli.app.call.WoliCallAccess
import com.woli.app.call.WoliCallActionController
import com.woli.app.contacts.WoliContactsAccess
import com.woli.app.notification.WoliNotificationAccess

enum class FocusNotificationPermissionItem {
    NotificationAccess,
    PostNotifications,
    BatteryOptimization,
    PhoneBundle,
    Contacts,
}

data class FocusNotificationPermissionState(
    val notificationAccess: Boolean,
    val postNotifications: Boolean,
    val batteryOptimizationIgnored: Boolean,
    val phoneState: Boolean,
    val contacts: Boolean,
    val callerId: Boolean,
    val callControl: Boolean,
) {
    val allGranted: Boolean
        get() = notificationAccess &&
            postNotifications &&
            batteryOptimizationIgnored &&
            phoneState &&
            contacts &&
            callerId &&
            callControl

    val needsPhoneRuntimePermissions: Boolean
        get() = !phoneState || !callerId || !callControl

    val missingItems: List<FocusNotificationPermissionItem>
        get() = buildList {
            if (!notificationAccess) add(FocusNotificationPermissionItem.NotificationAccess)
            if (!postNotifications) add(FocusNotificationPermissionItem.PostNotifications)
            if (!batteryOptimizationIgnored) add(FocusNotificationPermissionItem.BatteryOptimization)
            if (needsPhoneRuntimePermissions) add(FocusNotificationPermissionItem.PhoneBundle)
            if (!contacts) add(FocusNotificationPermissionItem.Contacts)
        }
}

object WoliFocusNotificationPermissions {
    fun read(context: Context): FocusNotificationPermissionState {
        return FocusNotificationPermissionState(
            notificationAccess = WoliNotificationAccess.isEnabled(context),
            postNotifications = WoliFocusGuardAccess.canPostNotifications(context),
            batteryOptimizationIgnored = WoliFocusGuardAccess.isIgnoringBatteryOptimizations(context),
            phoneState = WoliCallAccess.isGranted(context),
            contacts = WoliContactsAccess.canReadContacts(context),
            callerId = WoliCallAccess.canReadCallerId(context),
            callControl = WoliCallActionController.canControlCalls(context),
        )
    }

    fun allGranted(context: Context): Boolean = read(context).allGranted

    fun phoneRuntimePermissions(): Array<String> = arrayOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.ANSWER_PHONE_CALLS,
    )
}
