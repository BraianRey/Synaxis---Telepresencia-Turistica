package com.sismptm.partner.manager.worker

import android.content.Context
import androidx.work.*
import com.sismptm.partner.core.network.RetrofitClient
import com.sismptm.partner.core.session.SessionManager
import com.sismptm.partner.core.utils.NotificationHelper
import com.sismptm.partner.R
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Worker that checks for scheduled services and triggers notifications.
 */
class ServiceReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val partnerId = SessionManager.partnerId
        if (partnerId == 0L || !SessionManager.isLoggedIn()) return Result.success()

        try {
            val response = RetrofitClient.apiService.getServicesByPartner(partnerId)
            if (response.isSuccessful) {
                val services = response.body() ?: emptyList()
                val now = Instant.now()

                services.forEach { service ->
                    if (service.status.uppercase() == "WAITING_FOR_START" && service.scheduledAt != null) {
                        val scheduledTime = try { Instant.parse(service.scheduledAt) } catch (e: Exception) { null }
                        
                        scheduledTime?.let { time ->
                            val diff = Duration.between(now, time).toMinutes()
                            
                            // 1. Trigger notification if it's already time (or 1 min before)
                            if (now.isAfter(time.minusSeconds(60))) {
                                NotificationHelper.showNotification(
                                    applicationContext,
                                    service.serviceId,
                                    applicationContext.getString(R.string.notification_title_reminder),
                                    applicationContext.getString(R.string.notification_msg_partner, service.serviceId)
                                )
                            } 
                            // 2. If the tour is in the future but soon, we could schedule a one-time precise shot
                            // but usually the Periodic worker + the App open logic is enough.
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }

    companion object {
        /**
         * Schedules a one-time precise notification for a specific service.
         */
        fun schedulePreciseNotification(context: Context, serviceId: Long, scheduledAt: String) {
            val now = Instant.now()
            val tourTime = try { Instant.parse(scheduledAt) } catch (e: Exception) { return }
            val delay = Duration.between(now, tourTime).toMillis()

            if (delay > 0) {
                val workRequest = OneTimeWorkRequestBuilder<ServiceReminderWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .addTag("SERVICE_$serviceId")
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    "PRECISE_NOTIF_$serviceId",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            }
        }
    }
}
