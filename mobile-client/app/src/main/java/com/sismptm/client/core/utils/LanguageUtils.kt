package com.sismptm.client.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Provides a localized context for the application based on the provided language code.
 * This is used to dynamically change the app's language at runtime.
 */
@Composable
fun LanguageContext(
    languageCode: String?,
    content: @Composable () -> Unit
) {
    val targetLanguage = if (languageCode == "es") "es" else "en"
    val locale = Locale(targetLanguage)
    Locale.setDefault(locale)

    val configuration = LocalConfiguration.current
    configuration.setLocale(locale)

    val context = LocalContext.current
    val resources = context.resources
    
    // Update resources configuration to apply the new locale
    resources.updateConfiguration(configuration, resources.displayMetrics)

    CompositionLocalProvider(LocalConfiguration provides configuration) {
        content()
    }
}
