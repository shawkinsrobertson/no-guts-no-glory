package com.shawkinsrobertson.noguts.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Placeholder for the interactive notification's action handling (toggle a factor chip,
 * capture a RemoteInput note, one-tap "nothing notable", save). Fully implemented in the
 * notification logging milestone, once [DailyReminderNotifier] posts an interactive
 * notification for this receiver to respond to.
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // No-op until the interactive notification lands.
    }
}
