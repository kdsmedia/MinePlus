package com.altomedia.mineplus.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.altomedia.mineplus.data.SettingsRepository
import com.altomedia.mineplus.miner.MinerManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.DelicateCoroutinesApi

/**
 * Restarts mining after a device reboot when the user enabled
 * "Start on boot" in the settings screen. The full pre-start validation
 * (engine present, configuration valid) runs inside [MinerManager.start].
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var manager: MinerManager

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (settingsRepository.current().startOnBoot) {
                manager.start()
            }
        }
    }
}