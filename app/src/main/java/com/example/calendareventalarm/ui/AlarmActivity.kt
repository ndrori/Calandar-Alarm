package com.example.calendareventalarm.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.calendareventalarm.R
import com.example.calendareventalarm.data.PreferencesManager
import com.example.calendareventalarm.databinding.ActivityAlarmBinding
import com.example.calendareventalarm.receiver.AlarmReceiver
import com.example.calendareventalarm.service.AlarmService
import com.example.calendareventalarm.utils.AlarmScheduler
import com.example.calendareventalarm.utils.LocaleUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

class AlarmActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleUtils.wrapContext(newBase))
    }

    private lateinit var binding: ActivityAlarmBinding
    private var eventId: Long = -1L
    private var eventTitle: String = ""
    private var startTime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        showOverLockscreen()
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        eventId = intent.getLongExtra(AlarmScheduler.EXTRA_EVENT_ID, -1L)
        eventTitle = intent.getStringExtra(AlarmScheduler.EXTRA_EVENT_TITLE) ?: getString(R.string.app_name)
        startTime = intent.getLongExtra(AlarmScheduler.EXTRA_EVENT_START_TIME, 0L)
        val isSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false)

        binding.tvAlarmHeader.text = if (isSnooze) {
            getString(R.string.notification_snooze_title, eventTitle)
        } else {
            getString(R.string.alarm_ringing_title)
        }
        binding.tvAlarmEventTitle.text = eventTitle

        val leadMins = max(0, ((startTime - System.currentTimeMillis()) / (60 * 1000.0)).roundToInt())

        if (startTime > 0) {
            val activeLocale = LocaleUtils.getLocale(PreferencesManager(this).getLanguageCode())
            val timeFormat = SimpleDateFormat("h:mm a", activeLocale)
            val timeDetailText = if (leadMins > 0) {
                getString(R.string.meeting_starts_in_mins, leadMins)
            } else {
                getString(R.string.meeting_starting_now)
            }
            binding.tvMeetingTimeDetails.text = "$timeDetailText (${timeFormat.format(Date(startTime))})"
        } else {
            binding.tvMeetingTimeDetails.text = getString(R.string.meeting_starting_now)
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
