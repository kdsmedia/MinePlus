package com.altomedia.mineplus.miner

import com.altomedia.mineplus.model.LogLevel
import com.altomedia.mineplus.model.StratumWork
import com.altomedia.mineplus.stratum.StratumClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.math.BigInteger
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicLong

/**
 * Cotroller that feeds the native X11 miner with stratum jobs and keeps
 * the statistics that the dashboard observes.
 *
 * Hashing never happens in Kotlin: the nonce loop is executed by the
 * native library over large chunks.
 */
class MinerEngine(
    private val scope: CoroutineScope,
    private val stratum: StratumClient,
    private val log: (LogLevel, String) -> Unit = { _, _ -> }
) {
    /** Game loop bookkeeping. */
    private val nativeMutex = Mutex()
    private var job: Job? = null
    private var submitJob: Job? = null
    private val currentNonce = AtomicLong(0L)
    private val hashesDone = AtomicLong(0L)
    private val chunkSize = 1 shl 18  // 262,144 nonces per native call

    // Queue of found shares; drained by [submitShareLoop].
    private data class FoundShare(val work: StratumWork, val nonce: Long, val digestHex: String)
    private val shareChannel = Channel<FoundShare>(Channel.UNLIMITED)

    // Stats observable by the UI
    private val acceptedShares = AtomicLong(0L)
    private val rejectedShares = AtomicLong(0L)
    private val lastError = java.util.concurrent.atomic.AtomicReference<String?>(null)
    private val lastShareAtMs = AtomicLong(0L)
    private var startedAt = System.currentTimeMillis()

    @Volatile
    private var currentWork: StratumWork? = null

    private var currentTarget: ByteArray = heaviestTarget()

    @Volatile
    var isRunning = false
        private set

    val acceptedCount: Long get() = acceptedShares.get()
    val rejectedCount: Long get() = rejectedShares.get()
    val totalHashes: Long get() = hashesDone.get()
    val errorMessage: String? get() = lastError.get()
    val uptimeMs: Long get() = if (isRunning) System.currentTimeMillis() - startedAt else 0
    val lastShareAtMsValue: Long get() = lastShareAtMs.get()

    /** Registers a successful share for statistics. */
    fun onShareAccepted() {
        lastShareAtMs.set(System.currentTimeMillis())
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        startedAt = System.currentTimeMillis()
        hashesDone.set(0)
        acceptedShares.set(0)
        rejectedShares.set(0)
        lastError.set(null)
        job = scope.launch(Dispatchers.Default) { runLoop() }
        submitJob = scope.launch(Dispatchers.IO) { submitShareLoop() }
    }

    fun stop() = scope.launch {
        val j = job ?: return@launch
        isRunning = false
        j.cancelAndJoin()
        submitJob?.cancelAndJoin()
    }

    private suspend fun submitShareLoop() {
        for (share in shareChannel) {
            try {
                stratum.submitShare(share.work, share.nonce, share.digestHex)
            } catch (t: Throwable) {
                log(LogLevel.ERROR, "Share submission failed: ${t.message}")
            }
        }
    }

    /** Bridge used by the stratum client when a new job arrives. */
    fun onNewWork(work: StratumWork) {
        currentWork = work
        // Reset the nonce counter for every clean job start.
        currentNonce.set(0L)
        currentTarget = difficultyTarget(work.difficulty.coerceAtLeast(0.00001))
        log(LogLevel.INFO, "New job ${work.jobId} diff=${work.difficulty}")
    }

    /** Result of a submitted share, forwarded from the stratum client. */
    fun onShareResult(accepted: Boolean) {
        if (accepted) {
            acceptedShares.incrementAndGet()
            onShareAccepted()
        } else {
            rejectedShares.incrementAndGet()
        }
        log(if (accepted) LogLevel.INFO else LogLevel.WARN,
            if (accepted) "Share accepted" else "Share rejected")
    }

    /**
     * Builds the 80-byte block header for the current job and mines it in
     * the native loop until a share is found or the job rotates.
     */
    private suspend fun runLoop() {
        try {
            while (isActive && isRunning) {
                val work = currentWork ?: run {
                    delay(200)
                    continue@while
                }

                val header = buildHeader(work)
                val startNonce = currentNonce.getAndAdd(chunkSize.toLong())
                val count = chunkSize.toLong()

                val iterations =
                    async(Dispatchers.IO) {
                        nativeMutex.withLock {
                            NativeMiner.mine(
                                header = header,
                                target = currentTarget,
                                nonceStart = startNonce,
                                count = count
                            ) { nonce, digest ->
                                shareChannel.trySend(
                                    FoundShare(work, nonce, digest.toHex())
                                )
                            }
                        }
                    }

                val done = iterations.await()
                hashesDone.addAndGet(done)

                if (done == 0L) {
                    // Reached the end of the current job; request a fresh one.
                    log(LogLevel.DEBUG, "Job ${work.jobId} scanned, requesting more work")
                    stratum.requestWork()
                    delay(50)
                }
            }
        } catch (t: Throwable) {
            lastError.set(t.message ?: t.javaClass.simpleName)
            log(LogLevel.ERROR, "Miner loop failed: ${t.message}")
            isRunning = false
        }
    }

    private fun buildHeader(work: StratumWork): ByteArray {
        // 80-byte block header: version(4) + prevhash(32) + merkle_root(32)
        // + ntime(4) + nbits(4) + nonce(4).
        val root = calculateMerkleRoot(work)
        val header = ByteArray(80)
        writeInt32Be(header, 0, work.version.toInt())
        // Stratum ships prevhash in byte-reversed (internal) order; the
        // header expects the natural byte order of the previous block hash.
        hexToBytes(work.prevHash).reversedArray().copyInto(header, 4)
        root.copyInto(header, 36)
        writeInt32Be(header, 68, hexToInt(work.ntime))
        writeInt32Be(header, 72, hexToInt(work.nbits))
        return header
    }

    private fun calculateMerkleRoot(work: StratumWork): ByteArray {
        var root = doubleSha256(hexToBytes(work.coinb1 + work.coinb2))
        for (branch in work.merkleBranch) {
            // Merkle branch hashes are delivered byte-reversed; converting
            // them to the internal byte order keeps concatenation correct.
            root = doubleSha256(root + hexToBytes(branch).reversedArray())
        }
        return root
    }

    private fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)

    private fun doubleSha256(data: ByteArray): ByteArray = sha256(sha256(data))

    private fun writeInt32Be(dst: ByteArray, offset: Int, value: Int) {
        dst[offset] = (value ushr 24).toByte()
        dst[offset + 1] = (value ushr 16).toByte()
        dst[offset + 2] = (value ushr 8).toByte()
        dst[offset + 3] = value.toByte()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.removePrefix("0x")
        val out = ByteArray(clean.length / 2)
        for (i in out.indices) {
            out[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return out
    }

    private fun hexToInt(hex: String): Int {
        val clean = hex.removePrefix("0x")
        return clean.toLong(16).toInt()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    companion object {
        /**
         * Converts a stratum difficulty value to the 256-bit big-endian target.
         * Target = 0xFFFF0000... (256 bits) / difficulty.
         */
        fun difficultyTarget(difficulty: Double): ByteArray {
            val maxTarget = BigInteger("00000000FFFF0000000000000000000000000000000000000000000000000000", 16)
            var d = difficulty
            if (d <= 0.0) d = 1.0
            val target = maxTarget.divide(BigDecimal(d).toBigInteger())
            val bytes = target.toByteArray()
            val out = ByteArray(32)
            // BigInteger.toByteArray may produce a leading 0x00 sign byte.
            val copyLen = minOf(bytes.size, 32)
            System.arraycopy(bytes, bytes.size - copyLen, out, 32 - copyLen, copyLen)
            return out
        }

        fun heaviestTarget(): ByteArray =
            ByteArray(32) { 0xFF.toByte() }
    }
}

private fun ByteArray?.toHexString(): String = this?.joinToString("") { "%02x".format(it) } ?: ""