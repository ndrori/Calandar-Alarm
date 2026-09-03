package com.example.calendareventalarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.calendareventalarm.data.CalendarScanner
import com.example.calendareventalarm.service.CalendarContentObserverService
import com.example.calendareventalarm.utils.AlarmScheduler

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "BootReceiver received action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            // 1. Reschedule alarms for upcoming events
            try {
                val scanner = CalendarScanner(context)
                val scheduler = AlarmScheduler(context)

                val upcomingEvents = scanner.scanUpcomingEvents(48)
                val now = System.currentTimeMillis()

                for (event in upcomingEvents) {
                    if (event.alarmTime > now) {
                        scheduler.scheduleEventAlarm(event)
                    }
                }
                Log.i(TAG, "Successfully rescheduled alarms for ${upcomingEvents.size} calendar events after boot.")
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling alarms after boot", e)
            }

            // 2. Start Live Calendar Observer Service
            try {
                val observerIntent = Intent(context, CalendarContentObserverService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(observerIntent)
                } else {
                    context.startService(observerIntent)
                }
                Log.i(TAG, "CalendarContentObserverService started after boot.")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting CalendarContentObserverService after boot", e)
            }
        }
    }
}
