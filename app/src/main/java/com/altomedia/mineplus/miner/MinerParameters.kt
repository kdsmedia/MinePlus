package com.altomedia.mineplus.miner

import com.altomedia.mineplus.model.MinerSettings

/**
 * Configuration handed to the native miner process.
 *
 * Conceptually the embedded miner is launched like a CLI executable:
 *
 * ```
 * miner --algorithm x11
 *       --url stratum+ssl://x11.auto.nicehash.com:443
 *       --user USERNAME.WORKER
 *       --pass PASSWORD
 * ```
 *
 * IMPORTANT: flag names are NOT universal. ccminer uses `--algo` / `-a`,
 * cpuminer uses `--algo`, sgminer/bfgminer use `--algorithm`; URL/user/pass
 * are commonly `--url`/`-o`, `--user`/`-u`, `--pass`/`-p` but this still
 * varies per miner. All keys are therefore centralized here so the build
 * can target a specific X11 miner by adjusting one block.
 */
data class MinerParameters(
    val algorithm: String,
    val url: String,
    val user: String,
    val password: String
) {

    /** Renders as a command-line argument list (excluding the binary name). */
    fun toArguments(maskPassword: Boolean = false): List<String> {
        val pass = if (maskPassword && password.isNotEmpty()) "********" else password
        return listOf(
            KEY_ALGORITHM, algorithm,
            KEY_URL, url,
            KEY_USER, user,
            KEY_PASS, pass
        )
    }

    /** Renders the conceptual command line for logs/diagnostics. */
    fun toCommandLine(maskPassword: Boolean = true): String =
        (listOf(MINER_BINARY) + toArguments(maskPassword)).joinToString(" ")

    companion object {
        // The binary name as spawned by the process wrapper.
        const val MINER_BINARY: String = "miner"

        // Flag keys for the X11 miner this build targets.
        // MinePlus targets a sgminer-style miner:
        private val KEY_ALGORITHM: String = "--algorithm"
        private val KEY_URL: String = "--url"
        private val KEY_USER: String = "--user"
        private val KEY_PASS: String = "--pass"

        // Alternative flag sets for other common X11 miners.
        private val CCMINER_ALGORITHM: String = "--algo"
        private val CCMINER_URL: String = "-o"
        private val CCMINER_USER: String = "-u"
        private val CCMINER_PASS: String = "-p"

        fun from(settings: MinerSettings): MinerParameters = MinerParameters(
            algorithm = settings.algorithm.lowercase(),
            url = settings.endpoint,
            user = settings.login,
            password = settings.password
        )
    }
}