package com.example.calendareventalarm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.calendareventalarm.R
import com.example.calendareventalarm.data.PreferencesManager
import com.example.calendareventalarm.receiver.AlarmReceiver
import com.example.calendareventalarm.ui.AlarmActivity
import com.example.calendareventalarm.utils.AlarmScheduler
import com.example.calendareventalarm.utils.LocaleUtils
import com.example.calendareventalarm.utils.SoundManager
import kotlin.math.max
import kotlin.math.roundToInt

class AlarmService : Service() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleUtils.wrapContext(newBase))
    }

    private lateinit var soundManager: SoundManager
    private lateinit var prefsManager: PreferencesManager

    companion object {
        private const val TAG = "AlarmService"
        private const val CHANNEL_ID = "channel_calendar_alarm"
        private const val NOTIFICATION_ID = 2001

        const val ACTION_START_ALARM = "action_start_alarm"
        const val ACTION_STOP_ALARM = "action_stop_alarm"
    }

    override fun onCreate() {
        super.onCreate()
        soundManager = SoundManager(this)
        prefsManager = PreferencesManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_ALARM

        if (action == ACTION_STOP_ALARM) {
            stopSelfAlarm()
            return START_NOT_STICKY
        }

        val eventId = intent?.getLongExtra(AlarmScheduler.EXTRA_EVENT_ID, -1L) ?: -1L
        val eventTitle = intent?.getStringExtra(AlarmScheduler.EXTRA_EVENT_TITLE) ?: getString(R.string.app_name)
        val startTime = intent?.getLongExtra(AlarmScheduler.EXTRA_EVENT_START_TIME, 0L) ?: 0L
        val isSnooze = intent?.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false) ?: false

        Log.i(TAG, "Starting AlarmService for event '$eventTitle' (ID: $eventId, Snooze: $isSnooze)")

        // Play User Defined Alarm Audio Tone and Vibration
        val ringtoneUri = prefsManager.getRingtoneUri()
        soundManager.startAlarmSoundAndVibration(ringtoneUri)

        // Build Fullscreen Intent Activity
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(AlarmScheduler.EXTRA_EVENT_ID, eventId)
            putExtra(AlarmScheduler.EXTRA_EVENT_TITLE, eventTitle)
            putExtra(AlarmScheduler.EXTRA_EVENT_START_TIME, startTime)
            putExtra(AlarmScheduler.EXTRA_IS_SNOOZE, isSnooze)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            eventId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action PendingIntents for Notification Bar
        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            setAction(AlarmReceiver.ACTION_SNOOZE_ALARM)
            putExtra(AlarmScheduler.EXTRA_EVENT_ID, eventId)
            putExtra(AlarmScheduler.EXTRA_EVENT_TITLE, eventTitle)
            putExtra(AlarmScheduler.EXTRA_EVENT_START_TIME, startTime)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            (eventId + 100).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmReceiver::class.java).apply {
            setAction(AlarmReceiver.ACTION_DISMISS_ALARM)
            putExtra(AlarmScheduler.EXTRA_EVENT_ID, eventId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            (eventId + 200).toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val leadMins = max(0, ((startTime - System.currentTimeMillis()) / (60 * 1000.0)).roundToInt())

        val notificationTitle = when {
            isSnooze -> getString(R.string.notification_snooze_title, eventTitle)
            leadMins > 0 -> getString(R.string.notification_alarm_title_mins, leadMins)
            else -> getString(R.string.notification_alarm_title_now)
        }
        val notificationText = getString(R.string.notification_alarm_text, eventTitle)

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_media_pause, getString(R.string.notification_snooze_action), snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.notification_dismiss_action), dismissPendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Also explicitly launch activity so full screen shows up
        startActivity(fullScreenIntent)

        return START_STICKY
    }

    private fun stopSelfAlarm() {
        Log.i(TAG, "Stopping AlarmService audio & foreground notification")
        soundManager.stopSoundAndVibration()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        soundManager.stopSoundAndVibration()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.dashboard_title)
            val descriptionText = getString(R.string.dashboard_subtitle)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setBypassDnd(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
