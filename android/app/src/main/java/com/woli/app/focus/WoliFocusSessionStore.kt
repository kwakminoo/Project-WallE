package com.woli.app.focus

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class WoliFocusSessionSnapshot(
    val config: WoliFocusSessionConfig = WoliFocusSessionConfig(),
    val current: WoliFocusSession? = null,
    val history: List<WoliFocusSession> = emptyList(),
    val pendingHardwareCommand: String? = null,
)

object WoliFocusSessionStore {
    private const val PREFS_NAME = "woli_focus_sessions"
    private const val KEY_SNAPSHOT_JSON = "snapshot_json"

    fun load(context: Context): WoliFocusSessionSnapshot {
        val json = prefs(context).getString(KEY_SNAPSHOT_JSON, null).orEmpty()
        if (json.isBlank()) return WoliFocusSessionSnapshot()

        return runCatching {
            val root = JSONObject(json)
            WoliFocusSessionSnapshot(
                config = root.optJSONObject("config").toConfig(),
                current = root.optJSONObject("current")?.toSession(),
                history = root.optJSONArray("history").toSessionList(),
                pendingHardwareCommand = root.optNullableString("pendingHardwareCommand"),
            )
        }.getOrDefault(WoliFocusSessionSnapshot())
    }

    fun save(
        context: Context,
        config: WoliFocusSessionConfig,
        current: WoliFocusSession?,
        history: List<WoliFocusSession>,
        pendingHardwareCommand: String?,
    ) {
        val root = JSONObject()
            .put("config", config.toJson())
            .put("history", history.toJsonArray())
            .put("pendingHardwareCommand", pendingHardwareCommand ?: JSONObject.NULL)

        if (current != null) {
            root.put("current", current.toJson())
        } else {
            root.put("current", JSONObject.NULL)
        }

        prefs(context).edit()
            .putString(KEY_SNAPSHOT_JSON, root.toString())
            .apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun WoliFocusSessionConfig.toJson(): JSONObject {
        return JSONObject()
            .put("durationMinutes", durationMinutes)
            .put("allowImportantOnly", allowImportantOnly)
            .put("breakNotify", breakNotify)
    }

    private fun JSONObject?.toConfig(): WoliFocusSessionConfig {
        if (this == null) return WoliFocusSessionConfig()
        return WoliFocusSessionConfig(
            durationMinutes = optInt("durationMinutes", 90).coerceIn(5, 300),
            allowImportantOnly = optBoolean("allowImportantOnly", true),
            breakNotify = optBoolean("breakNotify", false),
        )
    }

    private fun WoliFocusSession.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("config", config.toJson())
            .put("startedAtMillis", startedAtMillis)
            .put("endsAtMillis", endsAtMillis)
            .put("completedAtMillis", completedAtMillis ?: JSONObject.NULL)
            .put("exitReason", exitReason?.name ?: JSONObject.NULL)
            .put("handWarningCount", handWarningCount)
            .put("missionAttemptCount", missionAttemptCount)
            .put("missionSuccessCount", missionSuccessCount)
    }

    private fun JSONObject.toSession(): WoliFocusSession? {
        val id = optString("id").takeIf { it.isNotBlank() } ?: return null
        val startedAtMillis = optLong("startedAtMillis", 0L)
        val endsAtMillis = optLong("endsAtMillis", 0L)
        if (startedAtMillis <= 0L || endsAtMillis <= startedAtMillis) return null

        return WoliFocusSession(
            id = id,
            config = optJSONObject("config").toConfig(),
            startedAtMillis = startedAtMillis,
            endsAtMillis = endsAtMillis,
            completedAtMillis = optNullableLong("completedAtMillis"),
            exitReason = optNullableString("exitReason")?.toExitReason(),
            handWarningCount = optInt("handWarningCount", 0).coerceAtLeast(0),
            missionAttemptCount = optInt("missionAttemptCount", 0).coerceAtLeast(0),
            missionSuccessCount = optInt("missionSuccessCount", 0).coerceAtLeast(0),
        )
    }

    private fun JSONArray?.toSessionList(): List<WoliFocusSession> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optJSONObject(index)?.toSession()?.let(::add)
            }
        }.sortedByDescending { it.completedAtMillis ?: it.startedAtMillis }
    }

    private fun List<WoliFocusSession>.toJsonArray(): JSONArray {
        val array = JSONArray()
        forEach { session -> array.put(session.toJson()) }
        return array
    }

    private fun JSONObject.optNullableLong(key: String): Long? {
        return if (isNull(key)) null else optLong(key)
    }

    private fun JSONObject.optNullableString(key: String): String? {
        return if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
    }

    private fun String.toExitReason(): WoliFocusExitReason? {
        return runCatching { WoliFocusExitReason.valueOf(this) }.getOrNull()
    }
}
