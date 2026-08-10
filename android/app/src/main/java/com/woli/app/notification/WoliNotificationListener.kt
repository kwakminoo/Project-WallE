package com.woli.app.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class WoliNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return

        NotificationEventParser.parse(this, sbn, packageManager)?.let(WoliNotificationCenter::add)
    }
}
