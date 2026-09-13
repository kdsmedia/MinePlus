package com.altomedia.mineplus.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.altomedia.mineplus.MainActivity
import com.altomedia.mineplus.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Emits transient miner events as notifications (Mining started,
 * Connection lost, High reject rate, …). These are separate from the
 * permanent foreground notification owned by [MinerService] and
 * auto-dismiss when tapped.
 */
@Singleton
class MinerNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                EVENT_CHANNEL_ID,
                context.getString(R.string.event_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.event_channel_desc)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /** Posts a one-off, auto-dismissing event notification. */
    fun post(message: String, subtext: String? = null) {
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, EVENT_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (!subtext.isNullOrBlank()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText("$message\n\n$subtext"))
        }
        notificationManager.notify(EVENT_NOTIFICATION_ID, builder.build())
    }

    companion object {
        private const val EVENT_CHANNEL_ID = "mineplus_events"
        private const val EVENT_NOTIFICATION_ID = 2
    }
}