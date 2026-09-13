package com.shawkinsrobertson.noguts.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** There is only one profile in V1, so this doubles as the app-wide settings model. */
data class UserProfile(
    val name: String = "",
    val photoUri: String? = null,
    val targetPercent: Double = 40.0,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val onboardingComplete: Boolean = false
)

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val NAME = stringPreferencesKey("name")
        val PHOTO_URI = stringPreferencesKey("photo_uri")
        val TARGET_PERCENT = doublePreferencesKey("target_percent")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val THEME = stringPreferencesKey("theme")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    val profile: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        UserProfile(
            name = prefs[Keys.NAME] ?: "",
            photoUri = prefs[Keys.PHOTO_URI],
            targetPercent = prefs[Keys.TARGET_PERCENT] ?: 40.0,
            reminderEnabled = prefs[Keys.REMINDER_ENABLED] ?: false,
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: 20,
            reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
            theme = prefs[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            onboardingComplete = prefs[Keys.ONBOARDING_COMPLETE] ?: false
        )
    }

    suspend fun updateName(name: String) {
        context.dataStore.edit { it[Keys.NAME] = name }
    }

    suspend fun updatePhotoUri(uri: String?) {
        context.dataStore.edit {
            if (uri != null) it[Keys.PHOTO_URI] = uri else it.remove(Keys.PHOTO_URI)
        }
    }

    suspend fun updateTargetPercent(percent: Double) {
        context.dataStore.edit { it[Keys.TARGET_PERCENT] = percent }
    }

    suspend fun updateReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.REMINDER_ENABLED] = enabled
            it[Keys.REMINDER_HOUR] = hour
            it[Keys.REMINDER_MINUTE] = minute
        }
    }

    suspend fun updateTheme(theme: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }
}
