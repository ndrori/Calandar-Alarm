package com.example.calendareventalarm.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.calendareventalarm.data.CalendarScanner
import com.example.calendareventalarm.data.PreferencesManager
import com.example.calendareventalarm.databinding.ActivityMainBinding
import com.example.calendareventalarm.model.AlarmStatus
import com.example.calendareventalarm.model.CalendarEvent
import com.example.calendareventalarm.service.CalendarContentObserverService
import com.example.calendareventalarm.utils.AlarmScheduler
import com.example.calendareventalarm.worker.CalendarSyncWorker

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefsManager: PreferencesManager
    private lateinit var calendarScanner: CalendarScanner
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var adapter: CalendarAdapter

    // Ringtone Picker Result Launcher
    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }

            if (uri != null) {
                val ringtone = RingtoneManager.getRingtone(this, uri)
                val name = ringtone?.getTitle(this) ?: "Selected Sound"
                prefsManager.setRingtone(uri, name)
                updateToneDisplay()
                Toast.makeText(this, "Alarm sound updated to: $name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Permission Request Launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val calendarGranted = permissions[Manifest.permission.READ_CALENDAR] ?: false
        if (calendarGranted) {
            binding.cardPermission.visibility = View.GONE
            syncCalendarAndScheduleAlarms()
            startBackgroundServices()
        } else {
            binding.cardPermission.visibility = View.VISIBLE
            Toast.makeText(this, "Calendar permission is required to track meetings.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)
        calendarScanner = CalendarScanner(this)
        alarmScheduler = AlarmScheduler(this)

        setupRecyclerView()
        updateToneDisplay()
        setupListeners()
        checkAndRequestPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (hasCalendarPermission()) {
            syncCalendarAndScheduleAlarms()
        }
    }

    private fun setupRecyclerView() {
        adapter = CalendarAdapter()
        binding.rvEvents.layoutManager = LinearLayoutManager(this)
        binding.rvEvents.adapter = adapter
    }

    private fun updateToneDisplay() {
        binding.tvSelectedToneName.text = prefsManager.getRingtoneName()
    }

    private fun setupListeners() {
        // Ringtone Picker Button
        binding.btnChangeSound.setOnClickListener {
            openRingtonePicker()
        }

        // Manual Sync Button
        binding.btnSyncNow.setOnClickListener {
            if (hasCalendarPermission()) {
                syncCalendarAndScheduleAlarms()
                Toast.makeText(this, "Calendar synced successfully!", Toast.LENGTH_SHORT).show()
            } else {
                checkAndRequestPermissions()
            }
        }

        // Test Alarm Button (5 Seconds Delay)
        binding.btnTestAlarm.setOnClickListener {
            alarmScheduler.scheduleTestAlarm(5000L)
            Toast.makeText(this, "Test alarm set for 5 seconds! (Lock or leave screen to test)", Toast.LENGTH_LONG).show()
        }

        // Grant Permission Card Button
        binding.btnGrantPermissions.setOnClickListener {
            checkAndRequestPermissions()
        }
    }

    private fun openRingtonePicker() {
        val currentUri = prefsManager.getRingtoneUri()
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone / Music")
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        }
        ringtonePickerLauncher.launch(intent)
    }

    private fun syncCalendarAndScheduleAlarms() {
        val events = calendarScanner.scanUpcomingEvents(48)
        val now = System.currentTimeMillis()

        val updatedEvents = mutableListOf<CalendarEvent>()

        for (event in events) {
            val isDismissed = prefsManager.isEventDismissed(event.eventId)
            val status = when {
                isDismissed -> AlarmStatus.DISMISSED
                event.alarmTime <= now && event.startTime > now -> AlarmStatus.FIRED
                else -> AlarmStatus.SCHEDULED
            }

            val updatedEvent = event.copy(status = status)
            updatedEvents.add(updatedEvent)

            // Schedule alarm 15 minutes prior to event start if active & not dismissed
            if (status == AlarmStatus.SCHEDULED && updatedEvent.alarmTime > now) {
                alarmScheduler.scheduleEventAlarm(updatedEvent)
            }
        }

        if (updatedEvents.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvEvents.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvEvents.visibility = View.VISIBLE
            adapter.updateEvents(updatedEvents)
        }
    }

    private fun startBackgroundServices() {
        // Start Live ContentObserver Service
        try {
            val serviceIntent = Intent(this, CalendarContentObserverService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Enqueue 15-minute background WorkManager task
        CalendarSyncWorker.enqueuePeriodicSync(this)
    }

    private fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (!hasCalendarPermission()) {
            permissionsToRequest.add(Manifest.permission.READ_CALENDAR)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            binding.cardPermission.visibility = View.VISIBLE
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            binding.cardPermission.visibility = View.GONE
            syncCalendarAndScheduleAlarms()
            startBackgroundServices()
            checkExactAlarmPermission()
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}
