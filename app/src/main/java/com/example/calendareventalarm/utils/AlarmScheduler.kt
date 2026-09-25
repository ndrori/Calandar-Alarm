package com.example.calendareventalarm.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.calendareventalarm.model.CalendarEvent
import com.example.calendareventalarm.receiver.AlarmReceiver
import com.example.calendareventalarm.ui.MainActivity

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "AlarmScheduler"
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_EVENT_TITLE = "extra_event_title"
        const val EXTRA_EVENT_START_TIME = "extra_event_start_time"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"
    }

    /**
     * Schedules a stepped lead time alarm for a given calendar event.
     * If the target alarm time is right now (within 10 seconds), triggers immediately.
     */
    fun scheduleEventAlarm(event: CalendarEvent) {
        val now = System.currentTimeMillis()
        var triggerTime = event.alarmTime

        if (triggerTime <= now - 10_000L) {
            Log.d(TAG, "Alarm time for event '${event.title}' has already passed. Skipping.")
            return
        } else if (triggerTime <= now) {
            // Trigger immediately if scheduled right on the boundary
            triggerTime = now + 500L
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            setAction(AlarmReceiver.ACTION_TRIGGER_ALARM)
            putExtra(EXTRA_EVENT_ID, event.eventId)
            putExtra(EXTRA_EVENT_TITLE, event.title)
            putExtra(EXTRA_EVENT_START_TIME, event.startTime)
            putExtra(EXTRA_IS_SNOOZE, false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.eventId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarm(triggerTime, pendingIntent)
        Log.i(TAG, "Scheduled alarm for event '${event.title}' at ${java.util.Date(triggerTime)}")
    }

    /**
     * Schedules a 5-minute snooze alarm for an event.
     */
    fun scheduleSnoozeAlarm(eventId: Long, title: String, startTime: Long, delayMillis: Long = 5 * 60 * 1000L) {
        val triggerTime = System.currentTimeMillis() + delayMillis

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            setAction(AlarmReceiver.ACTION_TRIGGER_ALARM)
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_EVENT_TITLE, title)
            putExtra(EXTRA_EVENT_START_TIME, startTime)
            putExtra(EXTRA_IS_SNOOZE, true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarm(triggerTime, pendingIntent)
        Log.i(TAG, "Snoozed alarm for event '$title' ($eventId) in ${delayMillis / 1000} seconds at ${java.util.Date(triggerTime)}")
    }

    /**
     * Test Alarm helper: triggers alarm after specified delay (default 5 seconds).
     */
    fun scheduleTestAlarm(delayMillis: Long = 5000L) {
        val triggerTime = System.currentTimeMillis() + delayMillis
        val testId = 999999L

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            setAction(AlarmReceiver.ACTION_TRIGGER_ALARM)
            putExtra(EXTRA_EVENT_ID, testId)
            putExtra(EXTRA_EVENT_TITLE, "Test Meeting (Calendar Alarm)")
            putExtra(EXTRA_EVENT_START_TIME, System.currentTimeMillis() + 15 * 60 * 1000L)
            putExtra(EXTRA_IS_SNOOZE, false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            testId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarm(triggerTime, pendingIntent)
        Log.i(TAG, "Scheduled Test Alarm in ${delayMillis / 1000} seconds")
    }

    /**
     * Cancels any active scheduled alarm for an event ID.
     */
    fun cancelAlarm(eventId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            setAction(AlarmReceiver.ACTION_TRIGGER_ALARM)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for event ID $eventId")
        }
    }

    private fun scheduleExactAlarm(triggerTime: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Exact alarms permission not granted on Android 12+. Using setAndAllowWhileIdle.")
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                return
            }
        }

        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            0,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }
}
