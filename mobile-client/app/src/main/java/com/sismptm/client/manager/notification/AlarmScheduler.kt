package com.sismptm.client.manager.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.Instant

object AlarmScheduler {

    /**
     * Schedules a tour reminder alarm at the exact scheduled time using AlarmManager, falling back to a non-exact alarm if exact scheduling is restricted.
     */
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
            if (triggerTime > System.currentTimeMillis()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Cancels any previously scheduled alarm for the specified service.
     */
    fun cancelServiceAlarm(context: Context, serviceId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            serviceId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
