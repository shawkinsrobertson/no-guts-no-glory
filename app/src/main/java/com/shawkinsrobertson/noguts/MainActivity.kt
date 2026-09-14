package com.shawkinsrobertson.noguts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.shawkinsrobertson.noguts.data.datastore.ThemeMode
import com.shawkinsrobertson.noguts.ui.LocalAppContainer
import com.shawkinsrobertson.noguts.ui.navigation.NoGutsNavHost
import com.shawkinsrobertson.noguts.ui.splash.BrandedLoadingScreen
import com.shawkinsrobertson.noguts.ui.theme.NoGutsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate() per the SplashScreen API contract. The system
        // splash it configures (Theme.NoGutsNoGlory.Starting) only ever shows the icon on
        // a background; the full branded screen below is this app's own first frame,
        // shown the moment Compose takes over.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as NoGutsApplication).container

        setContent {
            val profile by container.userPreferencesRepository.profile
                .collectAsState(initial = null)
            val loadedProfile = profile

            CompositionLocalProvider(LocalAppContainer provides container) {
                NoGutsTheme(themeMode = loadedProfile?.theme ?: ThemeMode.SYSTEM) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        // Wait for the first DataStore read before deciding where to start -
                        // otherwise a real "onboarding already done" user would flash onboarding.
                        // The branded screen fills that (usually brief) wait rather than a
                        // blank frame.
                        if (loadedProfile == null) {
                            BrandedLoadingScreen()
                        } else {
                            NoGutsNavHost(startAtOnboarding = !loadedProfile.onboardingComplete)
                        }
                    }
                }
            }
        }
    }
}
