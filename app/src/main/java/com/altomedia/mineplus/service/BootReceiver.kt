package com.altomedia.mineplus.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.altomedia.mineplus.data.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.DelicateCoroutinesApi

/**
 * Restarts mining after a device reboot when the user enabled
 * "Start on boot" in the settings screen.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val settings = settingsRepository.current()
            if (settings.startOnBoot) {
                MinerService.start(context)
            }
        }
    }
}