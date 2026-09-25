package com.example.calendareventalarm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.CalendarContract
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.calendareventalarm.R
import com.example.calendareventalarm.receiver.CalendarObserver
import com.example.calendareventalarm.utils.LocaleUtils

class CalendarContentObserverService : Service() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleUtils.wrapContext(newBase))
    }

    private var calendarObserver: CalendarObserver? = null

    companion object {
        private const val TAG = "CalendarObserverSvc"
        private const val CHANNEL_ID = "channel_calendar_monitoring"
        private const val NOTIFICATION_ID = 2002
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerCalendarObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle(getString(R.string.notification_monitoring_title))
            .setContentText(getString(R.string.notification_monitoring_text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private fun registerCalendarObserver() {
        try {
            calendarObserver = CalendarObserver(this)
            contentResolver.registerContentObserver(
                CalendarContract.Events.CONTENT_URI,
                true,
                calendarObserver!!
            )
            Log.i(TAG, "Successfully registered real-time Calendar ContentObserver")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering Calendar ContentObserver", e)
        }
    }

    override fun onDestroy() {
        try {
            calendarObserver?.let {
                contentResolver.unregisterContentObserver(it)
                Log.i(TAG, "Unregistered Calendar ContentObserver")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering Calendar ContentObserver", e)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_monitoring_title),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_monitoring_text)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
