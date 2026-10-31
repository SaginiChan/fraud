package com.jubileegroup.jbuzz

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jubileegroup.jbuzz.ui.theme.JBuzzTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationDemoScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun startsWithTheReferenceValues() {
        rule.setContent { JBuzzTheme { NotificationDemoScreen(onShowNotification = {}) } }

        rule.onNodeWithTag("field_amount").assert(hasText(ReferenceAmount))
        rule.onNodeWithTag("field_time").assert(hasText(ReferenceTime))
        rule.onNodeWithTag("field_paid_to").assert(hasText(ReferencePaidTo))
    }

    @Test
    fun sendsTheEditedValuesWhenTheButtonIsTapped() {
        var received: NotificationFields? = null
        rule.setContent { JBuzzTheme { NotificationDemoScreen(onShowNotification = { received = it }) } }

        rule.onNodeWithTag("field_amount").performTextReplacement("- KSH 1,250")
        rule.onNodeWithTag("field_time").performTextReplacement("10:45 AM")
        rule.onNodeWithTag("field_paid_to").performTextReplacement("Jane Doe")
        rule.onNodeWithTag("show_button").performClick()

        assertEquals(NotificationFields("- KSH 1,250", "10:45 AM", "Jane Doe"), received)
    }

    @Test
    fun doesNothingUntilTheButtonIsTapped() {
        var calls = 0
        rule.setContent { JBuzzTheme { NotificationDemoScreen(onShowNotification = { calls++ }) } }

        rule.onNodeWithTag("field_paid_to").performTextReplacement("typing, not sending")
        rule.waitForIdle()

        assertEquals(0, calls)
    }
}

