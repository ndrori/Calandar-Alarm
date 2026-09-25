package com.example.calendareventalarm.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import com.example.calendareventalarm.model.AlarmStatus
import com.example.calendareventalarm.model.CalendarEvent
import java.util.Calendar

class CalendarScanner(private val context: Context) {

    companion object {
        private const val TAG = "CalendarScanner"
        const val LEAD_TIME_15M = 15 * 60 * 1000L
        const val LEAD_TIME_10M = 10 * 60 * 1000L
        const val LEAD_TIME_5M = 5 * 60 * 1000L
        const val LEAD_TIME_0M = 0L

        /**
         * Calculates the next upcoming alarm trigger time based on 5-minute step thresholds (15m, 10m, 5m, 0m).
         * Selects the longest future lead-time step that has not yet passed.
         */
        fun calculateNextAlarmTime(startTime: Long, now: Long = System.currentTimeMillis()): Long {
            val steps = listOf(
                LEAD_TIME_15M,
                LEAD_TIME_10M,
                LEAD_TIME_5M,
                LEAD_TIME_0M
            )

            for (step in steps) {
                val targetAlarmTime = startTime - step
                // Return the first step whose alarm time is in the future (or triggering right now)
                if (targetAlarmTime > now - 5_000L) {
                    return targetAlarmTime
                }
            }

            // Fallback for past events
            return startTime - LEAD_TIME_15M
        }
    }

    /**
     * Scans device calendar for events occurring within the specified window (default: next 48 hours).
     * Calculates 5-minute stepped alarm times for each event based on current lead time.
     */
    fun scanUpcomingEvents(windowHours: Int = 48): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val contentResolver = context.contentResolver

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.HOUR_OF_DAY, windowHours)
        }
        val endTime = calendar.timeInMillis

        val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now - (60 * 60 * 1000L)) // Include events starting in last hour to capture active ones
        ContentUris.appendId(builder, endTime)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY
        )

        val selection = "${CalendarContract.Instances.ALL_DAY} = 0"
        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                builder.build(),
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.let {
                val idIdx = it.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleIdx = it.getColumnIndex(CalendarContract.Instances.TITLE)
                val descIdx = it.getColumnIndex(CalendarContract.Instances.DESCRIPTION)
                val locIdx = it.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                val beginIdx = it.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = it.getColumnIndex(CalendarContract.Instances.END)

                while (it.moveToNext()) {
                    val id = it.getLong(idIdx)
                    val title = it.getString(titleIdx) ?: "Untitled Meeting"
                    val desc = if (descIdx >= 0) it.getString(descIdx) else null
                    val loc = if (locIdx >= 0) it.getString(locIdx) else null
                    val start = it.getLong(beginIdx)
                    val end = it.getLong(endIdx)

                    val alarmTime = calculateNextAlarmTime(start, now)

                    val event = CalendarEvent(
                        eventId = id,
                        title = title,
                        description = desc,
                        location = loc,
                        startTime = start,
                        endTime = end,
                        alarmTime = alarmTime,
                        status = AlarmStatus.SCHEDULED
                    )
                    events.add(event)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Calendar permission not granted", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying calendar", e)
        } finally {
            cursor?.close()
        }

        Log.d(TAG, "Scanned ${events.size} upcoming calendar events")
        return events
    }
}
