package com.sismptm.partner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sismptm.partner.core.session.SessionManager
import com.sismptm.partner.ui.navigation.PartnerNavGraph
import com.sismptm.partner.ui.theme.SISPTMPartnerTheme
import com.sismptm.partner.core.utils.LanguageContext
import com.sismptm.partner.core.utils.NotificationHelper
import com.sismptm.partner.manager.worker.ServiceReminderWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize notification channels
        NotificationHelper.createNotificationChannel(this)
        
        // Schedule background service reminders
        setupBackgroundWorkers()

        setContent {
            val userLanguage by SessionManager.languageFlow.collectAsState()

            LanguageContext(languageCode = userLanguage) {
                SISPTMPartnerTheme {
                    PartnerNavGraph()
                }
            }
        }
    }

    private fun setupBackgroundWorkers() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val serviceCheckRequest = PeriodicWorkRequestBuilder<ServiceReminderWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "ServiceReminderWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            serviceCheckRequest
        )
    }
}
