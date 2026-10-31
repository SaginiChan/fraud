package com.jubileegroup.jbuzz

import android.graphics.Canvas
import android.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppIconTest {

    @Test
    fun launcherIconShowsTheJubileeLogoNotTheDefaultRobot() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val icon = context.packageManager.getApplicationIcon(context.packageName)
        val size = 216
        val bitmap = createBitmap(size, size)
        icon.setBounds(0, 0, size, size)
        icon.draw(Canvas(bitmap))

        var logoRed = 0
        var robotGreen = 0
        for (x in 0 until size) {
            for (y in 0 until size) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                if (r in 140..200 && g < 60 && b in 20..90) logoRed++ // the logo's crimson
                if (g > 180 && r < 120 && b in 90..170) robotGreen++ // the template icon's green
            }
        }
        assertTrue("expected the logo's crimson, found $logoRed px", logoRed > size * size * 0.02)
        assertEquals("the default robot icon's green must be gone", 0, robotGreen)
    }
}

