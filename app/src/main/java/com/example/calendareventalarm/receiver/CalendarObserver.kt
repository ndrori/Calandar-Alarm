package com.example.calendareventalarm.receiver

import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.calendareventalarm.data.CalendarScanner
import com.example.calendareventalarm.utils.AlarmScheduler

class CalendarObserver(
    private val context: Context,
    handler: Handler = Handler(Looper.getMainLooper()),
    private val onCalendarChanged: (() -> Unit)? = null
) : ContentObserver(handler) {

    companion object {
        private const val TAG = "CalendarObserver"
        const val ACTION_CALENDAR_CHANGED = "com.example.calendareventalarm.ACTION_CALENDAR_CHANGED"
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        Log.i(TAG, "Real-time calendar update detected! Uri: $uri. Rescanning upcoming events...")

        try {
            val scanner = CalendarScanner(context)
            val scheduler = AlarmScheduler(context)
            val events = scanner.scanUpcomingEvents(48)
            val now = System.currentTimeMillis()

            for (event in events) {
                if (event.alarmTime > now) {
                    scheduler.scheduleEventAlarm(event)
                }
            }

            onCalendarChanged?.invoke()

            // Broadcast change to active MainActivity UI
            val intent = Intent(ACTION_CALENDAR_CHANGED).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)

            Log.d(TAG, "Completed real-time rescan and scheduled ${events.size} event alarms.")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling real-time calendar change", e)
        }
    }
}

