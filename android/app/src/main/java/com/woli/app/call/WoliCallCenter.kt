package com.woli.app.call

import com.woli.app.contacts.WoliPhoneNumberNormalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object WoliCallCenter {
    private const val MAX_HISTORY = 20

    private val _current = MutableStateFlow<WoliCallEvent?>(null)
    private val _history = MutableStateFlow<List<WoliCallEvent>>(emptyList())

    val current: StateFlow<WoliCallEvent?> = _current.asStateFlow()
    val history: StateFlow<List<WoliCallEvent>> = _history.asStateFlow()

    fun updateState(
        state: WoliCallState,
        nowMillis: Long = System.currentTimeMillis(),
        callerInfo: WoliCallerInfo? = null,
    ) {
        when (state) {
            WoliCallState.Ringing -> startOrUpdateCurrent(WoliCallState.Ringing, nowMillis, callerInfo)
            WoliCallState.Active -> startOrUpdateCurrent(WoliCallState.Active, nowMillis, callerInfo)
            WoliCallState.Idle -> finishCurrent(nowMillis)
            WoliCallState.Unknown -> Unit
        }
    }

    fun clear() {
        _current.value = null
        _history.value = emptyList()
    }

    private fun startOrUpdateCurrent(
        state: WoliCallState,
        nowMillis: Long,
        callerInfo: WoliCallerInfo?,
    ) {
        val current = _current.value
        _current.value = if (current == null || current.state == WoliCallState.Idle) {
            WoliCallEvent(
                id = "${nowMillis}_${state.name}",
                state = state,
                callerLabel = callerInfo?.label ?: WoliCallEvent.UNKNOWN_CALLER_LABEL,
                startedAtMillis = nowMillis,
                updatedAtMillis = nowMillis,
                callerNumber = callerInfo?.phoneNumber,
                isImportant = callerInfo?.isImportant ?: false,
            )
        } else {
            current.copy(
                state = state,
                updatedAtMillis = nowMillis,
                callerLabel = callerInfo?.label ?: current.callerLabel,
                callerNumber = callerInfo?.phoneNumber ?: current.callerNumber,
                normalizedCallerNumber = callerInfo?.phoneNumber?.let(WoliPhoneNumberNormalizer::normalize)
                    ?: current.normalizedCallerNumber,
                isImportant = callerInfo?.isImportant ?: current.isImportant,
            )
        }
    }

    private fun finishCurrent(nowMillis: Long) {
        val current = _current.value ?: return
        val finished = current.copy(
            state = WoliCallState.Idle,
            updatedAtMillis = nowMillis,
        )
        _current.value = null
        _history.update { currentHistory ->
            (listOf(finished) + currentHistory.filterNot { it.id == finished.id })
                .take(MAX_HISTORY)
        }
    }
}
