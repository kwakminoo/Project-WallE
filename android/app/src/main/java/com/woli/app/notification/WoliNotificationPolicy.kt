package com.woli.app.notification

object WoliNotificationPolicy {
    private const val CATEGORY_CALL = "call"
    private const val CATEGORY_MESSAGE = "msg"

    fun priorityFor(category: String?, canReply: Boolean): WoliNotificationPriority {
        return when {
            category == CATEGORY_CALL -> WoliNotificationPriority.Critical
            category == CATEGORY_MESSAGE || canReply -> WoliNotificationPriority.Important
            else -> WoliNotificationPriority.Normal
        }
    }

    fun safePreview(text: CharSequence?, maxLength: Int = 90): String {
        val normalized = text
            ?.toString()
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()

        if (normalized.length <= maxLength) return normalized
        return normalized.take(maxLength - 1) + "…"
    }
}
