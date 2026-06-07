package com.sismptm.partner.manager.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sismptm.partner.R
import com.sismptm.partner.core.utils.NotificationHelper

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceId = intent.getLongExtra("SERVICE_ID", -1L)
        if (serviceId != -1L) {
            NotificationHelper.showNotification(
                context,
                serviceId,
                context.getString(R.string.notification_title_reminder),
                context.getString(R.string.notification_msg_partner, serviceId)
            )
        }
    }
}
