package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val label: String = "Alarm",
    val daysOfWeek: String = "Every day" // e.g. "Mon, Tue, Wed" or "Once"
) {
    val formattedTime: String
        get() {
            val amPm = if (hour >= 12) "PM" else "AM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            return String.format("%02d:%02d %s", displayHour, minute, amPm)
        }
}
