package com.shawkinsrobertson.noguts.notifications

/** Intent actions and extras shared between [DailyReminderNotifier] and [NotificationActionReceiver]. */
object NotificationActions {
    private const val PREFIX = "com.shawkinsrobertson.noguts.notifications.action"

    const val ACTION_TOGGLE_FACTOR = "$PREFIX.TOGGLE_FACTOR"
    const val ACTION_NOTHING_NOTABLE = "$PREFIX.NOTHING_NOTABLE"
    const val ACTION_SAVE = "$PREFIX.SAVE"

    const val EXTRA_DATE = "date"
    const val EXTRA_FACTOR_ID = "factorId"
}
