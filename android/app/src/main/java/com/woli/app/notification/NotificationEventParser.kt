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
        val ruleConfig = WoliNotificationRuleStore.configSnapshot(context)
        val notification = statusBarNotification.notification ?: return null
        val packageName = statusBarNotification.packageName
        val appName = appNameFor(packageName, packageManager)
        val extras = notification.extras
        val title = WoliNotificationPolicy.safePreview(
            extras.getCharSequence(Notification.EXTRA_TITLE)
                ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
                ?: appName,
            maxLength = 42,
        )
        val preview = WoliNotificationPolicy.safePreview(
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT),
        )
        if (title.isBlank() && preview.isBlank()) return null

        val eventId = "$packageName:${statusBarNotification.id}:${statusBarNotification.postTime}"
        val replyAction = WoliRemoteReplyActionStore.registerReplyAction(eventId, notification)
        val canReply = replyAction != null
        val importantContact = WoliImportantContactMatcher.matchText(
            text = "$title $preview",
            contacts = WoliImportantContactsStore.enabledContactsSnapshot(),
        )
        val importance = WoliNotificationPolicy.evaluate(
            WoliNotificationImportanceInput(
                category = notification.category,
                canReply = canReply,
                importantContactMatched = importantContact != null,
                title = title,
                preview = preview,
                appName = appName,
                packageName = packageName,
                isOngoing = notification.isOngoingStatus(),
                isGroupSummary = notification.isGroupSummary(),
                isGroupConversation = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false),
                systemPriority = notification.systemPriorityCompat(),
            ),
            config = ruleConfig,
        )

        return WoliNotificationEvent(
            id = eventId,
            packageName = packageName,
            appName = appName,
            title = title.ifBlank { appName },
            preview = preview.ifBlank { "내용 미리보기가 없는 알림입니다." },
            priority = importance.priority,
            canReply = canReply,
            postedAtMillis = statusBarNotification.postTime,
            replyAction = replyAction,
            importantContactLabel = importantContact?.displayName,
            importanceScore = importance.score,
            importanceReason = importance.reason,
        )
    }

    private fun appNameFor(packageName: String, packageManager: PackageManager): String {
        return runCatching {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(packageName)
    }

    private fun Notification.isOngoingStatus(): Boolean {
        return (flags and Notification.FLAG_ONGOING_EVENT) != 0 ||
            (flags and Notification.FLAG_FOREGROUND_SERVICE) != 0
    }

    private fun Notification.isGroupSummary(): Boolean {
        return (flags and Notification.FLAG_GROUP_SUMMARY) != 0
    }

    @Suppress("DEPRECATION")
    private fun Notification.systemPriorityCompat(): Int {
        return priority
    }
}
