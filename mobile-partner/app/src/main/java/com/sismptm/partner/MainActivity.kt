package com.sismptm.partner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
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

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    /**
     * Initializes the partner main activity, checks for permissions, and configures workers.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        NotificationHelper.createNotificationChannel(this)
        setupBackgroundWorkers()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

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
