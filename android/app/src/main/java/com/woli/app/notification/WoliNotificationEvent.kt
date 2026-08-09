package com.woli.app.notification

data class WoliNotificationEvent(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val preview: String,
    val priority: WoliNotificationPriority,
    val canReply: Boolean,
    val postedAtMillis: Long,
)

enum class WoliNotificationPriority {
    Critical,
    Important,
    Normal,
}
