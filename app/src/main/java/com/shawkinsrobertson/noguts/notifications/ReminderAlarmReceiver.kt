package com.shawkinsrobertson.noguts.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shawkinsrobertson.noguts.NoGutsApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Fires once a day at the configured reminder time; posts the check-in and re-arms itself. */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val container = (context.applicationContext as NoGutsApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                DailyReminderNotifier(context).postReminder()
                val profile = container.userPreferencesRepository.profile.first()
                if (profile.reminderEnabled) {
                    ReminderScheduler(context).schedule(profile.reminderHour, profile.reminderMinute)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
