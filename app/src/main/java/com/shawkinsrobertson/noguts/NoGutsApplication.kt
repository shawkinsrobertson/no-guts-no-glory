package com.shawkinsrobertson.noguts

import android.app.Application
import com.shawkinsrobertson.noguts.notifications.NotificationChannels

class NoGutsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
    }
}
