package dev.dawem.app

import dev.dawem.ui.*
import dev.dawem.journal.JournalPanel
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
import kotlinx.coroutines.delay
import dev.dawem.habits.streak
import dev.dawem.journal.JournalEntry


/** Today's cards, in the order and with the visibility chosen in Settings. */
val TodayCards = listOf("quote" to "Affirmation and tip", "streaks" to "Streaks", "habits" to "Habits today", "tasks" to "Tasks today", "journal" to "Journal")

fun todayOrder(settings: dev.dawem.core.AppSettings): List<String> {
    val ids = TodayCards.map { it.first }
    return (settings.homeOrder.filter { it in ids } + ids).distinct()
}

/** The day at a glance: a quote, streaks, today's habits and tasks, and the journal. */
@Composable
fun TodayScreen(store: DawemStore) {
    val today = store.today
    var shuffle by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column {
            Text(prettyDate(today), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
        todayOrder(store.settings).filter { it !in store.settings.homeHidden }.forEach { id ->
            when (id) {
                "quote" -> Panel(onClick = { shuffle++ }) {
                    Text(dev.dawem.core.Quotes.affirmation(today, shuffle), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(dev.dawem.core.Quotes.tip(today, shuffle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                "streaks" -> StreaksCard(store)
                "journal" -> JournalPanel(store.journalState, today)
                "habits" -> {
        val todays = store.habitsState.habits.filter { !it.archived && (it.days.isEmpty() || today.dayOfWeek.ordinal + 1 in it.days) }
        if (todays.isNotEmpty()) Panel {
            Text("Habits today", style = MaterialTheme.typography.titleSmall)
            todays.forEach { h ->
                val days = store.habitsState.log[h.id].orEmpty()
                val done = today in days
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape10).clickable { store.habitsState.setDone(h.id, today, !done) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    androidx.compose.material3.Icon(
                        if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null,
                        tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(h.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    val s = streak(h, days, today, store.habitsState.skips[h.id].orEmpty())
                    if (s > 0) Text("$s day streak", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

                }
                "tasks" -> {
        val open = store.tasksState.tasks.filter { !it.done && (it.due == null || it.due!! <= today) }.take(5)
        if (open.isNotEmpty()) Panel {
            Text("Tasks for today", style = MaterialTheme.typography.titleSmall)
            open.forEach { t ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(false, { store.tasksState.save(store.tasksState.tasks.map { x -> if (x.id == t.id) x.copy(done = true, doneOn = store.today) else x }) })
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
private fun StreaksCard(store: DawemStore) {
    val today = store.today
    val active = store.habitsState.habits.filter { !it.archived }
    val todays = active.filter { it.days.isEmpty() || today.dayOfWeek.ordinal + 1 in it.days }
    val best = active.maxOfOrNull { streak(it, store.habitsState.log[it.id].orEmpty(), today, store.habitsState.skips[it.id].orEmpty()) } ?: 0
    val doneToday = todays.count { today in store.habitsState.log[it.id].orEmpty() }
    val journalDays = remember(store.journalState.entry(store.today), today) { store.journalState.dates().toSet() }
    Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("Best streak", style = MaterialTheme.typography.labelMedium); Text("$best ${if (best == 1) "day" else "days"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) }
            Column { Text("Habits today", style = MaterialTheme.typography.labelMedium); Text("$doneToday of ${todays.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            Column { Text("Journal", style = MaterialTheme.typography.labelMedium); val j = dev.dawem.journal.journalStreak(journalDays, today); Text("$j ${if (j == 1) "day" else "days"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        }
    }
}

private val RoundedCornerShape10 = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
