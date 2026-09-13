package com.altomedia.mineplus.miner

import android.content.Context
import android.os.Build
import com.altomedia.mineplus.data.SettingsRepository
import com.altomedia.mineplus.model.MinerSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Result of validating the configuration + engine before starting. */
data class ValidationResult(
    val valid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * Pre-start validation gate (VALIDATE BEFORE START).
 *
 * Every requirement must pass *before* the pipeline is allowed to start.
 * In particular the miner engine is checked for real: if no X11 binary (or
 * the JNI engine) is present, the validation fails and the app must NOT
 * pretend to mine.
 */
@Singleton
class MinerValidator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    private val errors = mutableListOf<String>()
    private val warnings = mutableListOf<String>()

    /** Algorithms the engine supports. X11 only. */
    private val supportedAlgorithms = setOf("x11")

    /**
     * True only when a real `x11miner` executable is present for the current
     * ABI. The in-module JNI engine is NOT treated as "available": starting
     * a mining pipeline without the real binary would be pretending to mine,
     * which is never acceptable.
     */
    fun engineAvailable(): Boolean = findEngineBinary() != null

    /** Path to the bundled native `x11miner` executable for this ABI, or null. */
    fun engineBinaryPath(): String? = findEngineBinary()

    /** Runs every pre-start requirement against current settings. */
    suspend fun validate(): ValidationResult {
        errors.clear()
        warnings.clear()
        val s = settingsRepository.settings.first()

        checkAlgorithm(s)
        checkAbi()
        checkEngine()
        checkServer(s)
        checkPort(s)
        checkUsername(s)
        checkProtocol(s)

        return ValidationResult(errors.isEmpty(), errors.toList(), warnings.toList())
    }

    private fun checkAlgorithm(s: MinerSettings) {
        val algo = s.algorithm.trim().lowercase()
        if (!supportedAlgorithms.contains(algo)) {
            errors += "Algorithm '${s.algorithm}' is not supported (expected X11)."
        }
    }

    private fun checkAbi() {
        val abi = System.getProperty("os.arch")?.lowercase() ?: ""
        val supportedOnDevice =
            Build.SUPPORTED_ABIS.any { it in supportedAbis } ||
            abi.contains("aarch64") || abi.contains("arm")
        if (!supportedOnDevice) {
            errors += "ABI not supported on this device (${Build.SUPPORTED_ABIS.joinToString()})."
        }
    }

    private fun checkEngine() {
        val bin = engineBinaryPath()
        if (bin != null) {
            warnings += "Using bundled native miner: $bin"
        } else {
            errors += "Miner engine unavailable. Please install/provide a compatible X11 miner engine."
        }
    }

    private fun checkServer(s: MinerSettings) {
        val host = s.host.trim()
        when {
            host.isEmpty() -> errors += "Server not configured."
            !host.matches(Regex("^[a-zA-Z0-9.-]+$")) && !host.matches(IpAddress) ->
                errors += "Server address is invalid: '$host'."
        }
    }

    private fun checkPort(s: MinerSettings) {
        if (s.port !in 1..65535) {
            errors += "Port not configured (got ${s.port})."
        }
    }

    private fun checkUsername(s: MinerSettings) {
        if (s.login.isBlank()) {
            errors += "Username (wallet address) not configured."
        }
    }

    private fun checkProtocol(s: MinerSettings) {
        val protocol = s.endpoint.substringBefore("://")
        if (protocol !in setOf("stratum+tcp", "stratum+ssl")) {
            errors += "Connection protocol is invalid: '$protocol'."
        }
    }

    /** Finds a runnable `x11miner` for the current ABI (see [NativeMinerBinary]). */
    private fun findEngineBinary(): String? = NativeMinerBinary.resolve(context)

    companion object {
        private val IpAddress =
            Regex("^((25[0-5]|2[0-4][0-9]|1?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|1?[0-9][0-9]?)$")
    }
}