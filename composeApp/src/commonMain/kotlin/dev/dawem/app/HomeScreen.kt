package dev.dawem.app

import dev.dawem.ui.*
import dev.dawem.core.Quotes
import dev.dawem.core.ReleaseNotes
import dev.dawem.core.Progress
import dev.dawem.core.dayProgress
import dev.dawem.core.daySummaries
import dev.dawem.core.greetingFor
import dev.dawem.core.moodSeries
import dev.dawem.core.newId
import dev.dawem.core.onThisDay
import dev.dawem.core.totals
import dev.dawem.core.upcomingTasks
import dev.dawem.core.weekOf
import dev.dawem.core.weekStrip
import dev.dawem.habits.streak
import dev.dawem.journal.JournalPanel
import dev.dawem.journal.journalStreak
import dev.dawem.tasks.Task
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate

/** The Home cards the user can reorder or hide in Settings; the hero header on top is always there. */
val TodayCards = listOf(
    "capture" to "Quick add", "week" to "Your week", "habits" to "Habits today", "tasks" to "Tasks",
    "mood" to "Mood, last 14 days", "notes" to "Notes", "onthisday" to "On this day", "weekly" to "This week in numbers",
    "quote" to "Affirmation and tip", "streaks" to "Streaks", "journal" to "Journal", "whatsnew" to "What's new",
)

fun todayOrder(settings: dev.dawem.core.AppSettings): List<String> {
    val ids = TodayCards.map { it.first }
    return (settings.homeOrder.filter { it in ids } + ids).distinct()
}

/** The Home page: today's progress, your week, mood, quick add, tasks, notes and a look back, as cards in the order set in Settings. */
@Composable
fun HomeScreen(store: DawemStore, nav: Nav) {
    val today = store.today
    var shuffle by remember { mutableStateOf(0) }
    var newTask by remember { mutableStateOf(false) }
    val habits = store.habitsState.habits
    val progress = dayProgress(habits, store.habitsState.log, store.habitsState.skips, store.tasksState.tasks, today)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Hero(store, progress)
        todayOrder(store.settings).filter { it !in store.settings.homeHidden }.forEach { id ->
            when (id) {
                "capture" -> QuickAdd(
                    onNote = { nav.go(Section.Notes) }, onTask = { newTask = true },
                    onJournal = { nav.screen = Screen.Journal }, onHabit = { nav.go(Section.Habits) },
                )
                "week" -> WeekCard(store)
                "habits" -> HabitsCard(store)
                "tasks" -> TasksCard(store, nav)
                "mood" -> MoodCard(store)
                "notes" -> NotesCard(store, nav)
                "onthisday" -> OnThisDayCard(store, nav)
                "weekly" -> WeeklyCard(store)
                "quote" -> HomeCard("Today", Icons.Rounded.Today, onClick = { shuffle++ }) {
                    Text(Quotes.affirmation(today, shuffle), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(Quotes.tip(today, shuffle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                "streaks" -> StreaksCard(store)
                "journal" -> JournalPanel(store.journalState, today)
                "whatsnew" -> HomeCard("What's new in ${Brand.VERSION}", Icons.Rounded.NewReleases) {
                    ReleaseNotes.forVersion(Brand.VERSION).take(3).forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    if (newTask) NameDialog("New task", "", "Add", onDismiss = { newTask = false }) { name ->
        newTask = false
        if (name.isNotBlank()) store.tasksState.save(store.tasksState.tasks + Task(newId(), name.trim(), due = today))
    }
}

/** A Panel with an icon and a title; every Home card uses it so they read as one set. */
@Composable
fun HomeCard(title: String, icon: ImageVector, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Panel(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        content()
    }
}

@Composable
private fun Hero(store: DawemStore, progress: Progress) {
    val hour = store.hour
    Glow(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))) {
        Panel(Modifier.padding(0.dp)) {
            Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(greetingFor(hour), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(prettyDate(store.today), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        if (progress.total == 0) "Nothing planned yet. Add a habit or a task."
                        else if (progress.done == progress.total) "All done for today. Well kept."
                        else "${progress.total - progress.done} left for today",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ProgressRing(progress.fraction ?: 0f, 88.dp, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.outlineVariant) {
                    Text(if (progress.total == 0) "-" else "${progress.done}/${progress.total}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** A ring that fills clockwise from the top to [fraction]; [center] sits inside. */
@Composable
fun ProgressRing(fraction: Float, size: androidx.compose.ui.unit.Dp, color: Color, track: Color, stroke: androidx.compose.ui.unit.Dp = 9.dp, center: @Composable () -> Unit = {}) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val s = Size(this.size.width - w, this.size.height - w)
            val tl = Offset(w / 2, w / 2)
            drawArc(track, 0f, 360f, false, tl, s, style = Stroke(w))
            if (fraction > 0f) drawArc(color, -90f, 360f * fraction.coerceIn(0f, 1f), false, tl, s, style = Stroke(w, cap = StrokeCap.Round))
        }
        center()
    }
}

@Composable
private fun QuickAdd(onNote: () -> Unit, onTask: () -> Unit, onJournal: () -> Unit, onHabit: () -> Unit) {
    HomeCard("Quick add", Icons.Rounded.TrendingUp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            QuickButton(Icons.Rounded.Description, "Note", onNote)
            QuickButton(Icons.Rounded.CheckCircle, "Task", onTask)
            QuickButton(Icons.Rounded.AutoStories, "Journal", onJournal)
            QuickButton(Icons.Rounded.Loop, "Habit", onHabit)
        }
    }
}

@Composable
private fun QuickButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) { Icon(icon, label, tint = MaterialTheme.colorScheme.onPrimary) }
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun WeekCard(store: DawemStore) {
    val today = store.today
    val strip = weekStrip(store.habitsState.habits, store.habitsState.log, store.habitsState.skips, today)
    HomeCard("Your week", Icons.Rounded.Today) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            strip.forEach { (d, p) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(dayName(d.dayOfWeek).take(3), style = MaterialTheme.typography.labelSmall, color = if (d == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (d == today) FontWeight.Bold else FontWeight.Normal)
                    ProgressRing(if (d > today) 0f else p.fraction ?: 0f, 34.dp, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.outlineVariant, 4.dp) {
                        Text(d.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitsCard(store: DawemStore) {
    val today = store.today
    val todays = store.habitsState.habits.filter { !it.archived && (it.days.isEmpty() || today.dayOfWeek.ordinal + 1 in it.days) }
    if (todays.isEmpty()) return
    HomeCard("Habits today", Icons.Rounded.Loop) {
        todays.forEach { h ->
            val days = store.habitsState.log[h.id].orEmpty()
            val done = today in days
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { store.habitsState.setDone(h.id, today, !done) }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null, Modifier.size(28.dp), tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                Text(h.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                val s = streak(h, days, today, store.habitsState.skips[h.id].orEmpty())
                if (s > 0) Text("$s day streak", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun TasksCard(store: DawemStore, nav: Nav) {
    val today = store.today
    val list = upcomingTasks(store.tasksState.tasks, today)
    if (list.isEmpty()) return
    HomeCard("Upcoming and overdue", Icons.Rounded.CheckCircle, onClick = { nav.go(Section.Tasks) }) {
        list.forEach { t ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(false, { store.tasksState.complete(t.id, true, today) })
                Column(Modifier.weight(1f)) {
                    Text(t.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    t.due?.let { d ->
                        val late = d < today
                        Text(if (late) "Overdue: ${prettyDate(d)}" else if (d == today) "Today" else prettyDate(d), style = MaterialTheme.typography.labelSmall, color = if (late) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodCard(store: DawemStore) {
    val today = store.today
    val dates = store.journalState.dates()
    val series = remember(dates, today, store.journalState.entry(today)) { moodSeries({ store.journalState.entry(it)?.mood }, today) }
    val moods = series.filterNotNull()
    HomeCard("Mood, last 14 days", Icons.Rounded.EmojiEmotions, onClick = { /* the journal records it */ }) {
        if (moods.isEmpty()) { Text("Add a mood to your journal entries and it shows up here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); return@HomeCard }
        val line = MaterialTheme.colorScheme.primary
        val grid = MaterialTheme.colorScheme.outlineVariant
        Canvas(Modifier.fillMaxWidth().height(72.dp)) {
            val stepX = size.width / (series.size - 1).coerceAtLeast(1)
            fun y(m: Int) = size.height - (m - 1) / 4f * size.height * 0.84f - size.height * 0.08f
            for (m in 1..5 step 2) drawLine(grid, Offset(0f, y(m)), Offset(size.width, y(m)), 1f)
            val path = Path()
            var started = false
            series.forEachIndexed { i, m -> if (m != null) { val p = Offset(i * stepX, y(m)); if (!started) { path.moveTo(p.x, p.y); started = true } else path.lineTo(p.x, p.y) } }
            drawPath(path, line, style = Stroke(4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            series.forEachIndexed { i, m -> if (m != null) drawCircle(line, 6f, Offset(i * stepX, y(m))) }
        }
        Text("Average ${"%.1f".format(moods.average())} of 5 over ${moods.size} ${if (moods.size == 1) "entry" else "entries"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NotesCard(store: DawemStore, nav: Nav) {
    val notes = store.notesState.notes.take(4)
    if (notes.isEmpty()) return
    HomeCard("Notes", Icons.Rounded.Description, onClick = { nav.go(Section.Notes) }) {
        notes.forEach { n ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (n.pinned) Icon(Icons.Rounded.PushPin, "Pinned", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Text(n.title.ifBlank { "Untitled" }, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(n.updated.take(10), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun OnThisDayCard(store: DawemStore, nav: Nav) {
    val dates = onThisDay(store.journalState.dates(), store.today)
    if (dates.isEmpty()) return
    HomeCard("On this day", Icons.Rounded.History, onClick = { nav.screen = Screen.Journal }) {
        dates.take(2).forEach { d ->
            val e = store.journalState.entry(d) ?: return@forEach
            Column {
                Text("${d.year}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(e.body.trim().lineSequence().firstOrNull { it.isNotBlank() } ?: "(no text)", style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun WeeklyCard(store: DawemStore) {
    val today = store.today
    val (monday, _) = weekOf(today)
    val sums = remember(today, store.notesState.notes, store.tasksState.tasks, store.habitsState.log, store.journalState.dates()) {
        totals(daySummaries(monday, today, { store.journalState.entry(it) }, store.notesState.notes, store.tasksState.tasks, store.habitsState.log))
    }
    HomeCard("This week in numbers", Icons.Rounded.TrendingUp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("Words written", sums.words.toString()); Stat("Habits kept", sums.habitChecks.toString()); Stat("Tasks done", sums.tasks.toString()); Stat("Notes", sums.notes.toString())
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val journalDays = remember(store.journalState.entry(today), today) { store.journalState.dates().toSet() }
    val j = journalStreak(journalDays, today)
    HomeCard("Streaks", Icons.Rounded.TrendingUp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("Best streak", "$best ${if (best == 1) "day" else "days"}"); Stat("Habits today", "$doneToday of ${todays.size}"); Stat("Journal", "$j ${if (j == 1) "day" else "days"}")
        }
    }
}
