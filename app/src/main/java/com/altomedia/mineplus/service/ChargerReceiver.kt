package com.altomedia.mineplus.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.altomedia.mineplus.data.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Starts mining when the device starts charging, when the user enabled
 * "Start automatically when charger connected" in AUTO START.
 */
@AndroidEntryPoint
class ChargerReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_POWER_CONNECTED) return
        GlobalScope.launch(Dispatchers.IO) {
            val settings = settingsRepository.current()
            if (settings.autoStartOnCharger && !MinerService.isRunning()) {
                MinerService.start(context)
            }
        }
    }
}