package com.example.domain.engine

import com.example.data.model.CrossModuleEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AetherEventBus {
    private val _events = MutableStateFlow<List<CrossModuleEvent>>(emptyList())
    val events: StateFlow<List<CrossModuleEvent>> = _events.asStateFlow()

    fun publish(
        sourceModule: String,
        targetModule: String,
        eventType: String,
        summary: String,
        severity: String = "INFO"
    ) {
        val event = CrossModuleEvent(
            id = UUID.randomUUID().toString().take(10),
            sourceModule = sourceModule,
            targetModule = targetModule,
            eventType = eventType,
            summary = summary,
            severity = severity
        )
        _events.value = (listOf(event) + _events.value).take(60)
    }
}
