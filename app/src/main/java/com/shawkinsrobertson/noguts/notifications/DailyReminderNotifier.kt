package com.shawkinsrobertson.noguts.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.shawkinsrobertson.noguts.R

/**
 * Builds and posts the daily check-in notification. This is a minimal, non-interactive
 * placeholder for now; the interactive stateful-action version (toggleable factor chips,
 * RemoteInput note, one-tap zero day) lands with the notification logging milestone.
 */
class DailyReminderNotifier(private val context: Context) {

    fun postReminder() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(context, NotificationChannels.DAILY_CHECK_IN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(REMINDER_NOTIFICATION_ID, notification)
    }

    companion object {
        const val REMINDER_NOTIFICATION_ID = 42
    }
}
