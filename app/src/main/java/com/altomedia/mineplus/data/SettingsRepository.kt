package com.altomedia.mineplus.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.altomedia.mineplus.model.MinerSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "mineplus_settings")

private object Keys {
    val ALGORITHM = stringPreferencesKey("algorithm")
    val HOST = stringPreferencesKey("pool_host")
    val PORT = intPreferencesKey("pool_port")
    val USE_SSL = booleanPreferencesKey("pool_use_ssl")
    val WALLET = stringPreferencesKey("wallet_address")
    val RIG = stringPreferencesKey("rig_name")
    val PASSWORD = stringPreferencesKey("pool_password")
    val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
    val RECONNECT_INTERVAL = intPreferencesKey("reconnect_interval_sec")
    val MAX_RECONNECTS = intPreferencesKey("max_reconnects")
    val BACKGROUND_MINING = booleanPreferencesKey("background_mining")
    val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
    val THREADS = intPreferencesKey("threads")
}

/** Persisted application settings backed by DataStore. */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val settings: Flow<MinerSettings> = context.dataStore.data.map { p ->
        MinerSettings(
            algorithm = p[Keys.ALGORITHM] ?: "X11",
            host = p[Keys.HOST] ?: "x11.auto.nicehash.com",
            port = p[Keys.PORT] ?: 443,
            useSsl = p[Keys.USE_SSL] ?: true,
            walletAddress = p[Keys.WALLET] ?: "",
            rigName = p[Keys.RIG] ?: "ANDROID01",
            password = p[Keys.PASSWORD] ?: "",
            autoReconnect = p[Keys.AUTO_RECONNECT] ?: true,
            reconnectIntervalSec = p[Keys.RECONNECT_INTERVAL] ?: 10,
            maxReconnects = p[Keys.MAX_RECONNECTS] ?: 5,
            backgroundMining = p[Keys.BACKGROUND_MINING] ?: true,
            startOnBoot = p[Keys.START_ON_BOOT] ?: false,
            threads = p[Keys.THREADS] ?: 1
        )
    }

    suspend fun current(): MinerSettings = settings.first()

    suspend fun update(transform: (MinerSettings) -> MinerSettings) {
        val s = transform(current())
        context.dataStore.edit { p ->
            p[Keys.ALGORITHM] = s.algorithm
            p[Keys.HOST] = s.host
            p[Keys.PORT] = s.port
            p[Keys.USE_SSL] = s.useSsl
            p[Keys.WALLET] = s.walletAddress
            p[Keys.RIG] = s.rigName
            p[Keys.PASSWORD] = s.password
            p[Keys.AUTO_RECONNECT] = s.autoReconnect
            p[Keys.RECONNECT_INTERVAL] = s.reconnectIntervalSec
            p[Keys.MAX_RECONNECTS] = s.maxReconnects
            p[Keys.BACKGROUND_MINING] = s.backgroundMining
            p[Keys.START_ON_BOOT] = s.startOnBoot
            p[Keys.THREADS] = s.threads
        }
    }

    suspend fun save(s: MinerSettings) = update { s }
}