package com.shawkinsrobertson.noguts.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shawkinsrobertson.noguts.R
import com.shawkinsrobertson.noguts.ui.theme.SplashAccent
import com.shawkinsrobertson.noguts.ui.theme.SplashBackground
import com.shawkinsrobertson.noguts.ui.theme.SplashOnBackground

private data class Highlight(val icon: ImageVector, val label: String)

private val highlights = listOf(
    Highlight(Icons.Filled.Spa, "Know your\ntriggers"),
    Highlight(Icons.Filled.BarChart, "Spot\npatterns"),
    Highlight(Icons.Filled.Favorite, "Make better\nchoices"),
    Highlight(Icons.Filled.WbSunny, "More\ngood days")
)

/**
 * The app's own branded launch moment, shown in-app after the system splash hands off
 * (the OS only ever shows an icon on a background - it isn't the place for the full
 * branded screen with the mascot and tagline). This doubles as the loading state while
 * the first DataStore read resolves, so there's no artificial delay: it's on screen for
 * exactly as long as that genuinely takes.
 */
@Composable
fun BrandedLoadingScreen() {
    Box(modifier = Modifier.fillMaxSize().background(SplashBackground), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(160.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "NO GUTS NO GLORY",
                color = SplashOnBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(3.dp)
                    .background(SplashAccent)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Track the stress to feel your best",
                color = SplashOnBackground.copy(alpha = 0.85f),
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(36.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                highlights.forEach { highlight ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(highlight.icon, contentDescription = null, tint = SplashOnBackground)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            highlight.label,
                            color = SplashOnBackground.copy(alpha = 0.85f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
            CircularProgressIndicator(color = SplashOnBackground, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}
