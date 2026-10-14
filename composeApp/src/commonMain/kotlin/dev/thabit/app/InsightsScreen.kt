package dev.thabit.app

import dev.thabit.ui.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.thabit.core.DaySummary
import dev.thabit.core.daySummaries
import dev.thabit.core.heatmap
import dev.thabit.core.weeklyRates
import dev.thabit.core.totals
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import dev.thabit.habits.habitStats
import dev.thabit.habits.streak

private enum class Period(val label: String) { Day("Day"), Week("Week"), Month("Month") }

private val moodFaces = listOf("😞", "😕", "😐", "🙂", "😄")

/** The whole picture for a day, a week or a month: what you wrote, did and kept up. */
@Composable
fun InsightsScreen(store: ThabitStore, onOpenHabit: (String) -> Unit) {
    var period by remember { mutableStateOf(Period.Week) }
    var back by remember { mutableStateOf(0) }
    val today = store.today
    // The range ends today (or the end of an earlier period) and starts a day, a week or a month before.
    val (from, to) = remember(period, back, today) {
        when (period) {
            Period.Day -> today.minus(back, DateTimeUnit.DAY).let { it to it }
            Period.Week -> today.minus(back * 7, DateTimeUnit.DAY).let { end -> end.minus(6, DateTimeUnit.DAY) to end }
            Period.Month -> today.minus(back, DateTimeUnit.MONTH).let { m -> LocalDate(m.year, m.monthNumber, 1) to LocalDate(m.year, m.monthNumber, 1).plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY) }
        }.let { (a, b) -> a to minOf(b, today) }
    }
    val days = remember(from, to, store.notesState.notes, store.tasksState.tasks, store.habitsState.log, store.journalState.entry(store.today)) {
        daySummaries(from, to, store.journalState::entry, store.notesState.notes, store.tasksState.tasks, store.habitsState.log)
    }
    val sum = totals(days)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Period.entries.forEach { p -> FilterChip(period == p, { period = p; back = 0 }, { Text(p.label) }) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { back++ }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Earlier") }
            Text(rangeLabel(period, from, to), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            IconButton(onClick = { back-- }, enabled = back > 0) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Later") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile2("Journal", "${sum.journalDays}", "of ${sum.days} days", Modifier.weight(1f))
            Tile2("Words", "${sum.words}", "written", Modifier.weight(1f))
            Tile2("Mood", sum.moodAverage?.let { moodFaces[(it.toInt() - 1).coerceIn(0, 4)] + " %.1f".format(it) } ?: "–", "average", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile2("Notes", "${sum.notes}", "written", Modifier.weight(1f))
            Tile2("Tasks", "${sum.tasks}", "done", Modifier.weight(1f))
            Tile2("Habits", "${sum.habitChecks}", "kept", Modifier.weight(1f))
        }

        if (days.size > 1) Panel {
            Text("Mood and habits by day", style = MaterialTheme.typography.titleSmall)
            val topHabits = (days.maxOfOrNull { it.habitsDone } ?: 0).coerceAtLeast(1)
            Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(if (days.size > 14) 2.dp else 6.dp), verticalAlignment = Alignment.Bottom) {
                days.forEach { d ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        if (days.size <= 14) Text(d.mood?.let { moodFaces[(it - 1).coerceIn(0, 4)] } ?: " ", style = MaterialTheme.typography.labelSmall)
                        Box(Modifier.fillMaxWidth().height((70f * d.habitsDone / topHabits).coerceAtLeast(2f).dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = if (d.habitsDone > 0) 0.9f else 0.25f)))
                        if (days.size <= 14) Text(d.date.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text("Bars are habits done; faces are the day's mood.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        val habits = store.habitsState.habits.filter { !it.archived }
        if (habits.isNotEmpty()) HabitCharts(store)
        if (habits.isNotEmpty()) Panel {
            Text("Habits", style = MaterialTheme.typography.titleSmall)
            habits.forEach { h ->
                val s = habitStats(h, store.habitsState.log[h.id].orEmpty(), today)
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).androidClick { onOpenHabit(h.id) }.padding(vertical = 6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(h.name, style = MaterialTheme.typography.bodyLarge)
                        Text("${(s.rate30 * 100).toInt()}%  ·  ${s.current} day streak", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        Box(Modifier.fillMaxWidth(s.rate30.toFloat().coerceIn(0f, 1f)).height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.primary))
                    }
                }
            }
            Text("Last 30 days. Tap a habit for its own analytics.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SectionTitle("History")
        val active = days.filter { !it.isEmpty }.asReversed()
        if (active.isEmpty()) EmptyHint("Nothing recorded", "Days with a journal entry, notes, tasks or habits show up here.")
        active.forEach { d -> DayRow(d) }
    }
}

@Composable
private fun DayRow(d: DaySummary) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(d.mood?.let { moodFaces[(it - 1).coerceIn(0, 4)] } ?: "·", style = MaterialTheme.typography.titleLarge)
            Text(prettyDate(d.date), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        }
        val parts = listOfNotNull(
            d.journalWords.takeIf { it > 0 }?.let { "$it words written" },
            d.notesCreated.takeIf { it > 0 }?.let { "$it ${if (it == 1) "note" else "notes"}" },
            d.tasksDone.takeIf { it > 0 }?.let { "$it ${if (it == 1) "task" else "tasks"} done" },
            d.habitsDone.takeIf { it > 0 }?.let { "$it ${if (it == 1) "habit" else "habits"} kept" },
        )
        Text(parts.joinToString("  ·  "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Tile2(label: String, value: String, note: String, modifier: Modifier) {
    Panel(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
        if (note.isNotEmpty()) Text(note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun rangeLabel(p: Period, from: LocalDate, to: LocalDate): String = when (p) {
    Period.Day -> prettyDate(from) + " " + from.year
    Period.Week -> "${prettyDate(from)} – ${prettyDate(to)}"
    Period.Month -> "${monthName(from.monthNumber)} ${from.year}"
}

private fun Modifier.androidClick(onClick: () -> Unit): Modifier = this.then(Modifier.clickable(onClick = onClick))

/** A twelve-week heatmap of how much of each day's habits you kept, and the average for each week as bars. */
@Composable
private fun HabitCharts(store: ThabitStore) {
    val map = remember(store.habitsState.habits, store.habitsState.log, store.habitsState.skips, store.today) {
        heatmap(store.habitsState.habits.filter { !it.archived }, store.habitsState.log, store.habitsState.skips, store.today)
    }
    val rates = remember(map) { weeklyRates(map) }
    val on = MaterialTheme.colorScheme.primary
    val off = MaterialTheme.colorScheme.outlineVariant
    Panel {
        Text("Habit heatmap", style = MaterialTheme.typography.titleSmall)
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(132.dp)) {
            val cols = map.size
            val gap = 4.dp.toPx()
            val cell = minOf((size.width - gap * (cols - 1)) / cols, (size.height - gap * 6) / 7)
            val left = (size.width - (cell * cols + gap * (cols - 1))) / 2
            map.forEachIndexed { c, week ->
                week.forEachIndexed { r, (_, p) ->
                    val color = if (p == null) off.copy(alpha = 0.25f) else if (p.fraction == 0f) off else on.copy(alpha = 0.25f + 0.75f * (p.fraction ?: 0f))
                    drawRoundRect(color, androidx.compose.ui.geometry.Offset(left + c * (cell + gap), r * (cell + gap)), androidx.compose.ui.geometry.Size(cell, cell), androidx.compose.ui.geometry.CornerRadius(cell * 0.28f))
                }
            }
        }
        Text("Each square is a day, newest week on the right; a stronger colour means more of that day's habits kept.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Weekly completion", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
            rates.forEach { r ->
                Box(Modifier.weight(1f).height((76f * (r ?: 0f)).coerceAtLeast(3f).dp).clip(RoundedCornerShape(4.dp)).background(if (r == null) off.copy(alpha = 0.4f) else on.copy(alpha = 0.35f + 0.65f * r)))
            }
        }
        Text("Average share of habits kept in each of the last 12 weeks.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
