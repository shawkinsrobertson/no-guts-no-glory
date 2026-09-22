package com.shawkinsrobertson.noguts.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
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
 * Builds and posts the daily check-in notification, including the interactive version: up
 * to 3 quick-toggle factor buttons (rebuilt and reposted to the same notification ID on
 * every tap, per plan section 19), a one-tap zero day, a save action, and an inline note.
 *
 * A standard notification's addAction() row only ever renders the first 3 actions - past
 * that, extras are silently dropped, not overflowed into a menu. That's not enough room
 * for 3 factor toggles plus "Nothing notable" plus "Save", so this uses a *custom* content
 * view instead (setCustomBigContentView): the toggle/nothing-notable/save buttons are
 * plain RemoteViews Buttons wired with setOnClickPendingIntent, which aren't subject to
 * that 3-action cap at all since they're not NotificationCompat.Action objects. The one
 * thing that still has to be a real Action is "Add a note" - RemoteInput (the system's
 * inline reply UI) only attaches to an Action, not to an arbitrary RemoteViews click - but
 * with the other 5 buttons moved off the action row, that single action is nowhere near
 * the 3-action limit. Picking an intensity level for a LEVEL factor still isn't possible
 * here (no room for a picker), so those log at a sensible default; the full check-in
 * screen for that is reachable by tapping the notification body.
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

        val remoteViews = RemoteViews(context.packageName, R.layout.notification_interactive)

        QUICK_TOGGLE_BUTTON_IDS.forEachIndexed { index, buttonId ->
            val factor = shownFactors.getOrNull(index)
            if (factor == null) {
                remoteViews.setViewVisibility(buttonId, View.GONE)
            } else {
                val selected = factor.id in pending.selectedFactorIds
                remoteViews.setViewVisibility(buttonId, View.VISIBLE)
                remoteViews.setTextViewText(buttonId, if (selected) "✓ ${factor.name}" else factor.name)
                remoteViews.setOnClickPendingIntent(buttonId, toggleFactorPendingIntent(date, factor.id))
            }
        }
        remoteViews.setOnClickPendingIntent(
            R.id.nothing_notable_button,
            simplePendingIntent(date, NotificationActions.ACTION_NOTHING_NOTABLE)
        )
        remoteViews.setOnClickPendingIntent(R.id.save_button, simplePendingIntent(date, NotificationActions.ACTION_SAVE))

        val builder = NotificationCompat.Builder(context, NotificationChannels.DAILY_CHECK_IN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_body))
            // DecoratedCustomViewStyle wraps remoteViews with the normal notification chrome
            // (small icon, app name, timestamp, expand affordance) instead of it standing
            // alone - otherwise a custom big content view looks bare/unbranded.
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomBigContentView(remoteViews)
            // Deliberately not ongoing: plan section 23 expects the user to be able to
            // dismiss it and still resume from the persisted pending state later.
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openAppPendingIntent())
            .addAction(addNoteAction(date))

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

    private fun toggleFactorPendingIntent(date: LocalDate, factorId: Long): PendingIntent {
        val intent = actionIntent(NotificationActions.ACTION_TOGGLE_FACTOR, date).apply {
            putExtra(NotificationActions.EXTRA_FACTOR_ID, factorId)
        }
        val requestCode = requestCodeFor(date, factorId)
        return PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun simplePendingIntent(date: LocalDate, action: String): PendingIntent {
        val intent = actionIntent(action, date)
        return PendingIntent.getBroadcast(
            context, requestCodeFor(date, action.hashCode().toLong()), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** The one real NotificationCompat.Action left (see the class kdoc for why) - a
     * RemoteInput reply only attaches to an Action, not to a plain RemoteViews click. */
    private fun addNoteAction(date: LocalDate): NotificationCompat.Action {
        val remoteInput = RemoteInput.Builder(NotificationActions.REMOTE_INPUT_NOTE_KEY)
            .setLabel("Anything unusual about today?")
            .build()
        val intent = actionIntent(NotificationActions.ACTION_ADD_NOTE, date)
        // RemoteInput results require a MUTABLE PendingIntent (the system fills in the
        // reply before delivering it) - this is the one pending intent here that can't be
        // immutable.
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCodeFor(date, "note".hashCode().toLong()), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        return NotificationCompat.Action.Builder(0, "Add a note", pendingIntent)
            .addRemoteInput(remoteInput)
            .build()
    }

    /** Launches [MainActivity] on tapping the notification body - the way to reach the
     * full check-in (more than 3 factors, or an intensity level for a LEVEL factor). */
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

        /** The 3 fixed factor-button slots in notification_interactive.xml, in display
         * order - RemoteViews needs static view IDs, so factors are bound into these by
         * position rather than one button being generated per factor. */
        private val QUICK_TOGGLE_BUTTON_IDS = listOf(
            R.id.factor_button_1,
            R.id.factor_button_2,
            R.id.factor_button_3
        )
    }
}
