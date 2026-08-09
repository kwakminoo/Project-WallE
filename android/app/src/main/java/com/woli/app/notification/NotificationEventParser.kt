package com.woli.app.notification

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.StatusBarNotification

object NotificationEventParser {
    fun parse(
        statusBarNotification: StatusBarNotification,
        packageManager: PackageManager,
    ): WoliNotificationEvent? {
        val notification = statusBarNotification.notification ?: return null
        val extras = notification.extras
        val title = WoliNotificationPolicy.safePreview(
            extras.getCharSequence(Notification.EXTRA_TITLE)
                ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
                ?: appNameFor(statusBarNotification.packageName, packageManager),
            maxLength = 42,
        )
        val preview = WoliNotificationPolicy.safePreview(
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT),
        )
        val canReply = notification.actions
            ?.any { action -> !action.remoteInputs.isNullOrEmpty() }
            ?: false

        if (title.isBlank() && preview.isBlank()) return null

        return WoliNotificationEvent(
            id = "${statusBarNotification.packageName}:${statusBarNotification.id}:${statusBarNotification.postTime}",
            packageName = statusBarNotification.packageName,
            appName = appNameFor(statusBarNotification.packageName, packageManager),
            title = title.ifBlank { appNameFor(statusBarNotification.packageName, packageManager) },
            preview = preview.ifBlank { "내용 미리보기가 없는 알림입니다." },
            priority = WoliNotificationPolicy.priorityFor(notification.category, canReply),
            canReply = canReply,
            postedAtMillis = statusBarNotification.postTime,
        )
    }

    private fun appNameFor(packageName: String, packageManager: PackageManager): String {
        return runCatching {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(packageName)
    }
}
