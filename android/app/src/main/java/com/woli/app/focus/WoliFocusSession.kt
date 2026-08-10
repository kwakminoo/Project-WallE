package com.woli.app.focus

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max

data class WoliFocusSessionConfig(
    val durationMinutes: Int = 90,
    val allowImportantOnly: Boolean = true,
    val breakNotify: Boolean = false,
)

data class WoliFocusSession(
    val id: String,
    val config: WoliFocusSessionConfig,
    val startedAtMillis: Long,
    val endsAtMillis: Long,
    val completedAtMillis: Long? = null,
    val exitReason: WoliFocusExitReason? = null,
    val handWarningCount: Int = 0,
    val missionAttemptCount: Int = 0,
    val missionSuccessCount: Int = 0,
) {
    val isActive: Boolean
        get() = completedAtMillis == null

    fun elapsedMillis(nowMillis: Long): Long {
        val end = completedAtMillis ?: nowMillis
        return max(0L, end - startedAtMillis)
    }

    fun remainingMillis(nowMillis: Long): Long {
        if (!isActive) return 0L
        return max(0L, endsAtMillis - nowMillis)
    }
}

enum class WoliFocusExitReason {
    Completed,
    MissionUnlocked,
    UserQuit,
}

object WoliFocusSessionController {
    private const val MAX_HISTORY = 20

    private val _config = MutableStateFlow(WoliFocusSessionConfig())
    private val _current = MutableStateFlow<WoliFocusSession?>(null)
    private val _history = MutableStateFlow<List<WoliFocusSession>>(emptyList())
    private val lock = Any()

    @Volatile
    private var persistenceContext: Context? = null

    @Volatile
    private var loaded = false

    val config: StateFlow<WoliFocusSessionConfig> = _config.asStateFlow()
    val current: StateFlow<WoliFocusSession?> = _current.asStateFlow()
    val history: StateFlow<List<WoliFocusSession>> = _history.asStateFlow()

    fun load(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            persistenceContext = appContext
            if (loaded) return

            val snapshot = WoliFocusSessionStore.load(appContext)
            val nowMillis = System.currentTimeMillis()
            val activeSession = snapshot.current?.takeIf { it.isActive && it.endsAtMillis > nowMillis }
            val expiredSession = snapshot.current
                ?.takeIf { it.isActive && it.endsAtMillis <= nowMillis }
                ?.let { session ->
                    session.copy(
                        completedAtMillis = session.endsAtMillis,
                        exitReason = WoliFocusExitReason.Completed,
                    )
                }
            val nextHistory = (
                listOfNotNull(expiredSession) +
                    snapshot.history.filterNot { session -> session.id == expiredSession?.id }
                )
                .sortedByDescending { session -> session.completedAtMillis ?: session.startedAtMillis }
                .take(MAX_HISTORY)

            _config.value = snapshot.config
            _current.value = activeSession
            _history.value = nextHistory
            loaded = true
            persistLocked()
        }
    }

    fun updateConfig(config: WoliFocusSessionConfig) {
        synchronized(lock) {
            _config.value = config.copy(durationMinutes = config.durationMinutes.coerceIn(5, 300))
            persistLocked()
        }
    }

    fun startIfNeeded(nowMillis: Long = System.currentTimeMillis()): WoliFocusSession {
        return _current.value?.takeIf { it.isActive } ?: start(nowMillis)
    }

    fun start(nowMillis: Long = System.currentTimeMillis()): WoliFocusSession {
        synchronized(lock) {
            val activeConfig = _config.value
            val session = WoliFocusSession(
                id = "focus_$nowMillis",
                config = activeConfig,
                startedAtMillis = nowMillis,
                endsAtMillis = nowMillis + activeConfig.durationMinutes * 60_000L,
            )
            _current.value = session
            persistLocked()
            return session
        }
    }

    fun complete(
        reason: WoliFocusExitReason = WoliFocusExitReason.Completed,
        nowMillis: Long = System.currentTimeMillis(),
    ): WoliFocusSession? {
        synchronized(lock) {
            val current = _current.value ?: return _history.value.firstOrNull()
            val completed = current.copy(
                completedAtMillis = nowMillis,
                exitReason = reason,
            )
            _current.value = null
            _history.update { previous ->
                (listOf(completed) + previous.filterNot { it.id == completed.id }).take(MAX_HISTORY)
            }
            persistLocked()
            return completed
        }
    }

    fun addHandWarning() {
        synchronized(lock) {
            _current.update { current ->
                current?.copy(handWarningCount = current.handWarningCount + 1)
            }
            persistLocked()
        }
    }

    fun recordMissionAttempt(success: Boolean) {
        synchronized(lock) {
            _current.update { current ->
                current?.copy(
                    missionAttemptCount = current.missionAttemptCount + 1,
                    missionSuccessCount = current.missionSuccessCount + if (success) 1 else 0,
                )
            }
            persistLocked()
        }
    }

    fun latestCompleted(): WoliFocusSession? = _history.value.firstOrNull()

    fun resetForTest() {
        synchronized(lock) {
            _config.value = WoliFocusSessionConfig()
            _current.value = null
            _history.value = emptyList()
            persistenceContext = null
            loaded = false
        }
    }

    private fun persistLocked() {
        val context = persistenceContext ?: return
        WoliFocusSessionStore.save(
            context = context,
            config = _config.value,
            current = _current.value,
            history = _history.value,
        )
    }
}

fun Long.formatDurationClock(): String {
    val totalSeconds = max(0L, this / 1_000L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

fun Long.formatDurationKorean(): String {
    val totalMinutes = max(0L, this / 60_000L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분"
        hours > 0 -> "${hours}시간"
        else -> "${minutes}분"
    }
}
