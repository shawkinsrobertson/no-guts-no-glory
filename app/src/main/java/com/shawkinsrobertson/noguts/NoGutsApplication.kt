package com.shawkinsrobertson.noguts

import android.app.Application
import com.shawkinsrobertson.noguts.di.AppContainer
import com.shawkinsrobertson.noguts.notifications.NotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NoGutsApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
        applicationScope.launch {
            container.factorRepository.seedDefaultsIfEmpty()
        }
    }
}
