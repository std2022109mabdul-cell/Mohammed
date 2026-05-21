package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val epochMillis: Long,
    val alertOffsetMinutes: Int, // 0 = exact time, 5 = 5m before, 15 = 15m before, etc.
    val alertType: String,       // "VOICE", "TTS", "VIBRATE", "SILENT"
    val soundTone: String,       // "DEFAULT", "DIGITAL_BEEP", "SOFT_BELL", "CHIPTUNE"
    val isCompleted: Boolean = false,
    val isTriggered: Boolean = false
)
