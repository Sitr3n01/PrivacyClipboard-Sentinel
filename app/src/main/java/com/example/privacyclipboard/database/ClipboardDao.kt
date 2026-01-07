package com.example.privacyclipboard.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Classe simples para segurar os dados do gráfico
data class AppStats(
    val appName: String,
    val accessCount: Int
)

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<ClipboardEvent>>

    // NOVA FUNÇÃO: Agrupa por nome, conta e ordena do maior para o menor
    @Query("SELECT appName, COUNT(id) as accessCount FROM clipboard_events GROUP BY appName ORDER BY accessCount DESC LIMIT 5")
    fun getTopApps(): Flow<List<AppStats>>

    @Insert
    suspend fun insert(event: ClipboardEvent)

    @Query("DELETE FROM clipboard_events")
    suspend fun clearAll()
}