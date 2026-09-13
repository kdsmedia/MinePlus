package com.altomedia.mineplus.miner

import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.PortUnreachableException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

/**
 * Maps technical exceptions/statuses into user-friendly messages shown in
 * the app. Never exposes credentials, tokens or the raw server payload in
 * the messages (see security policy: no sensitive data in error output).
 */
object MinerErrorMapper {

    /** Preferred human-readable message for a thrown error. */
    fun friendly(throwable: Throwable): String = when (throwable) {
        is SocketTimeoutException -> "Connection timed out. Check your internet connection and try again."
        is UnknownHostException -> "Server not found. Check the mining pool address."
        is ConnectException ->
            if (throwable.message?.contains("refused", ignoreCase = true) == true)
                "Connection refused by the mining pool. Check the server and port."
            else
                "Could not connect to the mining pool. Check your internet connection."
        is NoRouteToHostException -> "Could not reach the mining pool. Check the server address."
        is PortUnreachableException -> "The mining pool port is unreachable. Check the configured port."
        is SocketException -> "The connection to the mining pool was interrupted."
        is SSLHandshakeException -> "Secure connection failed. Check the SSL setting for this pool."
        is SSLException -> "Secure connection failed. Check the SSL setting for this pool."
        is IOException -> "Could not connect to the mining pool. Try again later."
        else -> throwable.message?.takeUnless { it.isBlank() }
            ?: "Unknown mining error."
    }

    /** Human-readable label for a Stratum/server error string. */
    fun fromStratum(message: String?): String {
        val m = message ?: return "Unknown mining error."
        val lower = m.lowercase()
        return when {
            lower.contains("unauthorized") || lower.contains("auth") ||
                lower.contains("not authorized") || lower.contains("invalid password") ||
                lower.contains("bad password") || lower.contains("access denied") ->
                "Login failed. Check your wallet address and worker name."
            lower.contains("ban") || lower.contains("banned") ||
                lower.contains("difficulty too high") ->
                "Your worker was rejected by the pool. Check your settings and try again."
            lower.contains("timeout") || lower.contains("timed out") ->
                "Connection timed out. Check your internet connection."
            lower.contains("duplicate") || lower.contains("stale") ->
                "A share was submitted too late (stale). This is normal during reconnects."
            lower.contains("low difficulty") || lower.contains("high difficulty") ->
                "The pool reported a difficulty issue. Trying again…"
            else -> m
        }
    }

    /** Friendly text for a validation failure category. */
    fun fromValidation(error: String): String = when {
        error.contains("engine unavailable", ignoreCase = true) ->
            "Miner engine unavailable. Please install/provide a compatible X11 miner engine."
        error.contains("abi", ignoreCase = true) ->
            "This device architecture is not supported."
        error.contains("algorithm", ignoreCase = true) ->
            "The selected algorithm is not supported."
        error.contains("server", ignoreCase = true) ->
            "Server address not configured or invalid."
        error.contains("port", ignoreCase = true) ->
            "Port not configured or invalid."
        error.contains("username", ignoreCase = true) || error.contains("wallet", ignoreCase = true) ->
            "Username (wallet address) not configured."
        error.contains("protocol", ignoreCase = true) ->
            "Connection protocol is invalid."
        else -> error
    }

    /**
     * Short title used for notifications / the error banner, matching the
     * failure kinds the user cares about.
     */
    fun titleFor(error: String): String = when {
        error.contains("engine unavailable", ignoreCase = true) -> "Miner engine unavailable"
        error.contains("timed out", ignoreCase = true) -> "Connection timeout"
        error.contains("login failed", ignoreCase = true) || error.contains("auth", ignoreCase = true) ->
            "Authentication failed"
        error.contains("stopped", ignoreCase = true) || error.contains("unexpected", ignoreCase = true) ->
            "Miner process stopped"
        error.contains("architecture", ignoreCase = true) || error.contains("abi", ignoreCase = true) ->
            "Unsupported architecture"
        error.contains("connect", ignoreCase = true) || error.contains("connection", ignoreCase = true) ->
            "Stratum connection failed"
        else -> "Mining error"
    }
}