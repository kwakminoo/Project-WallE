package com.woli.app.voice

data class WoliReplyDraft(
    val eventId: String,
    val replyText: String,
    val confirmed: Boolean = false,
)

object WoliReplyDraftPolicy {
    fun sanitizeReply(text: String, maxLength: Int = 80): String {
        val normalized = text
            .replace(Regex("\\s+"), " ")
            .trim()

        if (normalized.length <= maxLength) return normalized
        return normalized.take(maxLength - 1) + "…"
    }
}
