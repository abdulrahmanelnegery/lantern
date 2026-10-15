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
