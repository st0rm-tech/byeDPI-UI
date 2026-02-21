package io.github.dovecoteescapee.byedpi.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.dovecoteescapee.byedpi.ui.screens.SettingsScreen
import io.github.dovecoteescapee.byedpi.ui.theme.ByeDPITheme

class SettingsActivityCompose : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ByeDPITheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}
