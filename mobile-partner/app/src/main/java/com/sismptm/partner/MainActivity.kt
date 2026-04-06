package com.sismptm.partner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.sismptm.partner.ui.navigation.PartnerNavGraph
import com.sismptm.partner.ui.theme.SISPTMPartnerTheme
import com.sismptm.partner.utils.LanguageContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // This is the user's "language" attribute.
            // In a real scenario, this would come from a database, DataStore, or SharedPreferences.
            val userLanguage by remember { mutableStateOf("en") } // Default "en"

            LanguageContext(languageCode = userLanguage) {
                SISPTMPartnerTheme {
                    PartnerNavGraph()
                }
            }
        }
    }
}
