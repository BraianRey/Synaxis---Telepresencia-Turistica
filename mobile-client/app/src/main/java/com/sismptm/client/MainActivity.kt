package com.sismptm.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.sismptm.client.ui.navigation.NavGraph
import com.sismptm.client.ui.theme.SISPTMClientTheme
import com.sismptm.client.utils.LanguageContext

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
            val userLanguage by remember { mutableStateOf("es") }

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
