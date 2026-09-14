package com.shawkinsrobertson.noguts.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shawkinsrobertson.noguts.NoGutsApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Re-arms the reminder alarm after any event that can invalidate a previously scheduled
 * one: a reboot clears all alarms outright, and a wall-clock or timezone change can make
 * an already-scheduled trigger time wrong (see plan section 25).
 */
class BootAndTimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val container = (context.applicationContext as NoGutsApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profile = container.userPreferencesRepository.profile.first()
                ReminderScheduler(context).applyPreferences(
                    reminderEnabled = profile.reminderEnabled,
                    hour = profile.reminderHour,
                    minute = profile.reminderMinute
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
