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
    val replyAction: WoliReplyAction? = null,
    val importantContactLabel: String? = null,
    val importanceScore: Int = 0,
    val importanceReason: WoliNotificationImportanceReason = WoliNotificationImportanceReason.Default,
)

enum class WoliNotificationPriority {
    Critical,
    Important,
    Normal,
}
