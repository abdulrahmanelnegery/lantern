package dev.thabit.core

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSettingsTest {
    @Test fun anOldSettingsFileWithoutTheAtharFlagStillReads() {
        val json = """{"palette":"forest","mode":"dark","homeOrder":["journal","tasks"],"tourDone":true}"""
        val s = Json { ignoreUnknownKeys = true }.decodeFromString(AppSettings.serializer(), json)
        assertEquals("forest", s.palette); assertTrue(s.tourDone); assertFalse(s.atharNoticeDismissed)
    }
}

class AppSettingsNormalizeTest {
    @Test fun defaultsAreAlreadyNormal() = assertEquals(AppSettings(), AppSettings().normalized())

    @Test fun outOfRangeValuesComeBack() {
        val n = AppSettings(palette = "pink", mode = "x", textSize = 9, density = "tiny", sectionOrder = listOf("tasks", "nope", "tasks"), hiddenSections = setOf("home", "insights", "zzz"), startSection = "insights").normalized()
        assertEquals(PaletteIds.DEFAULT, n.palette); assertEquals("system", n.mode); assertEquals(3, n.textSize); assertEquals("comfortable", n.density)
        assertEquals(listOf("tasks"), n.sectionOrder); assertEquals(setOf("insights"), n.hiddenSections)
        assertEquals(Sections.HOME, n.startSection, "a hidden section cannot be the start")
    }

    @Test fun visibleSectionsFollowTheOrderAndKeepHome() {
        assertEquals(listOf("home", "habits", "tasks", "insights"), Sections.visible(emptyList(), emptySet()))
        assertEquals(listOf("tasks", "home", "habits"), Sections.visible(listOf("tasks", "home"), setOf("insights", "home")))
    }

    @Test fun anOldFileWithoutTheNewFieldsGetsDefaults() {
        val s = Json { ignoreUnknownKeys = true }.decodeFromString(AppSettings.serializer(), """{"palette":"rose","mode":"light"}""")
        assertEquals(1, s.textSize); assertEquals(Sections.HOME, s.startSection); assertTrue(s.hiddenSections.isEmpty())
    }
}
