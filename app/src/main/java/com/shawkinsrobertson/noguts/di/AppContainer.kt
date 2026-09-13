package com.shawkinsrobertson.noguts.di

import android.content.Context
import com.shawkinsrobertson.noguts.data.datastore.UserPreferencesRepository
import com.shawkinsrobertson.noguts.data.db.AppDatabase
import com.shawkinsrobertson.noguts.data.repository.DailyLogRepository
import com.shawkinsrobertson.noguts.data.repository.FactorRepository
import com.shawkinsrobertson.noguts.data.repository.PendingNotificationLogRepository
import com.shawkinsrobertson.noguts.notifications.DailyReminderNotifier

/**
 * Hand-rolled dependency container. A DI framework (Hilt/Koin) would be overkill for an
 * app this size, and Hilt's Gradle plugin pulls from a repository this build environment
 * can't always reach - plain constructor injection keeps the dependency graph explicit
 * and easy to follow.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(appContext)
    }

    val factorRepository: FactorRepository by lazy {
        FactorRepository(database.factorDao())
    }

    val dailyLogRepository: DailyLogRepository by lazy {
        DailyLogRepository(
            dailyLogDao = database.dailyLogDao(),
            dailyLogFactorDao = database.dailyLogFactorDao(),
            scoreSnapshotDao = database.scoreSnapshotDao(),
            factorRepository = factorRepository,
            userPreferencesRepository = userPreferencesRepository
        )
    }

    val pendingNotificationLogRepository: PendingNotificationLogRepository by lazy {
        PendingNotificationLogRepository(database.pendingNotificationLogDao())
    }

    val dailyReminderNotifier: DailyReminderNotifier by lazy {
        DailyReminderNotifier(appContext, factorRepository, database.dailyLogFactorDao(), pendingNotificationLogRepository)
    }
}
