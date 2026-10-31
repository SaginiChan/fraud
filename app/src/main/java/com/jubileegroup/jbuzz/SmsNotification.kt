package com.jubileegroup.jbuzz

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.content.edit

/**

 * Shows the SMS card as a drop-down notification that stays on screen until the user closes it: with
 * the close button, or by swiping it away. The two actions at the bottom of the card leave it where it
 * is, and nothing in the app takes it away. A newer card replaces the one that is showing.
 *
 * The card is also remembered until it has been closed. Android can kill the app's process at any time
 * (a crash, low memory, a battery cleaner) and a window cannot outlive its process, so [OverlayService],
 * which the system starts again after such a kill, calls [restoreSmsNotification] to put the card back.
 */
internal fun showSmsNotification(context: Context, fields: NotificationFields) {
    if (!DropDownNotification.canShow(context)) return
    SmsNotificationStore.save(context, fields)
    display(context, fields)
}

/** Shows the remembered card again. Returns false if there is none, or it cannot be drawn. */
internal fun restoreSmsNotification(context: Context): Boolean {
    val fields = SmsNotificationStore.load(context) ?: return false
    if (!DropDownNotification.canShow(context)) return false
    display(context, fields)
    return true
}

private fun display(context: Context, fields: NotificationFields) {
    // Built here rather than inside the lambda: the notification can outlive any Activity, so the
    // lambda must not capture one.
    val meta = smsMeta(fields.time)
    val amount = fields.amount
    val message = paidToMessage(fields.paidTo)

    DropDownNotification.show(context) { dismiss ->
        // As wide as the window, but never taller than a share of it: a phone turned sideways is short,
        // and the card must not take over the screen.
        val windowHeightDp = LocalConfiguration.current.screenHeightDp
        TruecallerSmsCard(
            modifier = Modifier
                .widthIn(max = (windowHeightDp * MaxCardHeightShare * SmsCardAspectRatio).dp)
                .fillMaxWidth(),
            meta = meta,
            amount = amount,
            message = message,
            onClose = dismiss
        )
    }
}

// The card is never taller than this share of the window.
private const val MaxCardHeightShare = 0.45f

/** The card that is meant to be on screen, kept on disk so that it outlives the process. */
internal object SmsNotificationStore {

    private const val PREFS = "shown_notification"
    private const val KEY_AMOUNT = "amount"
    private const val KEY_TIME = "time"
    private const val KEY_PAID_TO = "paid_to"

    fun save(context: Context, fields: NotificationFields) {
        // commit = true: the process can be killed right after this, and the write must not be lost.
        prefs(context).edit(commit = true) {
            putString(KEY_AMOUNT, fields.amount)
            putString(KEY_TIME, fields.time)
            putString(KEY_PAID_TO, fields.paidTo)
        }
    }

    fun load(context: Context): NotificationFields? {
        val prefs = prefs(context)
        return NotificationFields(
            amount = prefs.getString(KEY_AMOUNT, null) ?: return null,
            time = prefs.getString(KEY_TIME, null) ?: return null,
            paidTo = prefs.getString(KEY_PAID_TO, null) ?: return null
        )
    }

    fun clear(context: Context) {
        prefs(context).edit(commit = true) { clear() }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
