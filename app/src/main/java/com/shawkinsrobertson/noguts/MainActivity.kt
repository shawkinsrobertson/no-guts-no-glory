package com.shawkinsrobertson.noguts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

// Placeholder content; replaced by the full navigation graph once the app shell
// (theme, nav, onboarding) lands.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    Text("No Guts No Glory")
                }
            }
        }
    }
}
