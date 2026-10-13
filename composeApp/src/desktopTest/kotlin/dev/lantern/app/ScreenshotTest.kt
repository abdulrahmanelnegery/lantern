package dev.lantern.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import dev.lantern.core.Account
import dev.lantern.core.Habit
import dev.lantern.core.JournalEntry
import dev.lantern.core.Note
import dev.lantern.core.Task
import dev.lantern.core.Txn
import dev.lantern.core.Vault
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

/**
 * Draws each screen offscreen into build/screenshots so the layout can be looked at without a device.
 * It also proves every screen composes with realistic data without throwing.
 */
@OptIn(ExperimentalComposeUiApi::class)
class ScreenshotTest {
    private fun sampleStore(): LanternStore {
        val vault = Vault(FakeFileSystem(), "/v".toPath())
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        vault.saveJournal(JournalEntry(today, "Slow morning, long walk, finished the chapter I was stuck on.", 4))
        vault.saveJournal(JournalEntry(today.minus(1, DateTimeUnit.DAY), "Busy day at work.", 3))
        vault.saveNote(Note("", "Trip to the mountains", "# Trip\n\nPack warm clothes, check the bus times.", "", "", listOf("travel"), pinned = true))
        vault.saveNote(Note("", "Reading list", "Books to read this winter.", "", "", listOf("books")))
        vault.saveTasks(listOf(
            Task("a1", "Renew the passport", due = today, priority = 3, list = "Errands"),
            Task("a2", "Call the plumber", due = today.minus(2, DateTimeUnit.DAY)),
            Task("a3", "Buy a birthday gift", list = "Errands"),
            Task("a4", "Water the plants", done = true),
        ))
        vault.saveHabits(listOf(Habit("h1", "Read 20 pages"), Habit("h2", "Walk", days = listOf(1, 3, 5))))
        for (back in listOf(0, 1, 2, 4, 5)) vault.setHabitDone("h1", today.minus(back, DateTimeUnit.DAY), true)
        vault.saveAccounts(listOf(Account("acc", "Cash", "USD", 50_000)))
        vault.saveTransaction(Txn("", today.minus(3, DateTimeUnit.DAY), 250_000, "acc", "Salary"))
        vault.saveTransaction(Txn("", today.minus(2, DateTimeUnit.DAY), -4_550, "acc", "Food", "Lunch"))
        vault.saveTransaction(Txn("", today.minus(1, DateTimeUnit.DAY), -12_000, "acc", "Bills"))
        return LanternStore(vault, "/v")
    }

    private fun shot(name: String, width: Int, height: Int, nav: (Nav) -> Unit) {
        val store = sampleStore()
        val scene = ImageComposeScene(width * 2, height * 2, Density(2f)) { LanternApp(store, Nav().also(nav)) }
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
        shot("phone-journal", 412, 860) { it.screen = Screen.Journal }
        shot("phone-insights", 412, 860) { it.screen = Screen.Insights }
        shot("phone-habit", 412, 860) { it.go(Section.Habits); it.habitId = "h1" }
        shot("phone-settings", 412, 860) { it.screen = Screen.Settings }
        shot("phone-search", 412, 860) { it.screen = Screen.Search }
    }

    @Test fun laptop() {
        shot("laptop-today", 1100, 760) { it.go(Section.Today) }
        shot("laptop-money", 1100, 760) { it.go(Section.Money) }
    }
}
