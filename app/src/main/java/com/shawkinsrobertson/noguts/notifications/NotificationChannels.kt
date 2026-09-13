package com.shawkinsrobertson.noguts.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {
    const val DAILY_CHECK_IN = "daily_check_in"

    fun ensureCreated(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            DAILY_CHECK_IN,
            "Daily check-in",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Your daily stomach load reminder"
        }
        manager.createNotificationChannel(channel)
    }
}
