package com.example.privacyclipboard

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EventBus {
    // extraBufferCapacity=64 garante que se 10 apps acessarem o clipboard
    // ao mesmo tempo, nenhum evento será perdido enquanto o Serviço processa o primeiro.
    private val _events = MutableSharedFlow<Pair<String, String>>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<Pair<String, String>> = _events.asSharedFlow()

    suspend fun emitEvent(appName: String, type: String) {
        _events.emit(appName to type)
    }
}