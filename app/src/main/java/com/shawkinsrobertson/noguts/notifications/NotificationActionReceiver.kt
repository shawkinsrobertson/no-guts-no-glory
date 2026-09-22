package com.shawkinsrobertson.noguts.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shawkinsrobertson.noguts.NoGutsApplication
import com.shawkinsrobertson.noguts.di.AppContainer
import com.shawkinsrobertson.noguts.data.repository.FactorLogInput
import com.shawkinsrobertson.noguts.scoring.InputType
import com.shawkinsrobertson.noguts.scoring.IntensityLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Handles every tap on the interactive daily notification (plan sections 19-22): toggling
 * one of the 3 quick factor buttons, one-tap "nothing notable", and the final save. Runs
 * entirely against Room + the scoring engine through [NoGutsApplication]'s container -
 * none of this depends on the app's UI process being alive.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val date = intent.getStringExtra(NotificationActions.EXTRA_DATE)
            ?.let { runCatching { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull() }
            ?: LocalDate.now()

        val pendingResult = goAsync()
        val container = (context.applicationContext as NoGutsApplication).container
        val notifier = container.dailyReminderNotifier

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    NotificationActions.ACTION_TOGGLE_FACTOR -> {
                        val factorId = intent.getLongExtra(NotificationActions.EXTRA_FACTOR_ID, -1L)
                        if (factorId != -1L) {
                            container.pendingNotificationLogRepository.toggleFactor(date, factorId)
                            notifier.refreshInteractive(date)
                        }
                    }

                    NotificationActions.ACTION_NOTHING_NOTABLE -> {
                        container.dailyLogRepository.saveZeroDay(date)
                        container.pendingNotificationLogRepository.clear(date)
                        postSavedResult(container, date)
                    }

                    NotificationActions.ACTION_SAVE -> {
                        val pending = container.pendingNotificationLogRepository.get(date)
                        val selectedFactors = container.factorRepository.getByIds(
                            pending?.selectedFactorIds ?: emptyList()
                        )
                        val inputs = selectedFactors.map { factor ->
                            FactorLogInput(
                                factorId = factor.id,
                                // The notification can't offer an intensity picker; a
                                // selected LEVEL factor logs at a sensible default.
                                level = if (factor.inputType == InputType.LEVEL) IntensityLevel.MODERATE else null
                            )
                        }
                        container.dailyLogRepository.saveDailyLog(date, inputs, pending?.notes)
                        container.pendingNotificationLogRepository.clear(date)
                        postSavedResult(container, date)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun postSavedResult(container: AppContainer, date: LocalDate) {
        val snapshot = container.database.scoreSnapshotDao().getByDate(date)
        val targetPoints = snapshot?.let {
            container.factorRepository.currentMaxPossibleDailyLoad() * it.targetPercent / 100.0
        }
        container.dailyReminderNotifier.postSavedResult(
            dailyPoints = snapshot?.dailyLoad,
            rolling72Points = snapshot?.rolling72Load,
            targetPoints = targetPoints
        )
    }
}
