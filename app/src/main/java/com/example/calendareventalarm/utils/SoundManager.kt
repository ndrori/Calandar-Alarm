package com.example.calendareventalarm.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class SoundManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    companion object {
        private const val TAG = "SoundManager"
    }

    init {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /**
     * Starts alarm sound and vibration loop using the provided ringtone Uri.
     */
    fun startAlarmSoundAndVibration(ringtoneUri: Uri) {
        stopSoundAndVibration() // Stop any previous playback

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, ringtoneUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setLegacyStreamType(AudioManager.STREAM_ALARM)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
            Log.i(TAG, "Alarm sound started playing: $ringtoneUri")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting alarm sound", e)
        }

        // Start Vibration
        try {
            vibrator?.let {
                val pattern = longArrayOf(0, 500, 500, 500, 500, 500)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(pattern, 0)
                    it.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(pattern, 0)
                }
                Log.d(TAG, "Alarm vibration started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering vibration", e)
        }
    }

    /**
     * Stops current alarm sound and vibration.
     */
    fun stopSoundAndVibration() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
                Log.d(TAG, "MediaPlayer stopped and released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        try {
            vibrator?.cancel()
            Log.d(TAG, "Vibrator cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling vibrator", e)
        }
    }
}
