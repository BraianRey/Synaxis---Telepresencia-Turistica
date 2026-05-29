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
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sismptm.client.core.session.SessionManager
import com.sismptm.client.core.utils.LanguageContext
import com.sismptm.client.core.utils.NotificationHelper
import com.sismptm.client.manager.worker.ServiceReminderWorker
import com.sismptm.client.ui.navigation.NavGraph
import com.sismptm.client.ui.theme.SISPTMClientTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize notification channels and schedule background tasks
        NotificationHelper.createNotificationChannel(this)
        setupBackgroundWorkers()

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

    private fun setupBackgroundWorkers() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val serviceCheckRequest = PeriodicWorkRequestBuilder<ServiceReminderWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "ClientServiceReminderWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            serviceCheckRequest
        )
    }
}
