package com.example.privacyclipboard

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EventBus {
    // OTIMIZAÇÃO: extraBufferCapacity aumentado para 128 para lidar melhor com bursts
    // no S25 Ultra e outros dispositivos de alta performance
    private val _events = MutableSharedFlow<Pair<String, String>>(
        replay = 0,
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<Pair<String, String>> = _events.asSharedFlow()

    suspend fun emitEvent(appName: String, type: String) {
        try {
            _events.emit(appName to type)
        } catch (e: Exception) {
            // Log silencioso para evitar crash se o flow estiver fechado
            android.util.Log.w("EventBus", "Falha ao emitir evento: ${e.message}")
        }
    }
}