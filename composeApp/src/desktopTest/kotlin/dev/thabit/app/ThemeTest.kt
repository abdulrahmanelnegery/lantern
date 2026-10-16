package dev.thabit.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import dev.thabit.core.PaletteIds
import dev.thabit.ui.Palettes
import dev.thabit.ui.textScales
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemeTest {
    private fun contrast(a: Color, b: Color): Float {
        val (hi, lo) = if (a.luminance() > b.luminance()) a to b else b to a
        return (hi.luminance() + 0.05f) / (lo.luminance() + 0.05f)
    }

    @Test fun theUiPalettesAreExactlyTheIdsTheSettingsKnow() = assertEquals(PaletteIds.all, Palettes.map { it.id })

    @Test fun everyPaletteReadsWellInLightAndDark() {
        for (p in Palettes) for (dark in listOf(false, true)) {
            val c = p.scheme(dark)
            assertTrue(contrast(c.onPrimary, c.primary) >= 4.5f, "${p.id} dark=$dark button text")
            assertTrue(contrast(c.onSurface, c.background) >= 7f, "${p.id} dark=$dark body text")
            assertTrue(contrast(c.onSurfaceVariant, c.background) >= 4.5f, "${p.id} dark=$dark secondary text")
            assertTrue(contrast(c.primary, c.background) >= 3f, "${p.id} dark=$dark accent on background")
        }
    }

    @Test fun textSizeStepsGrow() {
        assertEquals(4, textScales.size)
        assertTrue(textScales.toList().zipWithNext().all { (a, b) -> a < b })
        assertEquals(1f, textScales[1])
    }
}
