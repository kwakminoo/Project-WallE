package com.woli.app.focus

import android.content.Context
import com.woli.app.device.WoliDeviceProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

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

    /** 집중 모드가 켜진 구간(시작~종료, 계획 종료 시각 상한). */
    fun focusedElapsedMillis(): Long {
        val endMillis = completedAtMillis ?: return 0L
        return max(0L, min(endMillis, endsAtMillis) - startedAtMillis)
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

fun WoliFocusExitReason?.isEarlyUnlock(): Boolean {
    return this == WoliFocusExitReason.MissionUnlocked || this == WoliFocusExitReason.UserQuit
}

data class WoliDailyFocusStat(
    val day: LocalDate,
    val dayLabel: String,
    val focusMinutes: Int,
    val handWarnings: Int,
    val earlyUnlocks: Int,
)

fun List<WoliFocusSession>.dailyFocusStats(
    days: Int = 7,
    zone: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zone),
): List<WoliDailyFocusStat> {
    val dayRange = (days - 1 downTo 0).map { today.minusDays(it.toLong()) }
    val labelFormat = DateTimeFormatter.ofPattern("M/d")
    val buckets = dayRange.associateWith { day ->
        WoliDailyFocusStat(day, day.format(labelFormat), 0, 0, 0)
    }.toMutableMap()

    for (session in this) {
        val millis = session.completedAtMillis ?: session.startedAtMillis
        val day = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        val bucket = buckets[day] ?: continue
        buckets[day] = bucket.copy(
            focusMinutes = bucket.focusMinutes + (session.focusedElapsedMillis() / 60_000L).toInt(),
            handWarnings = bucket.handWarnings + session.handWarningCount,
            earlyUnlocks = bucket.earlyUnlocks + if (session.exitReason.isEarlyUnlock()) 1 else 0,
        )
    }

    return dayRange.map { buckets.getValue(it) }
}

/** ponytail: shell 통계 화면용; 실제 세션 기록이 생기면 StatsScreen이 실데이터를 우선한다. */
object WoliFocusSessionDemos {
    fun sampleHistory(
        zone: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zone),
    ): List<WoliFocusSession> {
        fun at(dayOffset: Long, hour: Int, minute: Int = 0): Long {
            return today.minusDays(dayOffset)
                .atTime(hour, minute)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
        }

        fun session(
            id: String,
            startedAtMillis: Long,
            plannedMinutes: Int,
            focusedMinutes: Int,
            handWarnings: Int,
            exitReason: WoliFocusExitReason,
        ): WoliFocusSession {
            val focusedMillis = focusedMinutes * 60_000L
            return WoliFocusSession(
                id = id,
                config = WoliFocusSessionConfig(durationMinutes = plannedMinutes),
                startedAtMillis = startedAtMillis,
                endsAtMillis = startedAtMillis + plannedMinutes * 60_000L,
                completedAtMillis = startedAtMillis + focusedMillis,
                exitReason = exitReason,
                handWarningCount = handWarnings,
            )
        }

        return listOf(
            session("demo_d6_am", at(6, 9, 30), plannedMinutes = 30, focusedMinutes = 28, handWarnings = 1, exitReason = WoliFocusExitReason.Completed),
            session("demo_d5_a", at(5, 13, 0), plannedMinutes = 50, focusedMinutes = 50, handWarnings = 2, exitReason = WoliFocusExitReason.Completed),
            session("demo_d5_b", at(5, 20, 15), plannedMinutes = 25, focusedMinutes = 12, handWarnings = 1, exitReason = WoliFocusExitReason.MissionUnlocked),
            session("demo_d3_long", at(3, 10, 0), plannedMinutes = 90, focusedMinutes = 90, handWarnings = 0, exitReason = WoliFocusExitReason.Completed),
            session("demo_d2", at(2, 19, 0), plannedMinutes = 45, focusedMinutes = 35, handWarnings = 3, exitReason = WoliFocusExitReason.UserQuit),
            session("demo_d1_a", at(1, 8, 0), plannedMinutes = 60, focusedMinutes = 60, handWarnings = 2, exitReason = WoliFocusExitReason.Completed),
            session("demo_d1_b", at(1, 21, 30), plannedMinutes = 50, focusedMinutes = 50, handWarnings = 4, exitReason = WoliFocusExitReason.Completed),
            session("demo_today", at(0, 15, 45), plannedMinutes = 40, focusedMinutes = 38, handWarnings = 1, exitReason = WoliFocusExitReason.Completed),
        )
    }
}

object WoliFocusSessionController {
    private const val MAX_HISTORY = 20

    private val _config = MutableStateFlow(WoliFocusSessionConfig())
    private val _current = MutableStateFlow<WoliFocusSession?>(null)
    private val _history = MutableStateFlow<List<WoliFocusSession>>(emptyList())
    private val _pendingHardwareCommand = MutableStateFlow<String?>(null)
    private val lock = Any()

    @Volatile
    private var persistenceContext: Context? = null

    @Volatile
    private var loaded = false

    val config: StateFlow<WoliFocusSessionConfig> = _config.asStateFlow()
    val current: StateFlow<WoliFocusSession?> = _current.asStateFlow()
    val history: StateFlow<List<WoliFocusSession>> = _history.asStateFlow()
    val pendingHardwareCommand: StateFlow<String?> = _pendingHardwareCommand.asStateFlow()

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
            _pendingHardwareCommand.value = if (expiredSession != null) {
                WoliDeviceProtocol.COMMAND_SESSION_END
            } else if (activeSession != null) {
                WoliDeviceProtocol.COMMAND_START
            } else {
                snapshot.pendingHardwareCommand.takeIf(::isFocusHardwareCommand)
            }
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

    /** UI에서 집중 시작을 다시 눌렀을 때 ESP32 전송을 재시도할 수 있게 pending을 갱신한다. */
    fun queueHardwareCommand(command: String) {
        synchronized(lock) {
            if (!isFocusHardwareCommand(command)) return
            _pendingHardwareCommand.value = command
            persistLocked()
        }
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
            _pendingHardwareCommand.value = WoliDeviceProtocol.COMMAND_START
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
            return completeLocked(current, reason, nowMillis)
        }
    }

    /** Completes only a live session so competing timer paths cannot finish it twice. */
    fun completeIfActive(
        reason: WoliFocusExitReason = WoliFocusExitReason.Completed,
        nowMillis: Long = System.currentTimeMillis(),
    ): WoliFocusSession? {
        synchronized(lock) {
            val current = _current.value ?: return null
            return completeLocked(current, reason, nowMillis)
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

    /** Records one already-edge-detected hand approach only while a live session remains. */
    fun addHandWarningIfActive(nowMillis: Long = System.currentTimeMillis()): Boolean {
        synchronized(lock) {
            val current = _current.value ?: return false
            if (current.remainingMillis(nowMillis) <= 0L) return false
            _current.value = current.copy(handWarningCount = current.handWarningCount + 1)
            persistLocked()
            return true
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

    fun markHardwareCommandDelivered(command: String) {
        synchronized(lock) {
            if (_pendingHardwareCommand.value == command) {
                _pendingHardwareCommand.value = null
                persistLocked()
            }
        }
    }

    fun resetForTest() {
        synchronized(lock) {
            _config.value = WoliFocusSessionConfig()
            _current.value = null
            _history.value = emptyList()
            _pendingHardwareCommand.value = null
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
            pendingHardwareCommand = _pendingHardwareCommand.value,
        )
    }

    private fun completeLocked(
        current: WoliFocusSession,
        reason: WoliFocusExitReason,
        nowMillis: Long,
    ): WoliFocusSession {
        val completed = current.copy(
            completedAtMillis = nowMillis,
            exitReason = reason,
        )
        _current.value = null
        _pendingHardwareCommand.value = when (reason) {
            WoliFocusExitReason.Completed -> WoliDeviceProtocol.COMMAND_SESSION_END
            WoliFocusExitReason.MissionUnlocked,
            WoliFocusExitReason.UserQuit -> WoliDeviceProtocol.COMMAND_STOP
        }
        _history.update { previous ->
            (listOf(completed) + previous.filterNot { it.id == completed.id }).take(MAX_HISTORY)
        }
        persistLocked()
        return completed
    }

    private fun isFocusHardwareCommand(command: String?): Boolean {
        return command == WoliDeviceProtocol.COMMAND_START ||
            command == WoliDeviceProtocol.COMMAND_SESSION_END ||
            command == WoliDeviceProtocol.COMMAND_STOP
    }
}

/** Small edge gate: one warning for each false-to-true hand transition. */
class WoliHandWarningEdgeGate(initialHandNear: Boolean) {
    private var armed = !initialHandNear

    fun onHandStatus(handNear: Boolean): Boolean {
        if (!handNear) {
            armed = true
            return false
        }
        if (!armed) return false
        armed = false
        return true
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
