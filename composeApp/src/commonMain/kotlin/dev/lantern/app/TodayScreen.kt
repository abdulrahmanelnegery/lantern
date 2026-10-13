package dev.lantern.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lantern.core.JournalEntry
import dev.lantern.core.streak
import kotlinx.coroutines.delay

private val moods = listOf("😞" to "Low", "😕" to "Meh", "😐" to "Okay", "🙂" to "Good", "😄" to "Great")

/** Today's cards, in the order and with the visibility chosen in Settings. */
val TodayCards = listOf("quote" to "Affirmation and tip", "streaks" to "Streaks", "habits" to "Habits today", "tasks" to "Tasks today", "journal" to "Journal")

fun todayOrder(settings: dev.lantern.core.AppSettings): List<String> {
    val ids = TodayCards.map { it.first }
    return (settings.homeOrder.filter { it in ids } + ids).distinct()
}

/** The day at a glance: a quote, streaks, today's habits and tasks, and the journal. */
@Composable
fun TodayScreen(store: LanternStore) {
    val today = store.today
    var shuffle by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column {
            Text(prettyDate(today), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
        todayOrder(store.settings).filter { it !in store.settings.homeHidden }.forEach { id ->
            when (id) {
                "quote" -> Panel(onClick = { shuffle++ }) {
                    Text(dev.lantern.core.Quotes.affirmation(today, shuffle), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(dev.lantern.core.Quotes.tip(today, shuffle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                "streaks" -> StreaksCard(store)
                "journal" -> JournalPanel(store, today)
                "habits" -> {
        val todays = store.habits.filter { !it.archived && (it.days.isEmpty() || today.dayOfWeek.ordinal + 1 in it.days) }
        if (todays.isNotEmpty()) Panel {
            Text("Habits today", style = MaterialTheme.typography.titleSmall)
            todays.forEach { h ->
                val days = store.habitLog[h.id].orEmpty()
                val done = today in days
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape10).clickable { store.setHabitDone(h.id, today, !done) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    androidx.compose.material3.Icon(
                        if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null,
                        tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(h.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    val s = streak(h, days, today, store.habitSkips[h.id].orEmpty())
                    if (s > 0) Text("$s day streak", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

                }
                "tasks" -> {
        val open = store.tasks.filter { !it.done && (it.due == null || it.due!! <= today) }.take(5)
        if (open.isNotEmpty()) Panel {
            Text("Tasks for today", style = MaterialTheme.typography.titleSmall)
            open.forEach { t ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(false, { store.saveTasks(store.tasks.map { x -> if (x.id == t.id) x.copy(done = true, doneOn = store.today) else x }) })
                    Column(Modifier.weight(1f)) {
                        Text(t.title, style = MaterialTheme.typography.bodyLarge)
                        t.due?.let { if (it < today) Text("Overdue: ${prettyDate(it)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
                }
            }
        }
    }
}

/** The longest running habit streak, how many of today's habits are done, and the days in a row with a journal entry. */
@Composable
private fun StreaksCard(store: LanternStore) {
    val today = store.today
    val active = store.habits.filter { !it.archived }
    val todays = active.filter { it.days.isEmpty() || today.dayOfWeek.ordinal + 1 in it.days }
    val best = active.maxOfOrNull { streak(it, store.habitLog[it.id].orEmpty(), today, store.habitSkips[it.id].orEmpty()) } ?: 0
    val doneToday = todays.count { today in store.habitLog[it.id].orEmpty() }
    val journalDays = remember(store.todayEntry, today) { store.vault.journalDates().toSet() }
    Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("Best streak", style = MaterialTheme.typography.labelMedium); Text("$best ${if (best == 1) "day" else "days"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) }
            Column { Text("Habits today", style = MaterialTheme.typography.labelMedium); Text("$doneToday of ${todays.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            Column { Text("Journal", style = MaterialTheme.typography.labelMedium); val j = dev.lantern.core.journalStreak(journalDays, today); Text("$j ${if (j == 1) "day" else "days"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        }
    }
}

private val RoundedCornerShape10 = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)

/** Mood and the journal text for one day, saved a moment after you stop typing. */
@Composable
fun JournalPanel(store: LanternStore, date: kotlinx.datetime.LocalDate, modifier: Modifier = Modifier) {
    val entry = if (date == store.today) store.todayEntry else remember(date) { store.journal(date) }
    var text by remember(date) { mutableStateOf(entry?.body.orEmpty()) }
    var mood by remember(date) { mutableStateOf(entry?.mood) }
    LaunchedEffect(text, mood) {
        if (text == entry?.body.orEmpty() && mood == entry?.mood) return@LaunchedEffect
        delay(700)
        store.saveJournal(JournalEntry(date, text, mood, entry?.tags.orEmpty()))
    }
    Panel(modifier) {
        Text("How are you?", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            moods.forEachIndexed { i, (face, label) ->
                val value = i + 1
                Column(
                    Modifier.clip(CircleShape).clickable { mood = if (mood == value) null else value }.padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(if (mood == value) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) { Text(face, style = MaterialTheme.typography.headlineSmall) }
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        OutlinedTextField(
            text, { text = it },
            placeholder = { Text("What happened? What are you thankful for?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 12,
        )
    }
}
