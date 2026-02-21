package io.github.dovecoteescapee.byedpi.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import io.github.dovecoteescapee.byedpi.fragments.MainSettingsFragment
import io.github.dovecoteescapee.byedpi.ui.theme.ByeDPITheme
import io.github.dovecoteescapee.byedpi.utility.getPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class MainActivityCompose : ComponentActivity() {
    companion object {
        private val TAG: String = MainActivityCompose::class.java.simpleName

        private fun collectLogs(): String? =
            try {
                Runtime.getRuntime().exec("logcat *:D -d").inputStream.bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to collect logs", e)
                null
            }
    }

    private val logsRegister = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        lifecycleScope.launch(Dispatchers.IO) {
            val logs = collectLogs()
            if (logs == null) {
                Log.e(TAG, "Failed to collect logs")
            } else {
                val uri = it.data?.data ?: run {
                    Log.e(TAG, "No data in result")
                    return@launch
                }
                contentResolver.openOutputStream(uri)?.use { stream ->
                    try {
                        stream.write(logs.toByteArray())
                    } catch (e: IOException) {
                        Log.e(TAG, "Failed to save logs", e)
                    }
                } ?: run {
                    Log.e(TAG, "Failed to open output stream")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val theme = getPreferences().getString("app_theme", null)
        MainSettingsFragment.setTheme(theme ?: "system")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        setContent { ByeDPIApp() }
    }

    @Composable
    private fun ByeDPIApp() {
        ByeDPITheme {
            io.github.dovecoteescapee.byedpi.ui.screens.MainScreen(
                onNavigateToSettings = {
                    val intent = Intent(this, SettingsActivityCompose::class.java)
                    startActivity(intent)
                },
                onSaveLogs = {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TITLE, "byedpi.log")
                    }
                    logsRegister.launch(intent)
                }
            )
        }
    }
}
