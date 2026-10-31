package com.jubileegroup.jbuzz

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jubileegroup.jbuzz.ui.theme.JBuzzTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TruecallerSmsCardTest {

    @get:Rule
    val rule = createComposeRule()

    private fun showCard(
        amount: String = "- KSH 1,250",
        paidTo: String = "Jane Doe",
        onClose: () -> Unit = {},
        onPrimary: () -> Unit = {},
        onSecondary: () -> Unit = {}
    ) = rule.setContent {
        JBuzzTheme {
            TruecallerSmsCard(
                modifier = Modifier.fillMaxWidth(),
                meta = smsMeta("10:45 AM"),
                amount = amount,
                message = paidToMessage(paidTo),
                onClose = onClose,
                onPrimaryAction = onPrimary,
                onSecondaryAction = onSecondary
            )
        }
    }

    // The close button is the only clickable part of the card without a text.
    private fun closeButton() =
        rule.onNode(hasClickAction() and !hasText("Mark as read") and !hasText("View profile"))

    @Test
    fun showsEverythingItIsGiven() {
        showCard()
        rule.onNodeWithContentDescription("Truecaller").assertIsDisplayed()
        rule.onNodeWithText("SMS • 10:45 AM").assertIsDisplayed()
        rule.onNodeWithContentDescription("MPESA").assertIsDisplayed()
        rule.onNodeWithText("MPESA").assertIsDisplayed()
        rule.onNodeWithText("- KSH 1,250").assertIsDisplayed()
        rule.onNodeWithText("Paid to Jane Doe").assertIsDisplayed()
        rule.onNodeWithText("Mark as read").assertIsDisplayed()
        rule.onNodeWithText("View profile").assertIsDisplayed()
    }

    @Test
    fun defaultsReproduceTheReferenceScreenshot() {
        rule.setContent { JBuzzTheme { TruecallerSmsCard(modifier = Modifier.fillMaxWidth()) } }
        rule.onNodeWithText("SMS • 09:09 PM").assertIsDisplayed()
        rule.onNodeWithText("- KSH 50").assertIsDisplayed()
        rule.onNodeWithText("Paid to Vincent Momanyi").assertIsDisplayed()
    }

    @Test
    fun closeButtonCallsOnClose() {
        var closed = 0
        showCard(onClose = { closed++ })
        closeButton().performClick()
        assertEquals(1, closed)
    }

    @Test
    fun footerActionsCallTheirOwnCallbacks() {
        var primary = 0
        var secondary = 0
        showCard(onPrimary = { primary++ }, onSecondary = { secondary++ })

        rule.onNodeWithText("Mark as read").performClick()
        assertEquals(1, primary)
        assertEquals(0, secondary)

        rule.onNodeWithText("View profile").performClick()
        assertEquals(1, primary)
        assertEquals(1, secondary)
    }

    @Test
    fun veryLongTextDoesNotBreakTheLayout() {
        val long = "w".repeat(300)
        showCard(amount = long, paidTo = long)
        rule.onNodeWithText("Mark as read").assertIsDisplayed()
        closeButton().assertIsDisplayed()
    }
}

