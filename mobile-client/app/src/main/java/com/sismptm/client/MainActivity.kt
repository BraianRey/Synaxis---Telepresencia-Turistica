package com.sismptm.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.sismptm.client.core.session.SessionManager
import com.sismptm.client.core.utils.LanguageContext
import com.sismptm.client.ui.navigation.NavGraph
import com.sismptm.client.ui.theme.SISPTMClientTheme

/**
 * Main activity of the application.
 * Manages the root UI composition and application-wide configurations.
 */
class MainActivity : ComponentActivity() {
    /**
     * Initializes the activity, sets up edge-to-edge display and the Compose content.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val userLanguage by SessionManager.languageFlow.collectAsState()

            LanguageContext(languageCode = userLanguage) {
                SISPTMClientTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NavGraph()
                    }
                }
            }
        }
    }
}
