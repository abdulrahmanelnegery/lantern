package dev.thabit.app

import dev.thabit.ui.*
import dev.thabit.core.Quotes
import dev.thabit.core.ReleaseNotes
import dev.thabit.core.Progress
import dev.thabit.core.dayProgress
import dev.thabit.core.daySummaries
import dev.thabit.core.totals
import dev.thabit.core.greetingFor
import dev.thabit.core.newId
import dev.thabit.core.upcomingTasks
import dev.thabit.core.weekStrip
import dev.thabit.habits.streak
import dev.thabit.tasks.Task
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalUriHandler
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** The Home cards the user can reorder or hide in Settings; the hero header on top is always there. */
val TodayCards = listOf(
    "capture" to "Quick add", "week" to "Your week", "habits" to "Habits today", "tasks" to "Tasks",
    "weekly" to "This week in numbers", "quote" to "Affirmation and tip", "streaks" to "Streaks", "whatsnew" to "What's new",
)

fun todayOrder(settings: dev.thabit.core.AppSettings): List<String> {
    val ids = TodayCards.map { it.first }
    return (settings.homeOrder.filter { it in ids } + ids).distinct()
}

/** The Home page: today's progress, your week, quick add, habits and tasks, as cards in the order set in Settings. */
@Composable
fun HomeScreen(store: ThabitStore, nav: Nav) {
    val today = store.today
    var shuffle by remember { mutableStateOf(0) }
    var newTask by remember { mutableStateOf(false) }
    val habits = store.habitsState.habits
    val progress = dayProgress(habits, store.habitsState.log, store.habitsState.skips, store.tasksState.tasks, today)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Hero(store, progress)
        if (store.showAtharNotice) AtharNoticeCard(store)
        todayOrder(store.settings).filter { it !in store.settings.homeHidden }.forEach { id ->
            when (id) {
                "capture" -> QuickAdd(onTask = { newTask = true }, onHabit = { nav.go(Section.Habits) })
                "week" -> WeekCard(store)
                "habits" -> HabitsCard(store)
                "tasks" -> TasksCard(store, nav)
                "weekly" -> WeeklyCard(store)
                "quote" -> HomeCard("Today", Icons.Rounded.Today, onClick = { shuffle++ }) {
                    Text(Quotes.affirmation(today, shuffle), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(Quotes.tip(today, shuffle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                "streaks" -> StreaksCard(store)
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
private fun Hero(store: ThabitStore, progress: Progress) {
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
private fun QuickAdd(onTask: () -> Unit, onHabit: () -> Unit) {
    HomeCard("Quick add", Icons.Rounded.TrendingUp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            QuickButton(Icons.Rounded.CheckCircle, "Task", onTask)
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
private fun WeekCard(store: ThabitStore) {
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
private fun HabitsCard(store: ThabitStore) {
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
private fun TasksCard(store: ThabitStore, nav: Nav) {
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
private fun WeeklyCard(store: ThabitStore) {
    val today = store.today
    val monday = today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)
    val sums = remember(today, store.tasksState.tasks, store.habitsState.log) {
        totals(daySummaries(monday, today, store.tasksState.tasks, store.habitsState.log))
    }
    HomeCard("This week in numbers", Icons.Rounded.TrendingUp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stat("Habits kept", sums.habitChecks.toString()); Stat("Tasks done", sums.tasks.toString())
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

/** The longest running habit streak and how many of today's habits are done. */
@Composable
private fun StreaksCard(store: ThabitStore) {
    val today = store.today
    val active = store.habitsState.habits.filter { !it.archived }
    val todays = active.filter { it.days.isEmpty() || today.dayOfWeek.ordinal + 1 in it.days }
    val best = active.maxOfOrNull { streak(it, store.habitsState.log[it.id].orEmpty(), today, store.habitsState.skips[it.id].orEmpty()) } ?: 0
    val doneToday = todays.count { today in store.habitsState.log[it.id].orEmpty() }
    HomeCard("Streaks", Icons.Rounded.TrendingUp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stat("Best streak", "$best ${if (best == 1) "day" else "days"}"); Stat("Habits today", "$doneToday of ${todays.size}")
        }
    }
}

private const val ATHAR_PACKAGE = "dev.athar.app"
private const val ATHAR_RELEASES = "https://github.com/zimamlabs/athar/releases"

/** Shown once to people whose vault has notes or journal files: those moved to Athar, and Thabit leaves the files alone. */
@Composable
private fun AtharNoticeCard(store: ThabitStore) {
    val platform = LocalPlatform.current
    val uri = LocalUriHandler.current
    HomeCard("Notes and journal moved to Athar", Icons.Rounded.NewReleases) {
        Text("Thabit is now for habits, tasks and insights. Your notes and journal stay in your Thabit folder untouched, and Athar can import them.", style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            TextButton(onClick = { store.saveSettings(store.settings.copy(atharNoticeDismissed = true)) }) { Text("Not now") }
            Button(onClick = { if (!platform.openInstalledApp(ATHAR_PACKAGE)) uri.openUri(ATHAR_RELEASES) }) { Text("Open Athar") }
        }
    }
}
