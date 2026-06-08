package com.sismptm.partner.manager.worker

import android.content.Context
import androidx.work.*
import com.sismptm.partner.R
import com.sismptm.partner.core.network.RetrofitClient
import com.sismptm.partner.core.session.SessionManager
import com.sismptm.partner.core.utils.NotificationHelper
import com.sismptm.partner.data.remote.api.dto.ServiceResponse
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

        return try {
            val response = RetrofitClient.apiService.getServicesByPartner(partnerId)
            if (response.isSuccessful) {
                val now = Instant.now()
                response.body().orEmpty().forEach { service -> notifyIfDue(service, now) }
            }
            Result.success()
        } catch (e: java.io.IOException) {
            android.util.Log.w("ServiceReminder", "Network error while checking services, will retry", e)
            Result.retry()
        }
    }

    /**
     * Triggers a reminder notification for a single scheduled service when its start time
     * has arrived (or is within one minute). No-op for any other service state.
     */
    private fun notifyIfDue(service: ServiceResponse, now: Instant) {
        if (service.status.uppercase() != "WAITING_FOR_START" || service.scheduledAt == null) return

        val scheduledTime = try {
            Instant.parse(service.scheduledAt)
        } catch (e: java.time.format.DateTimeParseException) {
            android.util.Log.w("ServiceReminder", "Invalid scheduledAt: ${service.scheduledAt}", e)
            return
        }

        if (now.isAfter(scheduledTime.minusSeconds(60))) {
            NotificationHelper.showNotification(
                applicationContext,
                service.serviceId,
                applicationContext.getString(R.string.notification_title_reminder),
                applicationContext.getString(R.string.notification_msg_partner, service.serviceId)
            )
        }
    }

    companion object {
        /**
         * Schedules a one-time precise notification for a specific service.
         */
        fun schedulePreciseNotification(context: Context, serviceId: Long, scheduledAt: String) {
            val now = Instant.now()
            val tourTime = try {
                Instant.parse(scheduledAt)
            } catch (e: java.time.format.DateTimeParseException) {
                android.util.Log.w("ServiceReminder", "Invalid scheduledAt: $scheduledAt", e)
                return
            }
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
