package com.woli.app.notification

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

object WoliReplyHistoryStore {
    private const val PREFS_NAME = "woli_reply_history"
    private const val KEY_ENTRIES_JSON = "entries_json"

    private val lock = Any()
    private val _entries = MutableStateFlow<List<WoliReplyHistoryEntry>>(emptyList())

    @Volatile
    private var loaded = false

    val entries: StateFlow<List<WoliReplyHistoryEntry>> = _entries.asStateFlow()

    fun load(context: Context) {
        synchronized(lock) {
            if (loaded) return

            _entries.value = readEntries(context.applicationContext)
            loaded = true
        }
    }

    fun record(
        context: Context,
        event: WoliNotificationEvent,
        replyText: String,
        result: WoliReplySendResult,
        createdAtMillis: Long = System.currentTimeMillis(),
    ): WoliReplyHistoryEntry {
        val appContext = context.applicationContext
        val entry = WoliReplyHistoryPolicy.createEntry(
            event = event,
            replyText = replyText,
            result = result,
            createdAtMillis = createdAtMillis,
        )

        synchronized(lock) {
            if (!loaded) {
                _entries.value = readEntries(appContext)
                loaded = true
            }

            val next = WoliReplyHistoryPolicy.mergeNewest(entry, _entries.value)
            _entries.value = next
            writeEntries(appContext, next)
        }

        return entry
    }

    fun clear(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            _entries.value = emptyList()
            loaded = true
            writeEntries(appContext, emptyList())
        }
    }

    private fun readEntries(context: Context): List<WoliReplyHistoryEntry> {
        val json = prefs(context).getString(KEY_ENTRIES_JSON, null).orEmpty()
        if (json.isBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        WoliReplyHistoryEntry(
                            id = item.optString("id"),
                            eventId = item.optString("eventId"),
                            packageName = item.optString("packageName"),
                            appName = item.optString("appName"),
                            title = item.optString("title"),
                            preview = item.optString("preview"),
                            replyText = item.optString("replyText"),
                            resultType = item.optString("resultType").toHistoryResultType(),
                            resultMessage = item.optString("resultMessage"),
                            createdAtMillis = item.optLong("createdAtMillis"),
                        ),
                    )
                }
            }.filter { it.id.isNotBlank() && it.eventId.isNotBlank() }
                .sortedByDescending { it.createdAtMillis }
                .take(WoliReplyHistoryPolicy.MAX_ENTRIES)
        }.getOrDefault(emptyList())
    }

    private fun writeEntries(context: Context, entries: List<WoliReplyHistoryEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("eventId", entry.eventId)
                    .put("packageName", entry.packageName)
                    .put("appName", entry.appName)
                    .put("title", entry.title)
                    .put("preview", entry.preview)
                    .put("replyText", entry.replyText)
                    .put("resultType", entry.resultType.name)
                    .put("resultMessage", entry.resultMessage)
                    .put("createdAtMillis", entry.createdAtMillis),
            )
        }

        prefs(context).edit()
            .putString(KEY_ENTRIES_JSON, array.toString())
            .apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun String.toHistoryResultType(): WoliReplyHistoryResultType {
        return runCatching { WoliReplyHistoryResultType.valueOf(this) }
            .getOrDefault(WoliReplyHistoryResultType.Failed)
    }
}
