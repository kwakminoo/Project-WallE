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
        val removedIds = mutableListOf<String>()
        _events.update { current ->
            val next = (listOf(event) + current.filterNot { it.id == event.id }).take(MAX_EVENTS)
            removedIds.clear()
            current
                .map { it.id }
                .filterNot { id -> next.any { it.id == id } }
                .forEach(removedIds::add)
            next
        }
        removedIds.forEach(WoliRemoteReplyActionStore::remove)
    }

    fun clear() {
        _events.value = emptyList()
        WoliRemoteReplyActionStore.clearAll()
    }
}
