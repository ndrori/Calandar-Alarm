package com.example.calendareventalarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.calendareventalarm.data.PreferencesManager
import com.example.calendareventalarm.service.AlarmService
import com.example.calendareventalarm.utils.AlarmScheduler

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_TRIGGER_ALARM = "com.example.calendareventalarm.ACTION_TRIGGER_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.example.calendareventalarm.ACTION_SNOOZE_ALARM"
        const val ACTION_DISMISS_ALARM = "com.example.calendareventalarm.ACTION_DISMISS_ALARM"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val eventId = intent.getLongExtra(AlarmScheduler.EXTRA_EVENT_ID, -1L)
        val eventTitle = intent.getStringExtra(AlarmScheduler.EXTRA_EVENT_TITLE) ?: "Meeting"
        val startTime = intent.getLongExtra(AlarmScheduler.EXTRA_EVENT_START_TIME, 0L)
        val isSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false)

        Log.i(TAG, "AlarmReceiver received action: $action for eventId: $eventId ('$eventTitle')")

        when (action) {
            ACTION_TRIGGER_ALARM -> {
                // Check if user dismissed this event already
                val prefs = PreferencesManager(context)
                if (prefs.isEventDismissed(eventId) && !isSnooze) {
                    Log.d(TAG, "Event ID $eventId was previously dismissed by user. Skipping alarm.")
                    return
                }

                // Start Foreground Service for continuous audio alarm & notification
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    setAction(AlarmService.ACTION_START_ALARM)
                    putExtra(AlarmScheduler.EXTRA_EVENT_ID, eventId)
                    putExtra(AlarmScheduler.EXTRA_EVENT_TITLE, eventTitle)
                    putExtra(AlarmScheduler.EXTRA_EVENT_START_TIME, startTime)
                    putExtra(AlarmScheduler.EXTRA_IS_SNOOZE, isSnooze)
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }

            ACTION_SNOOZE_ALARM -> {
                // Stop Current Alarm Audio
                stopAlarmService(context)

                // Schedule Snooze Alarm (5 Minutes)
                val scheduler = AlarmScheduler(context)
                val prefs = PreferencesManager(context)
                val delayMillis = prefs.getSnoozeDurationMillis()
                scheduler.scheduleSnoozeAlarm(eventId, eventTitle, startTime, delayMillis)
            }

            ACTION_DISMISS_ALARM -> {
                // Stop Current Alarm Audio & Clear Notification
                stopAlarmService(context)

                // Mark Event Dismissed
                val prefs = PreferencesManager(context)
                if (eventId != -1L) {
                    prefs.markEventDismissed(eventId)
                }
            }
        }
    }

    private fun stopAlarmService(context: Context) {
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            setAction(AlarmService.ACTION_STOP_ALARM)
        }
        context.startService(serviceIntent)
    }
}
