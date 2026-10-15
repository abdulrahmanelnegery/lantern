package dev.thabit.app

import dev.thabit.ui.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import dev.thabit.core.Vault
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import dev.thabit.habits.Habit
import dev.thabit.tasks.Task

/**
 * Draws each screen offscreen into build/screenshots so the layout can be looked at without a device.
 * It also proves every screen composes with realistic data without throwing.
 */
@OptIn(ExperimentalComposeUiApi::class)
class ScreenshotTest {
    private fun sampleStore(): ThabitStore {
        val vault = Vault(FakeFileSystem(), "/v".toPath())
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        // Notes and journal files from an older Thabit: the app only has to leave them alone and show the Athar card.
        vault.fs.createDirectories("/v/notes".toPath()); vault.fs.createDirectories("/v/journal/2026".toPath())
        vault.fs.write("/v/notes/trip-aaaa1111.md".toPath()) { writeUtf8("---\nid: aaaa1111\ntitle: Trip\n---\nPack warm clothes.") }
        vault.fs.write("/v/journal/2026/$today.md".toPath()) { writeUtf8("---\ndate: $today\nmood: 4\n---\nSlow morning.") }
        vault.saveTasks(listOf(
            Task("a1", "Renew the passport", due = today, priority = 3, list = "Errands"),
            Task("a2", "Call the plumber", due = today.minus(2, DateTimeUnit.DAY)),
            Task("a3", "Buy a birthday gift", list = "Errands"),
            Task("a4", "Water the plants", done = true),
        ))
        vault.saveHabits(listOf(Habit("h1", "Read 20 pages"), Habit("h2", "Walk", days = listOf(1, 3, 5))))
        for (back in listOf(0, 1, 2, 4, 5)) vault.setHabitDone("h1", today.minus(back, DateTimeUnit.DAY), true)
        // The tour and what's-new dialog are not what these renders are about.
        vault.saveSettings(dev.thabit.core.AppSettings(tourDone = true, lastSeenVersion = Brand.VERSION))
        return ThabitStore(vault, "/v")
    }

    private fun shot(name: String, width: Int, height: Int, settings: dev.thabit.core.AppSettings? = null, nav: (Nav) -> Unit) {
        val store = sampleStore()
        settings?.let { store.saveSettings(it.copy(tourDone = true, lastSeenVersion = Brand.VERSION)) }
        val scene = ImageComposeScene(width * 2, height * 2, Density(2f)) { ThabitApp(store, Nav().also(nav), showSplash = false) }
        try {
            scene.render(0)
            val data = scene.render(100_000_000).encodeToData(EncodedImageFormat.PNG)!!
            val out = File("build/screenshots").apply { mkdirs() }
            File(out, "$name.png").writeBytes(data.bytes)
            assertTrue(data.bytes.size > 1_000)
        } finally {
            scene.close()
        }
    }

    @Test fun phone() {
        for (s in Section.entries) shot("phone-${s.name.lowercase()}", 412, 860) { it.go(s) }
        shot("phone-insights", 412, 860) { it.screen = Screen.Insights }
        shot("phone-habit", 412, 860) { it.go(Section.Habits); it.habitId = "h1" }
        shot("phone-settings", 412, 860) { it.screen = Screen.Settings }
        shot("phone-search", 412, 860) { it.screen = Screen.Search }
    }

    @Test fun homeShowsTheAtharCardUntilItIsDismissed() {
        shot("phone-today-athar-card", 412, 860) { it.go(Section.Today) }
        val store = sampleStore()
        assertTrue(store.showAtharNotice)
        store.saveSettings(store.settings.copy(atharNoticeDismissed = true))
        assertTrue(!store.showAtharNotice)
        assertTrue(!ThabitStore(Vault(FakeFileSystem(), "/empty".toPath()), "/empty").showAtharNotice, "no notes or journal files, no card")
    }

    @Test fun themes() {
        val s = { palette: String, mode: String -> dev.thabit.core.AppSettings(palette, mode) }
        shot("theme-forest-light-today", 412, 860, s("forest", "light")) { it.go(Section.Today) }
        shot("theme-rose-dark-tasks", 412, 860, s("rose", "dark")) { it.go(Section.Tasks) }
        shot("theme-ocean-light-habits", 412, 860, s("ocean", "light")) { it.go(Section.Habits) }
        shot("theme-slate-dark-insights", 412, 860, s("slate", "dark")) { it.screen = Screen.Insights }
        shot("theme-settings", 412, 860, s("amber", "light")) { it.screen = Screen.Settings }
    }

    @Test fun laptop() {
        shot("laptop-today", 1100, 760) { it.go(Section.Today) }
        shot("laptop-habits", 1100, 760) { it.go(Section.Habits) }
    }
}
