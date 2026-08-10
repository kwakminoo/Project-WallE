package com.woli.app.notification

object WoliReplyHistoryPolicy {
    const val MAX_ENTRIES = 50

    fun createEntry(
        event: WoliNotificationEvent,
        replyText: String,
        result: WoliReplySendResult,
        createdAtMillis: Long,
    ): WoliReplyHistoryEntry {
        return WoliReplyHistoryEntry(
            id = "${createdAtMillis}_${event.id}",
            eventId = event.id,
            packageName = event.packageName,
            appName = event.appName,
            title = event.title,
            preview = event.preview,
            replyText = replyText.trim(),
            resultType = resultTypeOf(result),
            resultMessage = result.userMessage(),
            createdAtMillis = createdAtMillis,
        )
    }

    fun mergeNewest(
        entry: WoliReplyHistoryEntry,
        current: List<WoliReplyHistoryEntry>,
    ): List<WoliReplyHistoryEntry> {
        return (listOf(entry) + current.filterNot { it.id == entry.id })
            .sortedByDescending { it.createdAtMillis }
            .take(MAX_ENTRIES)
    }

    fun resultTypeOf(result: WoliReplySendResult): WoliReplyHistoryResultType {
        return when (result) {
            WoliReplySendResult.Sent -> WoliReplyHistoryResultType.Sent
            WoliReplySendResult.EmptyText -> WoliReplyHistoryResultType.EmptyText
            WoliReplySendResult.NoReplyAction -> WoliReplyHistoryResultType.NoReplyAction
            WoliReplySendResult.Expired -> WoliReplyHistoryResultType.Expired
            WoliReplySendResult.Canceled -> WoliReplyHistoryResultType.Canceled
            is WoliReplySendResult.Failed -> WoliReplyHistoryResultType.Failed
        }
    }
}
