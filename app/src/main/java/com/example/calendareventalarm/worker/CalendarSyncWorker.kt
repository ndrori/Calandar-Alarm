package com.example.calendareventalarm.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.calendareventalarm.data.CalendarScanner
import com.example.calendareventalarm.utils.AlarmScheduler
import java.util.concurrent.TimeUnit

class CalendarSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        private const val TAG = "CalendarSyncWorker"
        private const val WORK_NAME = "periodic_calendar_sync_work"

        fun enqueuePeriodicSync(context: Context) {
            val constraints = Constraints.Builder().build()

            val syncRequest = PeriodicWorkRequestBuilder<CalendarSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            Log.i(TAG, "Enqueued periodic calendar sync worker (15-minute interval)")
        }
    }

    override fun doWork(): Result {
        Log.i(TAG, "Executing background calendar sync worker...")
        return try {
            val scanner = CalendarScanner(applicationContext)
            val scheduler = AlarmScheduler(applicationContext)

            val events = scanner.scanUpcomingEvents(48)
            val now = System.currentTimeMillis()

            for (event in events) {
                if (event.alarmTime > now) {
                    scheduler.scheduleEventAlarm(event)
                }
            }

            Log.i(TAG, "CalendarSyncWorker successfully synced ${events.size} calendar events.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "CalendarSyncWorker failed", e)
            Result.retry()
        }
    }
}
