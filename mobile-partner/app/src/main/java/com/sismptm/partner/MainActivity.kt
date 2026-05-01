package com.sismptm.partner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sismptm.partner.core.session.SessionManager
import com.sismptm.partner.ui.navigation.PartnerNavGraph
import com.sismptm.partner.ui.theme.SISPTMPartnerTheme
import com.sismptm.partner.core.utils.LanguageContext

/**
 * Main activity of the application. Sets up the theme and the navigation graph.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Observe language dynamically from SessionManager
            val userLanguage by SessionManager.languageFlow.collectAsState()

            LanguageContext(languageCode = userLanguage) {
                SISPTMPartnerTheme {
                    PartnerNavGraph()
                }
            }
        }
    }
}
