package com.shawkinsrobertson.noguts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.shawkinsrobertson.noguts.data.datastore.ThemeMode
import com.shawkinsrobertson.noguts.data.datastore.UserProfile
import com.shawkinsrobertson.noguts.ui.LocalAppContainer
import com.shawkinsrobertson.noguts.ui.navigation.NoGutsNavHost
import com.shawkinsrobertson.noguts.ui.theme.NoGutsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as NoGutsApplication).container

        setContent {
            val profile by container.userPreferencesRepository.profile
                .collectAsState(initial = null)

            CompositionLocalProvider(LocalAppContainer provides container) {
                NoGutsTheme(themeMode = profile?.theme ?: ThemeMode.SYSTEM) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        // Wait for the first DataStore read before deciding where to start -
                        // otherwise a real "onboarding already done" user would flash onboarding.
                        val loadedProfile: UserProfile? = profile
                        if (loadedProfile != null) {
                            NoGutsNavHost(startAtOnboarding = !loadedProfile.onboardingComplete)
                        }
                    }
                }
            }
        }
    }
}
