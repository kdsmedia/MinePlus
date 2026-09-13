package com.altomedia.mineplus.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.altomedia.mineplus.MainActivity
import com.altomedia.mineplus.R
import com.altomedia.mineplus.di.MinerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service that keeps the X11 miner alive when the app is
 * backgrounded. The actual mining is orchestrated by [MinerController]
 * (native mining + stratum), this service only owns the process lifetime
 * and the ongoing notification.
 */
@AndroidEntryPoint
class MinerService : Service() {

    companion object {
        private const val CHANNEL_ID = "mineplus_mining"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_START = "com.altomedia.mineplus.action.START"
        private const val ACTION_STOP = "com.altomedia.mineplus.action.STOP"

        fun start(context: Context) {
            val intent = Intent(context, MinerService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, MinerService::class.java).setAction(ACTION_STOP))
        }
    }

    @Inject
    lateinit var controller: MinerController

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                controller.stop()
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startInForeground()
                controller.start()
                startStatsTicker()
                return START_STICKY
            }
        }
    }

    private fun startInForeground() {
        val notification = buildNotification("Starting…")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(text: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, MinerService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, getString(R.string.action_stop), stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun startStatsTicker() {
        serviceScope.launch {
            while (controller.isRunning) {
                val s = controller.state.value
                val notification = buildNotification(
                    "Hashrate: ${formatHashrate(s.hashrate)}" +
                        "  |  A: ${s.acceptedShares} R: ${s.rejectedShares}" +
                        "  |  Up: ${formatUptime(s.uptimeSeconds)}"
                )
                getSystemService(NotificationManager::class.java)
                    ?.notify(NOTIFICATION_ID, notification)
                delay(2000)
            }
        }
    }

    private fun formatHashrate(h: Double): String = when {
        h >= 1_000_000 -> "%.2f MH/s".format(h / 1_000_000)
        h >= 1_000 -> "%.2f KH/s".format(h / 1_000)
        else -> "%.0f H/s".format(h)
    }

    private fun formatUptime(sec: Long): String {
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        controller.destroy()
        super.onDestroy()
    }
}