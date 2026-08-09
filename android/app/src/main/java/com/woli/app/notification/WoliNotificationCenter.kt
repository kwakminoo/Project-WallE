package com.woli.app.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object WoliNotificationCenter {
    private const val MAX_EVENTS = 20

    private val _events = MutableStateFlow<List<WoliNotificationEvent>>(emptyList())
    val events: StateFlow<List<WoliNotificationEvent>> = _events.asStateFlow()

    fun add(event: WoliNotificationEvent) {
        _events.update { current ->
            (listOf(event) + current.filterNot { it.id == event.id }).take(MAX_EVENTS)
        }
    }

    fun clear() {
        _events.value = emptyList()
    }
}
