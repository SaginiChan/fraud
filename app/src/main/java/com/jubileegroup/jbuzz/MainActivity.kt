package com.jubileegroup.jbuzz

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.jubileegroup.jbuzz.ui.theme.JBuzzTheme

class MainActivity : ComponentActivity() {

    // What to show once the "Display over other apps" permission has been granted.
    private var pendingFields: NotificationFields? = null

    // Android 13+: lets the ongoing notification of the foreground service (see OverlayService) show in
    // the notification shade. Asked once, and the card is shown whatever the answer is.
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { continueShowing() }

    // The drop-down is drawn over other apps, which needs the "Display over other apps" permission.
    private val overlayPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val fields = pendingFields
            pendingFields = null
            if (fields != null && DropDownNotification.canShow(this)) showSmsNotification(this, fields)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // The app is always dark, so the bar icons must be light whatever the system setting is.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            JBuzzTheme {
                NotificationDemoScreen(onShowNotification = ::onShowNotificationClicked)
            }
        }
    }

    private fun onShowNotificationClicked(fields: NotificationFields) {
        pendingFields = fields
        if (shouldAskForNotificationPermission()) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ASKED_NOTIFICATIONS, true).apply()
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            continueShowing()
        }
    }

    private fun shouldAskForNotificationPermission() =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_ASKED_NOTIFICATIONS, false)

    /** The next step once the notification question is settled: get the overlay permission, then show. */
    private fun continueShowing() {
        val fields = pendingFields ?: return
        if (DropDownNotification.canShow(this)) {
            pendingFields = null
            showSmsNotification(this, fields)
        } else {
            overlayPermission.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())
            )
        }
    }

    private companion object {
        const val PREFS = "settings"
        const val KEY_ASKED_NOTIFICATIONS = "asked_notification_permission"
    }
}
