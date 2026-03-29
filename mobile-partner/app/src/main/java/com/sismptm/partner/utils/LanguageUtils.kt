package com.sismptm.partner.utils

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Utility to force a specific language in the app regardless of system settings.
 */
@Composable
fun LanguageContext(
    languageCode: String?, // Expected "en" or "es"
    content: @Composable () -> Unit
) {
    val targetLanguage = if (languageCode == "es") "es" else "en"
    val locale = Locale(targetLanguage)
    Locale.setDefault(locale)

    val configuration = LocalConfiguration.current
    configuration.setLocale(locale)

    val context = LocalContext.current
    val resources = context.resources
    resources.updateConfiguration(configuration, resources.displayMetrics)

    CompositionLocalProvider(LocalConfiguration provides configuration) {
        content()
    }
}
