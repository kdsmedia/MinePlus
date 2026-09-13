package com.altomedia.mineplus.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.mineplus.data.SettingsRepository
import com.altomedia.mineplus.model.MinerSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfigurationViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _settings = MutableStateFlow(MinerSettings())
    val settings: StateFlow<MinerSettings> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { _settings.value = it }
        }
    }

    fun update(transform: (MinerSettings) -> MinerSettings) {
        _settings.value = transform(_settings.value)
    }

    fun persist() {
        viewModelScope.launch {
            settingsRepository.save(_settings.value)
        }
    }
}