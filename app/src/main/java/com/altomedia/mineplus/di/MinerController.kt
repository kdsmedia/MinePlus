package com.altomedia.mineplus.di

import com.altomedia.mineplus.data.SettingsRepository
import com.altomedia.mineplus.miner.MinerEngine
import com.altomedia.mineplus.model.LogLevel
import com.altomedia.mineplus.model.MinerLogEntry
import com.altomedia.mineplus.model.MinerState
import com.altomedia.mineplus.model.MinerStatus
import com.altomedia.mineplus.stratum.StratumClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application-scoped controller owned by the foreground service.
 *
 * It wires the stratum client, the native mining engine and the shared
 * state exposed to the UI. The heavy hashing runs in native code; the
 * controller only coordinates and aggregates statistics.
 */
@Singleton
class MinerController @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(MinerState(MinerStatus.STOPPED))
    val state: StateFlow<MinerState> = _state.asStateFlow()

    private val _logs = MutableStateFlow<List<MinerLogEntry>>(emptyList())
    val logs: StateFlow<List<MinerLogEntry>> = _logs.asStateFlow()

    // Set up lazily so Hilt can construct us before sockets are needed.
    @Volatile
    private var engine: MinerEngine? = null

    @Volatile
    private var stratum: StratumClient? = null

    @Volatile
    var isRunning: Boolean = false
        private set

    /** Statics snapshot for the stats tick. */
    @Volatile
    private var endpoint: String = ""

    /** Callback used by the native loop and stratum client. */
    private fun log(level: LogLevel, message: String) {
        val entry = MinerLogEntry(System.currentTimeMillis(), level, message)
        _logs.value = (_logs.value + entry).takeLast(200)
        android.util.Log.d("MinePlus", "[${level.name}] $message")
    }

    @Synchronized
    fun start() {
        if (isRunning) return
        isRunning = true
        log(LogLevel.INFO, "Starting miner…")

        scope.launch {
            val settings = settingsRepository.current()
            endpoint = settings.endpoint

            // Resolve the circular dependency (client <-> engine) by making
            // the controller the mediator: the client forwards work/share
            // events to the controller, which owns the engine properties below.
            val client = StratumClient(
                scope = scope,
                settings = { settingsRepository.current() },
                onWork = { work ->
                    engine?.onNewWork(work)
                    _state.value = _state.value.copy(
                        status = if (isRunning) MinerStatus.MINING else MinerStatus.STOPPED,
                        poolEndpoint = endpoint
                    )
                },
                onShareResult = { accepted ->
                    engine?.onShareResult(accepted)
                },
                log = ::log
            )

            val eng = MinerEngine(
                scope = scope,
                stratum = client,
                log = ::log
            )

            stratum = client
            engine = eng

            _state.value = _state.value.copy(
                status = MinerStatus.CONNECTING,
                poolEndpoint = endpoint
            )

            eng.start()
            startStatsTicker(eng)
        }
    }

    @Synchronized
    fun stop() {
        isRunning = false
        engine?.stop()
        stratum?.stop()
        _state.value = MinerState(MinerStatus.STOPPED)
        log(LogLevel.INFO, "Miner stopped")
    }

    fun toggle(): Boolean {
        if (isRunning) stop() else start()
        return isRunning
    }

    private suspend fun startStatsTicker(eng: MinerEngine) {
        var lastHashTotal = 0L
        var lastSample = System.currentTimeMillis()
        var startUptime = System.currentTimeMillis()

        while (scope.isActive && isRunning) {
            val now = System.currentTimeMillis()
            val hashes = eng.totalHashes
            val elapsed = (now - lastSample).coerceAtLeast(1L)

            val instRate = (hashes - lastHashTotal) * 1000.0 / elapsed
            val state = MinerState(
                status = if (eng.isRunning) MinerStatus.MINING else MinerStatus.ERROR,
                hashrate = instRate.coerceAtLeast(0.0),
                acceptedShares = eng.acceptedCount,
                rejectedShares = eng.rejectedCount,
                uptimeSeconds = (now - startUptime) / 1000,
                difficulty = 0.0,
                poolEndpoint = endpoint,
                lastError = eng.errorMessage,
                connected = stratum?.connected ?: false
            )
            _state.value = state
            lastHashTotal = hashes
            lastSample = now
            delay(1000)
        }
    }

    fun destroy() {
        scope.cancel()
    }

    /** Allows the service to resolve the stratum connection status. */
    fun linkStratum(client: StratumClient) {
        stratum = client
    }
}