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
import androidx.core.app.RemoteInput
import com.shawkinsrobertson.noguts.R
import com.shawkinsrobertson.noguts.data.db.dao.DailyLogFactorDao
import com.shawkinsrobertson.noguts.data.db.entity.FactorEntity
import com.shawkinsrobertson.noguts.data.db.entity.PendingNotificationLogEntity
import com.shawkinsrobertson.noguts.data.repository.FactorRepository
import com.shawkinsrobertson.noguts.data.repository.PendingNotificationLogRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Builds and posts the daily check-in notification, including the interactive version:
 * a stateful set of factor toggle actions (rebuilt and reposted to the same notification
 * ID on every tap, per plan section 19), an inline note via RemoteInput, a one-tap zero
 * day, and a save action that hands off to [com.shawkinsrobertson.noguts.data.repository.DailyLogRepository].
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

        shownFactors.forEach { factor ->
            val selected = factor.id in pending.selectedFactorIds
            val title = if (selected) "✓ ${factor.name}" else factor.name
            builder.addAction(toggleAction(date, factor.id, title))
        }
        builder.addAction(addNoteAction(date))
        builder.addAction(simpleAction(date, NotificationActions.ACTION_NOTHING_NOTABLE, "Nothing notable"))
        builder.addAction(simpleAction(date, NotificationActions.ACTION_SAVE, "Save"))

        NotificationManagerCompat.from(context).notify(REMINDER_NOTIFICATION_ID, builder.build())
    }

    /** Posts the final, non-interactive confirmation once the day is saved (section 22). */
    @SuppressLint("MissingPermission")
    fun postSavedResult(dailyPercent: Double?, rolling72Percent: Double?, targetPercent: Double?) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val body = buildString {
            append("Logged")
            dailyPercent?.let { append("\nToday's load: ${it.toInt()}%") }
            rolling72Percent?.let { append("\n72-hour load: ${it.toInt()}%") }
            targetPercent?.let { append("\nTarget: ${it.toInt()}%") }
        }

        val notification = NotificationCompat.Builder(context, NotificationChannels.DAILY_CHECK_IN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Logged")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentText(body.lineSequence().drop(1).firstOrNull() ?: "Today's log saved")
            .setOngoing(false)
            .setAutoCancel(true)
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

    private fun addNoteAction(date: LocalDate): NotificationCompat.Action {
        val remoteInput = RemoteInput.Builder(NotificationActions.REMOTE_INPUT_NOTE_KEY)
            .setLabel("Anything unusual about today?")
            .build()
        val intent = actionIntent(NotificationActions.ACTION_ADD_NOTE, date)
        // RemoteInput results require a MUTABLE PendingIntent (the system fills in the
        // reply before delivering it) - this is the one action here that can't be immutable.
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCodeFor(date, "note".hashCode().toLong()), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        return NotificationCompat.Action.Builder(0, "Add a note", pendingIntent)
            .addRemoteInput(remoteInput)
            .build()
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
    }
}
