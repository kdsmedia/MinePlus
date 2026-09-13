package com.altomedia.mineplus.miner

import android.content.Context
import android.system.Os
import java.io.File

/**
 * Locates / materialises the bundled `x11miner` executable so the app can
 * actually run it, never pretend to.
 *
 * The binary is shipped in two places:
 *  * `assets/x11miner/<abi>/x11miner` — guaranteed to be packed by AGP
 *    (assets are always bundled) and executable after we grant +x;
 *  * `src/main/jniLibs/<abi>/x11miner` — the on-disk layout the release
 *    pipeline manages (see `native/build_recipe.sh`).
 */
object NativeMinerBinary {

    private const val ASSET_BASE = "x11miner"

    /**
     * Resolves a runnable `x11miner` for the current ABI. Copies the asset
     * into the app private dir (if needed) so it can be chmod +x and
     * executed. Returns the absolute path, or null when no binary exists.
     */
    fun resolve(context: Context): String? {
        val abi = currentAbi() ?: return null

        // 1. Already-extracted copy in private storage.
        File(context.filesDir, "x11miner-$abi").takeIf { it.exists() && it.canExecute() }?.let {
            return it.absolutePath
        }

        // 2. Packaged under ABI directories of jniLibs (post-build lib/).
        context.applicationInfo.nativeLibraryDir
            .takeIf { it.isNotBlank() }
            ?.let { File(it, "x11miner").takeIf { f -> f.exists() && f.canExecute() } }
            ?.let { return it.absolutePath }

        // 3. Copy from assets and mark executable.
        return try {
            val assetPath = "$ASSET_BASE/$abi/x11miner"
            val out = File(context.filesDir, "x11miner-$abi")
            context.assets.open(assetPath).use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
            if (!out.canExecute()) {
                // Grant +x; setExecutable is best-effort across OEM storage
                // quirks, so fall back to a direct chmod via Os.
                if (!out.setExecutable(true)) {
                    Os.chmod(out.absolutePath, 0b111101101 /* rwxr-xr-x */)
                }
            }
            out.absolutePath
        } catch (_: java.io.IOException) {
            null
        }
    }

    /** The first device ABI we have a binary for (arm64 preference). */
    private fun currentAbi(): String? {
        val preferredOrder = listOf("arm64-v8a", "armeabi-v7a")
        val supported = android.os.Build.SUPPORTED_ABIS.map { it.lowercase() }.toSet()
        val candidates = preferredOrder + android.os.Build.SUPPORTED_ABIS
        return candidates.firstOrNull { it in supported }
    }
}