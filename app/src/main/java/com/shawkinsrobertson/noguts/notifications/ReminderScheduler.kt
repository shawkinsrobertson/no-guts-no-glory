package com.shawkinsrobertson.noguts.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.time.ZonedDateTime

/**
 * Schedules the single daily reminder alarm. Re-derives the next trigger time from the
 * wall-clock hour/minute every time it's called (rather than storing an absolute
 * timestamp), so a device reboot, a timezone change, or a daylight-saving shift are all
 * handled the same way: whoever notices the change just calls [schedule] again.
 *
 * Uses an exact alarm when the OS grants that (required on API 31+ via a special,
 * user-grantable permission) and falls back to an inexact-but-still-doze-tolerant alarm
 * otherwise - a reminder landing a few minutes late is an acceptable V1 tradeoff for not
 * being able to schedule anything at all.
 */
class ReminderScheduler(private val context: Context) {

    fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    fun schedule(hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAtMillis = nextTriggerTimeMillis(hour, minute)
        val pendingIntent = alarmPendingIntent()
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancel() {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(alarmPendingIntent())
    }

    fun applyPreferences(reminderEnabled: Boolean, hour: Int, minute: Int) {
        if (reminderEnabled) schedule(hour, minute) else cancel()
    }

    /** Deep link to the OS screen where the user grants "Alarms & reminders" access. */
    fun exactAlarmSettingsIntent(): Intent =
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))

    private fun nextTriggerTimeMillis(hour: Int, minute: Int): Long {
        val now = ZonedDateTime.now()
        var trigger = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!trigger.isAfter(now)) {
            trigger = trigger.plusDays(1)
        }
        return trigger.toInstant().toEpochMilli()
    }

    private fun alarmPendingIntent(): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private companion object {
        const val REQUEST_CODE = 1001
    }
}
