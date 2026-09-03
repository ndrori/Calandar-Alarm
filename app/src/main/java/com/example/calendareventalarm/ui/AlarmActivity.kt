package com.example.calendareventalarm.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.calendareventalarm.databinding.ActivityAlarmBinding
import com.example.calendareventalarm.receiver.AlarmReceiver
import com.example.calendareventalarm.service.AlarmService
import com.example.calendareventalarm.utils.AlarmScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmBinding
    private var eventId: Long = -1L
    private var eventTitle: String = "Upcoming Meeting"
    private var startTime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        showOverLockscreen()
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        eventId = intent.getLongExtra(AlarmScheduler.EXTRA_EVENT_ID, -1L)
        eventTitle = intent.getStringExtra(AlarmScheduler.EXTRA_EVENT_TITLE) ?: "Upcoming Meeting"
        startTime = intent.getLongExtra(AlarmScheduler.EXTRA_EVENT_START_TIME, 0L)
        val isSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false)

        binding.tvAlarmHeader.text = if (isSnooze) "SNOOZED MEETING ALARM" else "UPCOMING MEETING ALARM"
        binding.tvAlarmEventTitle.text = eventTitle

        if (startTime > 0) {
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            binding.tvMeetingTimeDetails.text = "Meeting starts at ${timeFormat.format(Date(startTime))}"
        } else {
            binding.tvMeetingTimeDetails.text = "Meeting starts in 15 minutes"
        }

        // Snooze Button Click (Snooze 5 Minutes)
        binding.btnSnooze.setOnClickListener {
            onSnoozeClicked()
        }

        // Dismiss Button Click
        binding.btnDismiss.setOnClickListener {
            onDismissClicked()
        }
    }

    private fun onSnoozeClicked() {
        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            setAction(AlarmReceiver.ACTION_SNOOZE_ALARM)
            putExtra(AlarmScheduler.EXTRA_EVENT_ID, eventId)
            putExtra(AlarmScheduler.EXTRA_EVENT_TITLE, eventTitle)
            putExtra(AlarmScheduler.EXTRA_EVENT_START_TIME, startTime)
        }
        sendBroadcast(snoozeIntent)
        stopAlarmService()
        finish()
    }

    private fun onDismissClicked() {
        val dismissIntent = Intent(this, AlarmReceiver::class.java).apply {
            setAction(AlarmReceiver.ACTION_DISMISS_ALARM)
            putExtra(AlarmScheduler.EXTRA_EVENT_ID, eventId)
        }
        sendBroadcast(dismissIntent)
        stopAlarmService()
        finish()
    }

    private fun stopAlarmService() {
        val serviceIntent = Intent(this, AlarmService::class.java).apply {
            setAction(AlarmService.ACTION_STOP_ALARM)
        }
        startService(serviceIntent)
    }

    private fun showOverLockscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent back button from bypassing alarm screen without snooze or dismiss
        onSnoozeClicked()
    }
}
