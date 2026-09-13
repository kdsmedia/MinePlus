package com.altomedia.mineplus.miner

import com.altomedia.mineplus.stratum.StratumClient

/**
 * Handle to the running native miner process.
 *
 * Wraps the [MinerEngine] (which drives the JNI native nonce loop and the
 * X11 algorithm) together with its [StratumClient] session. The manager
 * exposes this handle through [MinerManager.getProcess] so components can
 * inspect the pipeline without owning its lifecycle.
 */
class MinerProcess(
    val engine: MinerEngine,
    val stratum: StratumClient,
    val parameters: MinerParameters
) {
    val connected: Boolean get() = stratum.connected
    val processId: Long = nextId

    /** Conceptual CLI invocation used to start this process, password masked. */
    val commandLine: String by lazy { parameters.toCommandLine(maskPassword = true) }

    companion object {
        private val counter = java.util.concurrent.atomic.AtomicLong(0)
        private val nextId: Long get() = counter.incrementAndGet()
    }
}