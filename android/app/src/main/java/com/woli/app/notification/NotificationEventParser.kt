package com.woli.app.notification

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.StatusBarNotification
import com.woli.app.contacts.WoliImportantContactMatcher
import com.woli.app.contacts.WoliImportantContactsStore

object NotificationEventParser {
    fun parse(
        context: Context,
        statusBarNotification: StatusBarNotification,
        packageManager: PackageManager,
    ): WoliNotificationEvent? {
        WoliImportantContactsStore.load(context)
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
        if (title.isBlank() && preview.isBlank()) return null

        val eventId = "${statusBarNotification.packageName}:${statusBarNotification.id}:${statusBarNotification.postTime}"
        val replyAction = WoliRemoteReplyActionStore.registerReplyAction(eventId, notification)
        val canReply = replyAction != null
        val importantContact = WoliImportantContactMatcher.matchText(
            text = "$title $preview",
            contacts = WoliImportantContactsStore.enabledContactsSnapshot(),
        )

        return WoliNotificationEvent(
            id = eventId,
            packageName = statusBarNotification.packageName,
            appName = appNameFor(statusBarNotification.packageName, packageManager),
            title = title.ifBlank { appNameFor(statusBarNotification.packageName, packageManager) },
            preview = preview.ifBlank { "내용 미리보기가 없는 알림입니다." },
            priority = WoliNotificationPolicy.priorityFor(
                category = notification.category,
                canReply = canReply,
                importantContactMatched = importantContact != null,
            ),
            canReply = canReply,
            postedAtMillis = statusBarNotification.postTime,
            replyAction = replyAction,
            importantContactLabel = importantContact?.displayName,
        )
    }

    private fun appNameFor(packageName: String, packageManager: PackageManager): String {
        return runCatching {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(packageName)
    }
}
