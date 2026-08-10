package com.woli.app.notification

data class WoliReplyHistoryEntry(
    val id: String,
    val eventId: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val preview: String,
    val replyText: String,
    val resultType: WoliReplyHistoryResultType,
    val resultMessage: String,
    val createdAtMillis: Long,
)

enum class WoliReplyHistoryResultType {
    Sent,
    EmptyText,
    NoReplyAction,
    Expired,
    Canceled,
    Failed,
}
