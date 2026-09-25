package com.example.calendareventalarm.data

import android.content.Context
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import com.example.calendareventalarm.R

class PreferencesManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "calendar_alarm_prefs"
        private const val KEY_RINGTONE_URI = "key_ringtone_uri"
        private const val KEY_RINGTONE_NAME = "key_ringtone_name"
        private const val KEY_SNOOZE_DURATION_MINS = "key_snooze_duration_mins"
        private const val KEY_APP_LANGUAGE = "key_app_language"
        private const val DEFAULT_SNOOZE_MINS = 5
        const val LANG_SYSTEM = "system"
    }

    /**
     * Get user-selected Ringtone URI. Fallbacks to default System Alarm tone.
     */
    fun getRingtoneUri(): Uri {
        val uriStr = prefs.getString(KEY_RINGTONE_URI, null)
        return if (!uriStr.isNull_Empty()) {
            Uri.parse(uriStr)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }

    fun setRingtone(uri: Uri, name: String) {
        prefs.edit()
            .putString(KEY_RINGTONE_URI, uri.toString())
            .putString(KEY_RINGTONE_NAME, name)
            .apply()
    }

    fun getRingtoneName(): String {
        val defaultName = context.getString(R.string.default_system_sound)
        return prefs.getString(KEY_RINGTONE_NAME, defaultName) ?: defaultName
    }

    fun getLanguageCode(): String {
        return prefs.getString(KEY_APP_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM
    }

    fun setLanguageCode(code: String) {
        prefs.edit().putString(KEY_APP_LANGUAGE, code).apply()
    }

    fun getLanguageDisplayName(): String {
        return when (getLanguageCode()) {
            "en" -> "English"
            "es" -> "Español"
            "he" -> "עברית"
            "fr" -> "Français"
            "de" -> "Deutsch"
            "ar" -> "العربية"
            else -> context.getString(R.string.language_system_default)
        }
    }

    fun getSnoozeDurationMillis(): Long {
        val mins = prefs.getInt(KEY_SNOOZE_DURATION_MINS, DEFAULT_SNOOZE_MINS)
        return mins * 60 * 1000L
    }

    fun setSnoozeDurationMins(mins: Int) {
        prefs.edit().putInt(KEY_SNOOZE_DURATION_MINS, mins).apply()
    }

    fun markEventDismissed(eventId: Long) {
        val set = prefs.getStringSet("dismissed_events", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        set.add(eventId.toString())
        prefs.edit().putStringSet("dismissed_events", set).apply()
    }

    fun isEventDismissed(eventId: Long): Boolean {
        val set = prefs.getStringSet("dismissed_events", emptySet()) ?: emptySet()
        return set.contains(eventId.toString())
    }

    fun clearDismissedEvents() {
        prefs.edit().remove("dismissed_events").apply()
    }
}

private fun String?.isNull_Empty(): Boolean = this == null || this.trim().isEmpty()
