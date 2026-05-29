package com.sismptm.partner.manager.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.Instant

/**
 * Utility to schedule exact alarms for tour start times.
 */
object AlarmScheduler {
    fun scheduleServiceAlarm(context: Context, serviceId: Long, scheduledAt: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("SERVICE_ID", serviceId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            serviceId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            val triggerTime = Instant.parse(scheduledAt).toEpochMilli()
            // Subtract 1 minute to give the partner time to prepare
            val finalTriggerTime = triggerTime - 60000 

            if (finalTriggerTime > System.currentTimeMillis()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    finalTriggerTime,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            // Invalid date format
        }
    }
}
