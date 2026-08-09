package com.woli.app.notification

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import java.util.LinkedHashMap
import java.util.Locale

object WoliRemoteReplyActionStore {
    private const val MAX_HANDLES = 40

    private val lock = Any()
    private val handles = LinkedHashMap<String, ReplyActionHandle>(MAX_HANDLES, 0.75f, true)
    private val actionIdsByEventId = mutableMapOf<String, String>()

    fun registerReplyAction(
        eventId: String,
        notification: Notification,
    ): WoliReplyAction? {
        val candidate = selectReplyCandidate(notification.actions ?: return clear(eventId))
            ?: return clear(eventId)

        val actionId = "$eventId:${candidate.index}"
        val summary = WoliReplyAction(
            actionId = actionId,
            eventId = eventId,
            label = candidate.label,
            requiresUnlock = candidate.requiresUnlock,
        )

        synchronized(lock) {
            handles[actionId] = ReplyActionHandle(
                actionId = actionId,
                eventId = eventId,
                label = candidate.label,
                pendingIntent = candidate.pendingIntent,
                remoteInputs = candidate.remoteInputs,
            )
            actionIdsByEventId[eventId] = actionId
            pruneLocked()
        }

        return summary
    }

    fun sendReply(
        context: Context,
        eventId: String,
        replyText: String,
    ): WoliReplySendResult {
        val normalizedReply = replyText.trim()
        if (normalizedReply.isBlank()) return WoliReplySendResult.EmptyText

        val handle = synchronized(lock) {
            val actionId = actionIdsByEventId[eventId] ?: return WoliReplySendResult.NoReplyAction
            handles[actionId] ?: return WoliReplySendResult.Expired
        }

        return runCatching {
            val fillInIntent = Intent().addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            val results = Bundle().apply {
                handle.remoteInputs.forEach { remoteInput ->
                    putCharSequence(remoteInput.resultKey, normalizedReply)
                }
            }

            RemoteInput.addResultsToIntent(handle.remoteInputs, fillInIntent, results)
            RemoteInput.setResultsSource(fillInIntent, RemoteInput.SOURCE_FREE_FORM_INPUT)
            handle.pendingIntent.send(context.applicationContext, 0, fillInIntent)
            WoliReplySendResult.Sent
        }.getOrElse { throwable ->
            when (throwable) {
                is PendingIntent.CanceledException -> {
                    remove(handle.eventId)
                    WoliReplySendResult.Canceled
                }
                is SecurityException -> WoliReplySendResult.Failed(
                    "답장 권한을 실행하지 못했습니다. 알림 접근 권한을 다시 확인하세요.",
                )
                else -> WoliReplySendResult.Failed(
                    throwable.message?.takeIf { it.isNotBlank() }
                        ?: "답장 전송에 실패했습니다.",
                )
            }
        }
    }

    fun hasReplyAction(eventId: String): Boolean {
        return synchronized(lock) {
            actionIdsByEventId[eventId]?.let(handles::containsKey) == true
        }
    }

    fun remove(eventId: String) {
        synchronized(lock) {
            val actionId = actionIdsByEventId.remove(eventId)
            if (actionId != null) handles.remove(actionId)
        }
    }

    fun clearAll() {
        synchronized(lock) {
            handles.clear()
            actionIdsByEventId.clear()
        }
    }

    private fun clear(eventId: String): WoliReplyAction? {
        remove(eventId)
        return null
    }

    private fun selectReplyCandidate(
        actions: Array<Notification.Action>,
    ): ReplyCandidate? {
        return actions
            .mapIndexedNotNull { index, action ->
                val pendingIntent = action.actionIntent ?: return@mapIndexedNotNull null
                val remoteInputs = action.remoteInputs
                    ?.filter { it.allowFreeFormInput }
                    ?.toTypedArray()
                    ?: emptyArray<RemoteInput>()
                if (remoteInputs.isEmpty()) return@mapIndexedNotNull null

                ReplyCandidate(
                    index = index,
                    label = action.title?.toString()?.ifBlank { "답장" } ?: "답장",
                    pendingIntent = pendingIntent,
                    remoteInputs = remoteInputs,
                    semanticScore = if (action.semanticAction == Notification.Action.SEMANTIC_ACTION_REPLY) 2 else 0,
                    titleScore = if (looksLikeReplyAction(action.title?.toString())) 1 else 0,
                    requiresUnlock = action.isAuthenticationRequired,
                )
            }
            .maxWithOrNull(
                compareBy<ReplyCandidate> { it.semanticScore }
                    .thenBy { it.titleScore }
                    .thenByDescending { it.remoteInputs.size }
                    .thenByDescending { it.index },
            )
    }

    private fun looksLikeReplyAction(label: String?): Boolean {
        val normalized = label?.lowercase(Locale.ROOT).orEmpty()
        return listOf("reply", "respond", "답장", "응답", "보내기").any(normalized::contains)
    }

    private fun pruneLocked() {
        while (handles.size > MAX_HANDLES) {
            val eldest = handles.entries.iterator().next()
            handles.remove(eldest.key)
            actionIdsByEventId.remove(eldest.value.eventId)
        }
    }

    private data class ReplyCandidate(
        val index: Int,
        val label: String,
        val pendingIntent: PendingIntent,
        val remoteInputs: Array<RemoteInput>,
        val semanticScore: Int,
        val titleScore: Int,
        val requiresUnlock: Boolean,
    )

    private data class ReplyActionHandle(
        val actionId: String,
        val eventId: String,
        val label: String,
        val pendingIntent: PendingIntent,
        val remoteInputs: Array<RemoteInput>,
    )
}
