package com.nandomclaren.watery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.nandomclaren.watery.ui.SettingsScreen
import com.nandomclaren.watery.ui.theme.WateryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WateryTheme {
                SettingsScreen()
            }
        }
    }
}
