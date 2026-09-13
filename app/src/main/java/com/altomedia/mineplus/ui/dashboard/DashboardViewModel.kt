package com.altomedia.mineplus.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.mineplus.data.SettingsRepository
import com.altomedia.mineplus.di.MinerController
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
    private val controller: MinerController,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val minerState = controller.state
    val logs = controller.logs

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
        // The foreground service owns the miner process lifetime.
        MinerService.start(appContext)
    }

    fun stopMining() {
        MinerService.stop(appContext)
    }

    val isRunning: Boolean get() = controller.isRunning

    fun toggleMining() {
        if (isRunning) {
            MinerService.stop(appContext)
        } else {
            MinerService.start(appContext)
        }
    }
}