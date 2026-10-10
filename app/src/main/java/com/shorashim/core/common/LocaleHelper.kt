package com.shorashim.core.common

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    const val HEBREW_TAG = "he"

    fun wrap(context: Context): Context {
        val locale = Locale.forLanguageTag(HEBREW_TAG)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
