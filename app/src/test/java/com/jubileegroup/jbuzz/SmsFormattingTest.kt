package com.jubileegroup.jbuzz

import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Test

class SmsFormattingTest {

    @Test
    fun metaIsSmsBulletAndTheTime() {
        assertEquals("SMS • 09:09 PM", smsMeta("09:09 PM"))
    }

    @Test
    fun paidToReadsLikeASentence() {
        assertEquals("Paid to Jane Doe", paidToMessage("Jane Doe").text)
    }

    @Test
    fun paidAndTheNameAreBoldButToIsNot() {
        val message = paidToMessage("Jane Doe")
        val bold = message.spanStyles
            .filter { it.item.fontWeight == FontWeight.Bold }
            .map { message.text.substring(it.start, it.end) }
        assertEquals(listOf("Paid", "Jane Doe"), bold)
    }

    @Test
    fun referenceValuesAreTheOnesInTheScreenshot() {
        assertEquals("09:09 PM", ReferenceTime)
        assertEquals("- KSH 50", ReferenceAmount)
        assertEquals("Vincent Momanyi", ReferencePaidTo)
    }
}

