package com.altomedia.mineplus.miner

import android.content.Context
import com.altomedia.mineplus.data.SettingsRepository
import com.altomedia.mineplus.model.LogLevel
import com.altomedia.mineplus.model.MinerLogEntry
import com.altomedia.mineplus.model.MinerState
import com.altomedia.mineplus.model.MinerStatus
import com.altomedia.mineplus.model.ReconnectState
import com.altomedia.mineplus.service.MinerNotifier
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
    private val settingsRepository: SettingsRepository,
    private val notifier: MinerNotifier
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val rejectWarned = java.util.concurrent.atomic.AtomicBoolean(false)
    private val highRejectThresholdPercent = 5.0

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

    /** Reconnect bookkeeping shared with the UI. */
    private val _reconnectState = MutableStateFlow(
        ReconnectState(idle = true, retries = 0, nextRetryInSec = 0)
    )
    val reconnectState: StateFlow<ReconnectState> = _reconnectState.asStateFlow()

    private fun log(level: LogLevel, message: String) {
        val entry = MinerLogEntry(System.currentTimeMillis(), level, message)
        _logs.value = (_logs.value + entry).takeLast(200)
        android.util.Log.d("MinePlus", "[${level.name}] $message")
    }

    private fun notifyConnectionEstablished() {
        log(LogLevel.INFO, "Connection established")
        notifier.post("Connection established", "NiceHash X11")
    }

    private fun notifyConnectionLost() {
        log(LogLevel.WARN, "Connection lost")
        notifier.post("Connection lost", "Reconnecting…")
    }

    private fun notifyHighRejectRate(percent: Double) {
        notifier.post("High reject rate", String.format("%.1f%% of shares rejected", percent))
    }

    /** Emits a single "high reject rate" event when the threshold is crossed. */
    private fun maybeWarnHighReject(pendingState: MinerState) {
        val rate = pendingState.rejectRatePercent
        if (rate > highRejectThresholdPercent && rejectWarned.compareAndSet(false, true)) {
            notifyHighRejectRate(rate)
        } else if (rate <= highRejectThresholdPercent) {
            rejectWarned.set(false)
        }
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
        val proto = if (settingsRepository.current().useSsl) "SSL" else "TCP"
        notifier.post("Mining started", "NiceHash X11 • $proto")
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
                onConnectionChanged = { isConnected ->
                    if (isConnected) notifyConnectionEstablished() else notifyConnectionLost()
                },
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
            startReconnectMonitor(eng)
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
        notifier.post("Mining stopped")
        // Note: stopping the foreground service is owned by MinerService;
        // calling MinerService.stop() here would re-enter this action.
    }

    /** Restarts the pipeline (stop + start). */
    @Synchronized
    fun restart() {
        log(LogLevel.INFO, "Restarting miner…")
        notifier.post("Miner restarted", "NiceHash X11")
        stop()
        start()
    }

    /** Returns the running native process, if any. */
    fun getProcess(): MinerProcess? = process

    /** Clears the in-app log. */
    fun clearLogs() {
        _logs.value = emptyList()
    }

    private suspend fun startStatsTicker(eng: MinerEngine) {
        var lastHashTotal = 0L
        var lastSample = System.currentTimeMillis()
        var startUptime = System.currentTimeMillis()
        val history = ArrayDeque<Double>()
        var sampleCount = 0L
        var rateSum = 0.0

        while (scope.isActive && isRunning && eng === engine) {
            val now = System.currentTimeMillis()
            val hashes = eng.totalHashes
            val elapsed = (now - lastSample).coerceAtLeast(1L)

            val instRate = (hashes - lastHashTotal) * 1000.0 / elapsed
            history.addLast(instRate.coerceAtLeast(0.0))
            if (history.size > HISTORY_POINTS) history.removeFirst()
            sampleCount++
            rateSum += instRate.coerceAtLeast(0.0)

            val accepted = eng.acceptedCount
            val lastShareAgo = if (eng.lastShareAtMsValue > 0L)
                (now - eng.lastShareAtMsValue) / 1000 else Long.MAX_VALUE

            _state.value = MinerState(
                status = if (eng.isRunning) MinerStatus.MINING else MinerStatus.ERROR,
                hashrate = instRate.coerceAtLeast(0.0),
                averageHashrate = if (sampleCount > 0) rateSum / sampleCount else 0.0,
                acceptedShares = accepted,
                rejectedShares = eng.rejectedCount,
                uptimeSeconds = (now - startUptime) / 1000,
                difficulty = 0.0,
                poolEndpoint = endpoint,
                lastError = eng.errorMessage,
                connected = stratum?.connected ?: false,
                sharesPerHour = if (accepted > 0 && (now - startUptime) > 0) {
                    accepted * 3600.0 / ((now - startUptime) / 1000.0)
                } else 0.0,
                lastShareSecondsAgo = lastShareAgo,
                hashrateHistory = history.toList()
            ).let {
                maybeWarnHighReject(pendingState = it)
                it
            }
            lastHashTotal = hashes
            lastSample = now
            delay(1000)
        }
    }

    companion object {
        private const val HISTORY_POINTS = 30  // 30 × ~1s samples
    }

    /**
     * Watches the stratum/engine and drives the reconnect loop:
     *
     * ```
     * Mining -> Connection lost -> Stop current process
     *         -> Wait interval -> Restart miner -> Connect
     * ```
     *
     * When [MinerSettings.autoReconnect] is enabled, a dropped connection
     * stops the process, waits [MinerSettings.reconnectIntervalSec], then
     * restarts, up to [MinerSettings.maxReconnects] attempts.
     */
    private suspend fun startReconnectMonitor(eng: MinerEngine) {
        val settings = settingsRepository.current()
        if (!settings.autoReconnect) return

        var retries = 0
        var wasConnected = false
        val maxRetries = settings.maxReconnects.coerceAtLeast(1)
        while (scope.isActive && isRunning && eng === engine) {
            val connected = stratum?.connected ?: false
            if (!connected && !wasConnected) {
                // Still in the initial connect phase — don't count retries yet.
                delay(1000)
                continue
            }
            if (!connected) {
                retries++
                _reconnectState.value = ReconnectState(
                    idle = false,
                    retries = retries,
                    nextRetryInSec = settings.reconnectIntervalSec,
                    maxRetries = maxRetries,
                    message = "Connection lost"
                )
                log(LogLevel.WARN, "Connection lost (attempt $retries/$maxRetries)")

                // Stop current process
                eng.stop()

                // Wait for the configured interval, emitting a countdown.
                var wait = settings.reconnectIntervalSec
                _state.value = _state.value.copy(status = MinerStatus.ERROR)
                while (wait > 0 && isRunning && eng === engine) {
                    _reconnectState.value = _reconnectState.value.copy(
                        message = "Retrying in $wait sec…",
                        nextRetryInSec = wait
                    )
                    delay(1000)
                    wait--
                }
                if (wait <= 0) {
                    if (retries >= maxRetries) {
                        _reconnectState.value = _reconnectState.value.copy(
                            message = "Gave up after $maxRetries attempts",
                            gaveUp = true
                        )
                        log(LogLevel.ERROR, "Gave up after $maxRetries reconnect attempts")
                        _state.value = _state.value.copy(status = MinerStatus.ERROR)
                        notifier.post("Miner error", "Gave up reconnecting after $maxRetries attempts")
                        break
                    }
                    restartPipeline()
                    retries = 0
                }
            } else {
                wasConnected = true
                if (_reconnectState.value.reconnecting || _reconnectState.value.retries > 0) {
                    _reconnectState.value = ReconnectState(idle = true)
                    log(LogLevel.INFO, "Connection restored")
                }
                delay(1000)
            }
        }
        _reconnectState.value = ReconnectState(idle = true)
    }

    private fun restartPipeline() {
        log(LogLevel.INFO, "Restarting miner (auto-reconnect)…")
        notifier.post("Miner restarted", "Reconnecting to pool")
        // Stop the abandoned process object and spin a fresh pipeline.
        stratum?.stop()
        engine?.stop()
        process = null
        isRunning = false
        start()
    }

    fun destroy() {
        stop()
        scope.cancel()
    }
}