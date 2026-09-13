package com.altomedia.mineplus.stratum

import com.altomedia.mineplus.miner.MinerEngine
import com.altomedia.mineplus.model.LogLevel
import com.altomedia.mineplus.model.MinerSettings
import com.altomedia.mineplus.model.StratumWork
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import java.security.KeyStore

/**
 * Minimal but complete Stratum (BTC-style `mining.*`) client for TCP and
 * SSL connections to NiceHash. Uses coroutines for the reader loop and
 * sequential JSON-RPC request/response matching.
 */
class StratumClient(
    private val scope: CoroutineScope,
    private val settings: () -> MinerSettings,
    private val onWork: (StratumWork) -> Unit = {},
    private val onShareResult: (Boolean) -> Unit = {},
    private val onConnectionChanged: (Boolean) -> Unit = {},
    private val log: (LogLevel, String) -> Unit = { _, _ -> }
) {
    private val gson = Gson()

    @Volatile
    private var running = false
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private var socket: Socket? = null

    private var workJob: Job? = null
    private val requestMutex = Mutex()
    private val responseChannel = Channel<JsonElement>(Channel.UNLIMITED)

    // Stratum session state
    @Volatile
    private var subscribed = false
    private var lastJob: StratumWork? = null
    private var nextRequestId = 0

    @Volatile
    var connected: Boolean = false
        private set

    private fun nextId(): Int = ++nextRequestId

    suspend fun start() {
        if (running) return
        running = true
        val s = settings()
        log(LogLevel.INFO, "Connecting to ${s.endpoint}")
        workJob = scope.launch(Dispatchers.IO) { connectAndRun(s) }
    }

    suspend fun stop() {
        running = false
        workJob?.cancel()
        closeSocket()
        if (connected) {
            connected = false
            onConnectionChanged(false)
        }
    }

    private suspend fun connectAndRun(settings: MinerSettings) {
        while (running && scope.isActive) {
            try {
                log(LogLevel.INFO, "Connecting…")
                openConnection(settings)
                connected = true
                log(LogLevel.INFO, "Connected to ${settings.host}:${settings.port}")
                onConnectionChanged(true)

                // Subscribe and authorize
                subscribe()
                if (settings.login.isNotEmpty()) authorize(settings.login, settings.password)

                // Launch the notification listener
                scope.launch(Dispatchers.IO) { listenForMessages() }

                // Keep the connection alive and scan for new work
                runLoop(settings)
            } catch (t: Throwable) {
                log(LogLevel.ERROR, "Connection error: ${t.message}")
                if (connected) {
                    connected = false
                    onConnectionChanged(false)
                }
                closeSocket()

                if (!settings.autoReconnect) {
                    running = false
                    break
                }
                delay(3000)
            }
        }
        connected = false
    }

    private fun openConnection(settings: MinerSettings) {
        val raw: Socket = if (settings.useSsl) {
            val sslContext = sslContext()
            sslContext.socketFactory.createSocket() as javax.net.ssl.SSLSocket
        } else {
            Socket()
        }

        raw.connect(InetSocketAddress(settings.host, settings.port), 15_000)
        socket = raw
        reader = BufferedReader(
            InputStreamReader(raw.getInputStream(), StandardCharsets.UTF_8)
        )
        writer = BufferedWriter(
            OutputStreamWriter(raw.getOutputStream(), StandardCharsets.UTF_8)
        )
    }

    private fun sslContext(): SSLContext {
        val tms = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tms.init(null as KeyStore?)
        val ctx = SSLContext.getInstance("TLS")
        ctx.init(null, tms.trustManagers, null)
        return ctx
    }

    private suspend fun subscribe() {
        send("mining.subscribe", listOf("MinePlus/1.0"))
        // Wait for subscription result (drain a few responses)
        drainUntilResult()
        subscribed = true
    }

    private suspend fun authorize(login: String, password: String = "") {
        send("mining.authorize", listOf(login, password))
        drainUntilResult()
    }

    private suspend fun runLoop(settings: MinerSettings) {
        while (running && scope.isActive && connected) {
            // Wait for new work or respond to server pings.
            if (lastJob == null) {
                log(LogLevel.DEBUG, "Requesting work")
                send("mining.extranonce.subscribe", listOf())
                delay(500)
            } else {
                delay(100)
            }
        }
    }

    /**
     * Reads one JSON line at a time, dispatches responses and notifications.
     */
    private suspend fun listenForMessages() {
        val r = reader ?: return
        while (running && scope.isActive) {
            val line = r.readLine() ?: break
            if (line.isBlank()) continue
            try {
                val element = gson.fromJson(line, JsonElement::class.java)
                if (element.isJsonObject) {
                    val obj = element.asJsonObject
                    when {
                        obj.has("id") && obj.get("id") is JsonNull ->
                            handleNotification(obj)
                        obj.has("id") && obj.get("id").isJsonPrimitive ->
                            responseChannel.trySend(obj)
                    }
                }
            } catch (t: Throwable) {
                log(LogLevel.ERROR, "Malformed stratum message: ${t.message}")
            }
        }
    }

    private suspend fun handleNotification(obj: JsonObject) {
        val method = obj.getAsJsonPrimitive("method")?.asString ?: return
        val params = obj.getAsJsonArray("params")
        when (method) {
            "mining.notify" -> handleNotify(params)
            "mining.set_difficulty" -> handleDifficulty(params)
            "mining.set_extranonce" -> log(LogLevel.DEBUG, "Extranonce set")
            else -> log(LogLevel.DEBUG, "Unknown notification $method")
        }
    }

    private fun handleNotify(params: JsonArray?) {
        if (params == null || params.size() < 9) return
        val work = StratumWork(
            jobId = params[0].asString,
            prevHash = params[1].asString,
            coinb1 = params[2].asString,
            coinb2 = params[3].asString,
            merkleBranch = (params[4] as JsonArray).map { it.asString },
            version = params[5].asString.toLong(16),
            nbits = params[6].asString,
            ntime = params[7].asString,
            cleanJobs = params[8].asBoolean
        )
        lastJob = work
        onWork(work)
    }

    private fun handleDifficulty(params: JsonArray?) {
        if (params != null && params.size() > 0) {
            val d = params[0].asDouble
            lastJob = lastJob?.copy(difficulty = d)
        }
    }

    private suspend fun drainUntilResult() {
        // Responses arrive through the channel; we just need to consume the
        // subscribe/authorize acknowledgements.
        repeat(2) {
            responseChannel.receiveCatching().getOrNull()
        }
    }

    private suspend fun send(method: String, params: List<Any>): Int {
        requestMutex.withLock {
            val id = nextId()
            val request = JsonObject()
            request.addProperty("id", id)
            request.addProperty("method", method)
            request.add("params", gson.toJsonTree(params))
            writer?.write(gson.toJson(request) + "\n")
            writer?.flush()
            return id
        }
    }

    /** Submits a share for the current job and reports the result. */
    suspend fun submitShare(work: StratumWork, nonce: Long, digestHex: String) {
        val login = settings().login
        val params = listOf(
            login,
            work.jobId,
            work.ntime,
            work.nbits,
            "%08x".format(nonce)
        )
        val id = send("mining.submit", params)
        log(LogLevel.INFO, "Share submitted: ${work.jobId} nonce=$nonce")

        // A quick attempt to read the server response for the submission.
        val submittedId = findResponse(id)
        val dict = submittedId ?: return
        if (dict.isJsonObject) {
            val res = dict.asJsonObject.get("result")
            if (res != null && !res.isJsonNull) {
                val accepted = if (res.isJsonPrimitive && res.asJsonPrimitive.isBoolean) {
                    res.asBoolean
                } else if (res.isJsonArray && (res.asJsonArray) != null) {
                    res.asJsonArray.firstOrNull()?.let { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean && it.asBoolean } ?: true
                } else {
                    true
                }
                onShareResult(accepted)
            }
        }
    }

    /** Finds and removes a response with the given id from the channel. */
    private suspend fun findResponse(id: Int): JsonElement? {
        // The channel is processed sequentially; we scan a bounded window.
        repeat(4) {
            val el = responseChannel.receiveCatching().getOrNull() ?: return null
            val obj = el.takeIf { it.isJsonObject }?.asJsonObject ?: continue
            val rid = obj.get("id")?.takeIf { !it.isJsonNull }?.asInt
            if (rid == id) return obj
        }
        return null
    }

    fun requestWork() {
        scope.launch(Dispatchers.IO) {
            send("mining.extranonce.subscribe", listOf())
        }
    }

    private fun closeSocket() {
        try {
            writer?.close()
        } catch (_: Exception) {
        }
        try {
            reader?.close()
        } catch (_: Exception) {
        }
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
        reader = null
        writer = null
    }
}