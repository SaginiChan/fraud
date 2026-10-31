package com.jubileegroup.jbuzz

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Keeps the app's process alive while a [DropDownNotification] is on screen, and brings the card back
 * if the process is killed anyway.
 *
 * The notification is a window owned by this app's process. Without a foreground service Android is
 * free to freeze or kill that process once the app has left the foreground, and the window would go
 * with it. Running as a foreground service marks the process as doing work the user can see.
 *
 * If the process is killed regardless (a crash, a battery cleaner, an extreme low-memory situation),
 * the window goes with it. The service is sticky, so Android starts it again in a new process and
 * [onStartCommand] shows the card again (see [restoreSmsNotification]). That does not happen after
 * "Force stop" in Settings: that is the user stopping the app on purpose.
 *
 * Android shows an ongoing notification for a foreground service (on Android 13+ only if the user has
 * allowed notifications for the app; the service keeps running either way). Tapping it opens the app.
 */
class OverlayService : Service() {

    private var isForeground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(getString(R.string.overlay_channel_name))
                .build()
        )
        goForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Normally already done in onCreate; this is the second chance if Android refused it there.
        goForeground()
        // A null intent means Android started this service again after the app's process was killed.
        // The card was a window of that process and went with it, so put it back.
        if (intent == null && !restoreSmsNotification(this)) stopSelf()
        return START_STICKY
    }

    /**
     * Android can refuse to make a service a foreground one (for example when it was restarted in the
     * background and the phone does not allow that). That must not crash the app, and so the card, so
     * the service then just carries on as an ordinary one.
     */
    private fun goForeground() {
        if (isForeground) return
        isForeground = try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                // The "specialUse" type only exists on Android 14+; earlier versions have no type to pass.
                if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
            )
            true
        } catch (e: RuntimeException) {
            false
        }
    }

    override fun onDestroy() {
        // Only reached when the service is stopped on purpose (a killed process never gets here), which
        // means the card is no longer wanted: forget it, so that it is not brought back. Not if a newer
        // card is already on screen: this is then an old instance finishing after the new one was shown.
        if (!DropDownNotification.isShowing()) SmsNotificationStore.clear(this)
        super.onDestroy()
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_buzz)
        .setColor(ContextCompat.getColor(this, R.color.primary_color))
        .setContentTitle(getString(R.string.overlay_notification_title))
        .setContentText(getString(R.string.overlay_notification_text))
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .build()

    companion object {
        private const val CHANNEL_ID = "overlay_card"
        private const val NOTIFICATION_ID = 1

        /** Starts the service. Call while the app is visible. Best effort: the card works without it. */
        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, OverlayService::class.java))
            } catch (e: RuntimeException) {
                // The system refused to start a foreground service; the overlay window itself still works.
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }
}
