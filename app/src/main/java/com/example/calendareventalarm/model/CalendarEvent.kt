package com.example.calendareventalarm.model

import java.io.Serializable

enum class AlarmStatus {
    SCHEDULED,
    SNOOZED,
    DISMISSED,
    FIRED
}

data class CalendarEvent(
    val eventId: Long,
    val title: String,
    val description: String?,
    val location: String?,
    val startTime: Long,
    val endTime: Long,
    val alarmTime: Long, // 15 mins prior to startTime (or snoozed time)
    val status: AlarmStatus = AlarmStatus.SCHEDULED,
    val calendarName: String? = null
) : Serializable {
    /**
     * Calculates time remaining until event start in milliseconds.
     */
    fun getTimeUntilStart(): Long = startTime - System.currentTimeMillis()

    /**
     * Calculates time remaining until alarm trigger in milliseconds.
     */
    fun getTimeUntilAlarm(): Long = alarmTime - System.currentTimeMillis()
}
