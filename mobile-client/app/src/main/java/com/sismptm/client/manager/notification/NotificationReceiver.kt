package com.sismptm.client.manager.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sismptm.client.R
import com.sismptm.client.core.utils.NotificationHelper

/**
 * Receiver to handle alarms and trigger notifications for the client.
 */
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceId = intent.getLongExtra("SERVICE_ID", -1L)
        if (serviceId != -1L) {
            NotificationHelper.showNotification(
                context,
                context.getString(R.string.notification_title_reminder),
                context.getString(R.string.notification_msg_client, serviceId),
                serviceId
            )
        }
    }
}
