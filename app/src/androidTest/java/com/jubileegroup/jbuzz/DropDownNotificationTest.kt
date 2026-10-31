package com.jubileegroup.jbuzz

import android.app.ActivityManager
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileInputStream
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the real overlay window and the foreground service behind it. */
@RunWith(AndroidJUnit4::class)
class DropDownNotificationTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Before
    fun allowDrawingOverOtherApps() {
        shell("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
        rule.waitUntil(timeoutMillis = 5_000) { Settings.canDrawOverlays(context) }
    }

    @After
    fun takeTheNotificationAway() {
        DropDownNotification.dismiss()
        SmsNotificationStore.clear(context)
    }

    @Test
    fun staysUntilDismissedAndKeepsTheProcessInTheForeground() {
        DropDownNotification.show(context) { dismiss ->
            TruecallerSmsCard(
                modifier = Modifier.fillMaxWidth(),
                meta = smsMeta("10:45 AM"),
                amount = "- KSH 1,250",
                message = paidToMessage("Overlay Person"),
                onClose = dismiss,
                onPrimaryAction = dismiss
            )
        }

        rule.waitUntil(timeoutMillis = 10_000) { isShowing("- KSH 1,250") }
        rule.onNodeWithText("- KSH 1,250").assertIsDisplayed()
        rule.waitUntil(timeoutMillis = 10_000) { overlayServiceIsForeground() }

        // There is no timeout: it is still there long after a normal heads-up would have gone.
        Thread.sleep(6_000)
        rule.onNodeWithText("- KSH 1,250").assertIsDisplayed()
        assertTrue("service should still be in the foreground", overlayServiceIsForeground())

        rule.onNodeWithText("Mark as read").performClick()
        rule.waitUntil(timeoutMillis = 10_000) { !isShowing("- KSH 1,250") }
        rule.waitUntil(timeoutMillis = 10_000) { !overlayServiceIsRunning() }
    }

    @Test
    fun theFooterActionsLeaveTheCardWhereItIs() {
        showSmsNotification(context, NotificationFields("- KSH 77", "08:15 AM", "Grace Wanjiru"))
        rule.waitUntil(timeoutMillis = 10_000) { isShowing("- KSH 77") }
        rule.waitForIdle()

        rule.onNodeWithText("Mark as read").performClick()
        rule.onNodeWithText("View profile").performClick()

        // A dismissal would have the card slide away and its window removed within about 300 ms.
        Thread.sleep(2_000)
        rule.onNodeWithText("- KSH 77").assertIsDisplayed()
        assertTrue("service should still be in the foreground", overlayServiceIsForeground())
    }

    @Test
    fun theCloseButtonTakesTheCardAway() {
        showSmsNotification(context, NotificationFields("- KSH 88", "10:10 AM", "Esther Njeri"))
        rule.waitUntil(timeoutMillis = 10_000) { isShowing("- KSH 88") }
        rule.waitForIdle()

        closeButton().performClick()

        assertGoneForGood("- KSH 88")
    }

    @Test
    fun swipingTheCardUpTakesItAway() {
        showSmsNotification(context, NotificationFields("- KSH 99", "11:11 AM", "Brian Kiptoo"))
        rule.waitUntil(timeoutMillis = 10_000) { isShowing("- KSH 99") }
        rule.waitForIdle()

        val x = screenWidthPx() / 2
        swipe(fromX = x, fromY = dp(150), toX = x, toY = dp(20))

        assertGoneForGood("- KSH 99")
    }

    @Test
    fun swipingTheCardSidewaysTakesItAway() {
        showSmsNotification(context, NotificationFields("- KSH 44", "12:30 PM", "Lucy Achieng"))
        rule.waitUntil(timeoutMillis = 10_000) { isShowing("- KSH 44") }
        rule.waitForIdle()

        val width = screenWidthPx()
        swipe(fromX = width / 4, fromY = dp(130), toX = width * 3 / 4, toY = dp(130))

        assertGoneForGood("- KSH 44")
    }

    @Test
    fun aShortDragOnTheCardIsNotASwipe() {
        showSmsNotification(context, NotificationFields("- KSH 33", "01:45 PM", "Ken Mwangi"))
        rule.waitUntil(timeoutMillis = 10_000) { isShowing("- KSH 33") }
        rule.waitForIdle()

        val x = screenWidthPx() / 2
        swipe(fromX = x, fromY = dp(150), toX = x, toY = dp(140)) // 10 dp, less than the swipe distance

        Thread.sleep(2_000)
        rule.onNodeWithText("- KSH 33").assertIsDisplayed()
    }

    @Test
    fun aRememberedCardCanBeShownAgain() {
        // This is what OverlayService does once Android has started it again after killing the process.
        SmsNotificationStore.save(context, NotificationFields("- KSH 55", "07:30 PM", "Peter Otieno"))

        assertTrue(restoreSmsNotification(context))
        rule.waitUntil(timeoutMillis = 10_000) { isShowing("- KSH 55") }
        rule.onNodeWithText("Paid to Peter Otieno").assertIsDisplayed()
        rule.onNodeWithText("SMS • 07:30 PM").assertIsDisplayed()
    }

    @Test
    fun thereIsNothingToRestoreWhenNoCardWasShown() {
        SmsNotificationStore.clear(context)
        assertFalse(restoreSmsNotification(context))
    }

    @Test
    fun aCardThatCodeTakesAwayIsForgotten() {
        showSmsNotification(context, NotificationFields("- KSH 66", "09:00 AM", "Mary Atieno"))
        rule.waitUntil(timeoutMillis = 10_000) { isShowing("- KSH 66") }
        rule.waitUntil(timeoutMillis = 10_000) { overlayServiceIsForeground() }
        assertNotNull("a card that is showing is remembered", SmsNotificationStore.load(context))

        DropDownNotification.dismiss()
        rule.waitUntil(timeoutMillis = 10_000) { !isShowing("- KSH 66") }
        rule.waitUntil(timeoutMillis = 10_000) { !overlayServiceIsRunning() }
        rule.waitUntil(timeoutMillis = 10_000) { SmsNotificationStore.load(context) == null }
    }

    @Test
    fun aSecondNotificationReplacesTheFirst() {
        DropDownNotification.show(context) { TruecallerSmsCard(amount = "- KSH 111") }
        rule.waitUntil(timeoutMillis = 10_000) { isShowing("- KSH 111") }

        DropDownNotification.show(context) { TruecallerSmsCard(amount = "- KSH 222") }
        rule.waitUntil(timeoutMillis = 10_000) { isShowing("- KSH 222") }
        rule.waitUntil(timeoutMillis = 10_000) { !isShowing("- KSH 111") }
        assertTrue("service should still run for the new card", overlayServiceIsForeground())
    }

    private fun isShowing(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    // The close button is the part of the card that can be clicked and has no text of its own. The card's
    // logo is its sibling, which tells it apart from the clickable parts of the app's own screen.
    private fun closeButton() = rule.onNode(
        hasClickAction() and hasAnySibling(hasContentDescription("Truecaller")) and
            !hasText("Mark as read") and !hasText("View profile")
    )

    private fun screenWidthPx() = context.resources.displayMetrics.widthPixels

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()

    /** A swipe made through the system, like a finger on the screen, over the overlay window. */
    private fun swipe(fromX: Int, fromY: Int, toX: Int, toY: Int) {
        shell("input swipe $fromX $fromY $toX $toY 250")
    }

    /** The card is gone, so are its window and the service, and it is forgotten: it is not brought back. */
    private fun assertGoneForGood(text: String) {
        rule.waitUntil(timeoutMillis = 10_000) { !isShowing(text) }
        rule.waitUntil(timeoutMillis = 10_000) { !overlayServiceIsRunning() }
        rule.waitUntil(timeoutMillis = 10_000) { SmsNotificationStore.load(context) == null }
        assertFalse("a closed card must not be restored", restoreSmsNotification(context))
    }

    @Suppress("DEPRECATION") // still reports the app's own services
    private fun overlayServices() = context.getSystemService(ActivityManager::class.java)
        .getRunningServices(Int.MAX_VALUE)
        .filter { it.service.className == OverlayService::class.java.name }

    private fun overlayServiceIsRunning() = overlayServices().isNotEmpty()

    private fun overlayServiceIsForeground() = overlayServices().any { it.foreground }

    private fun shell(command: String) {
        instrumentation.uiAutomation.executeShellCommand(command).use { pfd ->
            FileInputStream(pfd.fileDescriptor).use { it.readBytes() } // blocks until the command is done
        }
    }
}
