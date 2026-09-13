package com.altomedia.mineplus.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.mineplus.data.SettingsRepository
import com.altomedia.mineplus.miner.MinerManager
import com.altomedia.mineplus.miner.MinerValidator
import com.altomedia.mineplus.miner.ValidationResult
import com.altomedia.mineplus.model.MinerSettings
import com.altomedia.mineplus.service.MinerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val manager: MinerManager,
    private val settingsRepository: SettingsRepository,
    private val validator: MinerValidator,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val minerState = manager.state
    val logs = manager.logs
    val reconnectState = manager.reconnectState

    /** True when a real `x11miner` binary is present for this device/ABI. */
    val engineAvailable: Boolean get() = validator.engineAvailable()

    private val _validation = MutableStateFlow(ValidationResult(valid = false))
    val validation: StateFlow<ValidationResult> = _validation.asStateFlow()

    fun checkBeforeStart() {
        viewModelScope.launch {
            _validation.value = validator.validate()
        }
    }

    private val _settings = MutableStateFlow(MinerSettings())
    val settings: StateFlow<MinerSettings> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                _settings.value = s
            }
        }
    }

    fun startMining() {
        // Run the pre-start validation and surface failures in the UI, then
        // let MinerManager decide (it re-validates before starting).
        checkBeforeStart()
        manager.start()
    }

    fun stopMining() {
        manager.stop()
        MinerService.stop(appContext)
    }

    val isRunning: Boolean get() = manager.isRunning()

    fun toggleMining() {
        if (isRunning) stopMining() else startMining()
    }

    fun restartMining() {
        manager.restart()
    }

    fun clearMinerLogs() {
        manager.clearLogs()
    }
}