package io.github.dovecoteescapee.byedpi.ui.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.dovecoteescapee.byedpi.data.*
import io.github.dovecoteescapee.byedpi.services.appStatus
import io.github.dovecoteescapee.byedpi.utility.getPreferences
import io.github.dovecoteescapee.byedpi.utility.getStringNotNull
import io.github.dovecoteescapee.byedpi.utility.mode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val appStatus: AppStatus = AppStatus.Halted,
    val mode: Mode = Mode.VPN,
    val proxyAddress: String = "127.0.0.1:1080",
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private val TAG: String = MainViewModel::class.java.simpleName
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Received intent: ${intent?.action}")
            if (intent == null) {
                Log.w(TAG, "Received null intent")
                return
            }
            val senderOrd = intent.getIntExtra(SENDER, -1)
            val sender = Sender.entries.getOrNull(senderOrd)
            if (sender == null) {
                Log.w(TAG, "Received intent with unknown sender: $senderOrd")
                return
            }
            when (val action = intent.action) {
                STARTED_BROADCAST, STOPPED_BROADCAST -> updateStatus()
                FAILED_BROADCAST -> {
                    _uiState.update { it.copy(errorMessage = "Failed to start ${sender.name}") }
                    updateStatus()
                }
                else -> Log.w(TAG, "Unknown action: $action")
            }
        }
    }

    init {
        val intentFilter = IntentFilter().apply {
            addAction(STARTED_BROADCAST)
            addAction(STOPPED_BROADCAST)
            addAction(FAILED_BROADCAST)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getApplication<Application>().registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            getApplication<Application>().registerReceiver(receiver, intentFilter)
        }
        updateStatus()
    }

    fun updateStatus() {
        viewModelScope.launch {
            val (status, mode) = appStatus
            val preferences = getApplication<Application>().getPreferences()
            val proxyIp = preferences.getStringNotNull("byedpi_proxy_ip", "127.0.0.1")
            val proxyPort = preferences.getStringNotNull("byedpi_proxy_port", "1080")
            val currentMode = preferences.mode()
            _uiState.update {
                it.copy(appStatus = status, mode = mode ?: currentMode, proxyAddress = "$proxyIp:$proxyPort")
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(receiver)
    }
}
