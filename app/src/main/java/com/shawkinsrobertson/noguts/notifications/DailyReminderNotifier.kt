package com.shawkinsrobertson.noguts.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.shawkinsrobertson.noguts.MainActivity
import com.shawkinsrobertson.noguts.R
import com.shawkinsrobertson.noguts.data.db.dao.DailyLogFactorDao
import com.shawkinsrobertson.noguts.data.db.entity.FactorEntity
import com.shawkinsrobertson.noguts.data.db.entity.PendingNotificationLogEntity
import com.shawkinsrobertson.noguts.data.repository.FactorRepository
import com.shawkinsrobertson.noguts.data.repository.PendingNotificationLogRepository
import com.shawkinsrobertson.noguts.ui.components.formatPoints
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Builds and posts the daily check-in notification, including the interactive version:
 * a stateful quick-toggle action (rebuilt and reposted to the same notification ID on
 * every tap, per plan section 19), a one-tap zero day, and a save action that hands off
 * to [com.shawkinsrobertson.noguts.data.repository.DailyLogRepository].
 *
 * Android's notification shade only ever renders the first 3 actions added via
 * [NotificationCompat.Builder.addAction] - anything past that is silently dropped, not
 * overflowed into a menu. So exactly 3 actions are added here: one quick factor toggle,
 * "Nothing notable", and "Save" - the two ways to actually finish a day always fit.
 * Toggling more than one factor, adding a note, or picking an intensity level for a LEVEL
 * factor all require the full check-in screen, reachable by tapping the notification body.
 */
class DailyReminderNotifier(
    private val context: Context,
    private val factorRepository: FactorRepository,
    private val dailyLogFactorDao: DailyLogFactorDao,
    private val pendingRepository: PendingNotificationLogRepository
) {

    /** Posted by the daily alarm: ranks factors fresh and starts a new pending log for [date]. */
    suspend fun postInitialReminder(date: LocalDate = LocalDate.now()) {
        val activeFactors = factorRepository.getActiveFactors()
        val stats = dailyLogFactorDao.getSelectionStatsSince(date.minusDays(14))
        val shownFactors = NotificationFactorRanker.rank(activeFactors, stats)
        val pending = pendingRepository.ensureInitialized(date, shownFactors.map { it.id })
        postInteractive(date, shownFactors, pending)
    }

    /** Rebuilds and reposts the interactive notification after a toggle or note capture,
     * reusing the same factor set the pending log already committed to. */
    suspend fun refreshInteractive(date: LocalDate) {
        val pending = pendingRepository.get(date) ?: return
        val shownFactors = factorRepository.getByIds(pending.shownFactorIds)
            .sortedBy { pending.shownFactorIds.indexOf(it.id) }
        postInteractive(date, shownFactors, pending)
    }

    // The checkSelfPermission call below is inline (rather than via a shared private
    // helper) so lint's own MissingPermission data-flow check can see it guarding the
    // notify() call in the same method; @SuppressLint is a belt-and-suspenders backstop
    // in case that heuristic still doesn't trace it in a given AGP/lint version.
    @SuppressLint("MissingPermission")
    private fun postInteractive(date: LocalDate, shownFactors: List<FactorEntity>, pending: PendingNotificationLogEntity) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val builder = NotificationCompat.Builder(context, NotificationChannels.DAILY_CHECK_IN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_body))
            // Deliberately not ongoing: plan section 23 expects the user to be able to
            // dismiss it and still resume from the persisted pending state later.
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openAppPendingIntent())

        shownFactors.take(MAX_QUICK_TOGGLE_FACTORS).forEach { factor ->
            val selected = factor.id in pending.selectedFactorIds
            val title = if (selected) "✓ ${factor.name}" else factor.name
            builder.addAction(toggleAction(date, factor.id, title))
        }
        builder.addAction(simpleAction(date, NotificationActions.ACTION_NOTHING_NOTABLE, "Nothing notable"))
        builder.addAction(simpleAction(date, NotificationActions.ACTION_SAVE, "Save"))

        NotificationManagerCompat.from(context).notify(REMINDER_NOTIFICATION_ID, builder.build())
    }

    /** Posts the final, non-interactive confirmation once the day is saved (section 22).
     * Points, not the normalized percentage - same as everywhere else in the UI. */
    @SuppressLint("MissingPermission")
    fun postSavedResult(dailyPoints: Double?, rolling72Points: Double?, targetPoints: Double?) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val body = buildString {
            append("Logged")
            dailyPoints?.let { append("\nToday's load: ${it.formatPoints()}") }
            rolling72Points?.let { append("\n72-hour load: ${it.formatPoints()}") }
            targetPoints?.let { append("\nTarget: ${it.formatPoints()}") }
        }

        val notification = NotificationCompat.Builder(context, NotificationChannels.DAILY_CHECK_IN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Logged")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentText(body.lineSequence().drop(1).firstOrNull() ?: "Today's log saved")
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent())
            .build()

        NotificationManagerCompat.from(context).notify(REMINDER_NOTIFICATION_ID, notification)
    }

    private fun toggleAction(date: LocalDate, factorId: Long, title: String): NotificationCompat.Action {
        val intent = actionIntent(NotificationActions.ACTION_TOGGLE_FACTOR, date).apply {
            putExtra(NotificationActions.EXTRA_FACTOR_ID, factorId)
        }
        val requestCode = requestCodeFor(date, factorId)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(0, title, pendingIntent).build()
    }

    private fun simpleAction(date: LocalDate, action: String, title: String): NotificationCompat.Action {
        val intent = actionIntent(action, date)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCodeFor(date, action.hashCode().toLong()), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(0, title, pendingIntent).build()
    }

    /** Launches [MainActivity] on tapping the notification body - the way to reach the
     * full check-in (more than one factor, an intensity level, a note) once the 3-action
     * budget is spent on the quick toggle, Nothing notable, and Save. */
    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun actionIntent(action: String, date: LocalDate): Intent =
        Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationActions.EXTRA_DATE, date.format(DateTimeFormatter.ISO_LOCAL_DATE))
        }

    private fun requestCodeFor(date: LocalDate, discriminator: Long): Int =
        (date.toEpochDay() * 1000 + (discriminator % 1000)).toInt()

    companion object {
        const val REMINDER_NOTIFICATION_ID = 42

        /** Leaves exactly 2 slots for "Nothing notable" and "Save" within Android's
         * 3-visible-action budget (see the class kdoc). */
        private const val MAX_QUICK_TOGGLE_FACTORS = 1
    }
}
