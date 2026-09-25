package com.example.calendareventalarm.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.example.calendareventalarm.data.PreferencesManager
import java.util.Locale

object LocaleUtils {

    fun wrapContext(context: Context): Context {
        val prefs = PreferencesManager(context)
        val langCode = prefs.getLanguageCode()

        if (langCode == PreferencesManager.LANG_SYSTEM) {
            return context
        }

        val locale = getLocale(langCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLayoutDirection(locale)
        }

        return context.createConfigurationContext(config)
    }

    fun getLocale(langCode: String): Locale {
        return when (langCode) {
            "he" -> Locale("iw") // Samsung and older Android devices use legacy ISO 639 tag "iw"
            "ar" -> Locale("ar")
            "es" -> Locale("es")
            "fr" -> Locale("fr")
            "de" -> Locale("de")
            "en" -> Locale("en")
            else -> Locale.getDefault()
        }
    }
}
