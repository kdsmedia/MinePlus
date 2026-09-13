package com.altomedia.mineplus.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.altomedia.mineplus.data.SettingsRepository
import com.altomedia.mineplus.miner.MinerManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Starts mining when the device starts charging, when the user enabled
 * "Start automatically when charger connected" in AUTO START. The full
 * pre-start validation runs inside [MinerManager.start].
 */
@AndroidEntryPoint
class ChargerReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var manager: MinerManager

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_POWER_CONNECTED) return
        GlobalScope.launch(Dispatchers.IO) {
            if (settingsRepository.current().autoStartOnCharger) {
                manager.start()
            }
        }
    }
}