package com.example.privacyclipboard.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "clipboard_events")
data class ClipboardEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val contentType: String = "Texto"
) {
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}