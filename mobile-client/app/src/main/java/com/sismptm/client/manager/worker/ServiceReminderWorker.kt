package com.sismptm.client.manager.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sismptm.client.core.network.RetrofitClient
import com.sismptm.client.core.session.SessionManager
import com.sismptm.client.core.utils.NotificationHelper
import com.sismptm.client.R
import java.time.Instant

/**
 * Worker that runs in the background to check for scheduled services that are about to start for the client.
 */
class ServiceReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val clientId = SessionManager.userId
        if (clientId == -1L || !SessionManager.isLoggedIn()) {
            return Result.success()
        }

        try {
            val response = RetrofitClient.apiService.getServicesByClient(clientId)
            if (response.isSuccessful) {
                val services = response.body() ?: emptyList()
                val now = Instant.now()

                services.forEach { service ->
                    if (service.status.uppercase() == "ACCEPTED" || service.status.uppercase() == "WAITING_FOR_START") {
                        service.scheduledAt?.let { scheduledAt ->
                            try {
                                val scheduledTime = Instant.parse(scheduledAt)
                                // Notify if we are within 5 minutes of start time or passed it
                                if (now.isAfter(scheduledTime.minusSeconds(300))) {
                                    NotificationHelper.showNotification(
                                        applicationContext,
                                        applicationContext.getString(R.string.notification_title_reminder),
                                        applicationContext.getString(R.string.notification_msg_client, service.serviceId),
                                        service.serviceId
                                    )
                                }
                            } catch (e: Exception) {
                                // Invalid date format
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }
}
