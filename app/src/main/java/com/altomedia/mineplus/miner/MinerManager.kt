package com.altomedia.mineplus.miner

import android.content.Context
import com.altomedia.mineplus.data.SettingsRepository
import com.altomedia.mineplus.model.LogLevel
import com.altomedia.mineplus.model.MinerLogEntry
import com.altomedia.mineplus.model.MinerState
import com.altomedia.mineplus.model.MinerStatus
import com.altomedia.mineplus.service.MinerService
import com.altomedia.mineplus.stratum.StratumClient
import dagger.hilt.android.qualifiers.ApplicationContext
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
 * Orchestrates the whole mining pipeline:
 *
 * ```
 * MinerService -> MinerManager -> Native Miner Process (MinerEngine)
 *                                     -> X11 Algorithm (native C)
 *                                     -> NiceHash Stratum (StratumClient)
 * ```
 *
 * The manager owns the service lifetime, the stratum session and the native
 * engine. It exposes the shared [state] and [logs] flows that the UI
 * observes. No hashing happens in Kotlin: nonce chunks travel through JNI
 * into the native X11 loop.
 */
@Singleton
class MinerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(MinerState(MinerStatus.STOPPED))
    val state: StateFlow<MinerState> = _state.asStateFlow()

    private val _logs = MutableStateFlow<List<MinerLogEntry>>(emptyList())
    val logs: StateFlow<List<MinerLogEntry>> = _logs.asStateFlow()

    @Volatile
    private var engine: MinerEngine? = null

    @Volatile
    private var stratum: StratumClient? = null

    @Volatile
    private var process: MinerProcess? = null

    @Volatile
    var isRunning: Boolean = false
        private set

    @Volatile
    private var endpoint: String = ""

    private fun log(level: LogLevel, message: String) {
        val entry = MinerLogEntry(System.currentTimeMillis(), level, message)
        _logs.value = (_logs.value + entry).takeLast(200)
        android.util.Log.d("MinePlus", "[${level.name}] $message")
    }

    /**
     * True when the native miner process has been spawned. Kept as a method
     * so the API contract reads naturally: `manager.isRunning()`.
     */
    fun isRunning(): Boolean = isRunning

    /**
     * Starts (or re-starts) the mining pipeline. Spins up the foreground
     * service, connects to the stratum endpoint and starts the native
     * process.
     */
    @Synchronized
    fun start() {
        if (isRunning) return
        isRunning = true
        log(LogLevel.INFO, "Starting miner…")

        MinerService.start(context)

        scope.launch {
            val settings = settingsRepository.current()
            endpoint = settings.endpoint

            // Native miner parameters (mirrors a CLI invocation).
            val params = MinerParameters.from(settings)
            log(LogLevel.INFO, "Native miner: ${params.toCommandLine(maskPassword = true)}")

            // Stratum <-> engine coupling is mediated through the manager.
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
                onShareResult = { accepted -> engine?.onShareResult(accepted) },
                log = ::log
            )

            val eng = MinerEngine(scope = scope, stratum = client, log = ::log)
            process = MinerProcess(eng, client, params)

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

    /** Stops the whole pipeline and the foreground service. */
    @Synchronized
    fun stop() {
        isRunning = false
        engine?.stop()
        stratum?.stop()
        engine = null
        stratum = null
        process = null
        _state.value = MinerState(MinerStatus.STOPPED)
        log(LogLevel.INFO, "Miner stopped")
        // Note: stopping the foreground service is owned by MinerService;
        // calling MinerService.stop() here would re-enter this action.
    }

    /** Restarts the pipeline (stop + start). */
    @Synchronized
    fun restart() {
        log(LogLevel.INFO, "Restarting miner…")
        stop()
        start()
    }

    /** Returns the running native process, if any. */
    fun getProcess(): MinerProcess? = process

    private suspend fun startStatsTicker(eng: MinerEngine) {
        var lastHashTotal = 0L
        var lastSample = System.currentTimeMillis()
        var startUptime = System.currentTimeMillis()

        while (scope.isActive && isRunning) {
            val now = System.currentTimeMillis()
            val hashes = eng.totalHashes
            val elapsed = (now - lastSample).coerceAtLeast(1L)

            val instRate = (hashes - lastHashTotal) * 1000.0 / elapsed
            _state.value = MinerState(
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
            lastHashTotal = hashes
            lastSample = now
            delay(1000)
        }
    }

    fun destroy() {
        stop()
        scope.cancel()
    }
}