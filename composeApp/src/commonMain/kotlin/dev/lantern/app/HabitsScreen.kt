package dev.lantern.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import dev.lantern.core.Habit
import dev.lantern.core.newId
import dev.lantern.core.streak
import kotlinx.datetime.DateTimeUnit
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import dev.lantern.core.HabitMark
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.minus

private enum class HabitView(val label: String) { Today("Today"), Week("Week"), Month("Month") }

@Composable
fun HabitsScreen(store: LanternStore, openId: String? = null, onOpen: (String) -> Unit = {}, onClose: () -> Unit = {}) {
    store.habits.firstOrNull { it.id == openId }?.let { HabitDetailScreen(store, it, onClose); return }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Habit?>(null) }
    var view by remember { mutableStateOf(HabitView.Today) }
    // How many weeks or months back from now the Week and Month views are showing (0 is this week or month).
    var back by remember { mutableStateOf(0) }
    var paged by remember { mutableStateOf(false) }
    val active = store.habits.filter { !it.archived }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = { FloatingActionButton(onClick = { adding = true }) { Icon(Icons.Rounded.Add, "New habit") } },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                HabitView.entries.forEachIndexed { i, v ->
                    SegmentedButton(selected = view == v, onClick = { view = v; back = 0 }, shape = SegmentedButtonDefaults.itemShape(i, HabitView.entries.size), label = { Text(v.label) })
                }
            }
            if (active.isEmpty()) EmptyHint("No habits yet", "Tap + to start one: reading, water, a walk. Swipe it right each day to build a streak.")
            when (view) {
                HabitView.Today -> TodayHabits(store, active, onOpen, onEdit = { editing = it })
                else -> PeriodHabits(store, active, month = view == HabitView.Month, back = back, onBack = { back = it }, paged = paged, onPaged = { paged = it }, onOpen = onOpen)
            }
        }
    }
    if (adding) NewHabitDialog("New habit", "", emptyList(), "Create", onDismiss = { adding = false }) { name, days ->
        store.saveHabits(store.habits + Habit(newId(), name.trim(), days))
        adding = false
    }
    editing?.let { h ->
        NewHabitDialog("Edit habit", h.name, h.days, "Save", onDismiss = { editing = null }) { name, days ->
            store.saveHabits(store.habits.map { if (it.id == h.id) it.copy(name = name.trim(), days = days) else it })
            editing = null
        }
    }
}

/** The habits that apply today: swipe right to mark one done, left to undo it, hold for skip, edit, archive and analytics. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun TodayHabits(store: LanternStore, active: List<Habit>, onOpen: (String) -> Unit, onEdit: (Habit) -> Unit) {
    val today = store.today
    val todays = active.filter { it.days.isEmpty() || today.dayOfWeek.ordinal + 1 in it.days }
    val rest = active - todays.toSet()
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(todays, key = { it.id }) { h ->
            val done = today in store.habitLog[h.id].orEmpty()
            val skipped = today in store.habitSkips[h.id].orEmpty()
            var menu by remember { mutableStateOf(false) }
            val state = rememberSwipeToDismissBoxState(confirmValueChange = { v ->
                when (v) {
                    SwipeToDismissBoxValue.StartToEnd -> store.setHabitMark(h.id, today, HabitMark.Done)
                    SwipeToDismissBoxValue.EndToStart -> store.setHabitMark(h.id, today, null)
                    else -> {}
                }
                false
            })
            SwipeToDismissBox(
                state = state,
                backgroundContent = {
                    val toRight = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
                    Box(
                        Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium).background(if (toRight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 20.dp),
                        contentAlignment = if (toRight) Alignment.CenterStart else Alignment.CenterEnd,
                    ) { Icon(if (toRight) Icons.Rounded.Check else Icons.Rounded.Undo, null, tint = if (toRight) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant) }
                },
            ) {
                Box {
                    Panel(modifier = Modifier.combinedClickable(onClick = { onOpen(h.id) }, onLongClick = { menu = true })) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                                if (done) Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                else if (skipped) Icon(Icons.Rounded.SkipNext, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                                Text(h.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                val s = streak(h, store.habitLog[h.id].orEmpty(), today, store.habitSkips[h.id].orEmpty())
                                Text(if (skipped) "Skipped today" else if (s > 0) "$s day streak" else "Swipe right when it is done", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text(if (skipped) "Clear the skip" else "Skip today") }, onClick = { menu = false; store.setHabitMark(h.id, today, if (skipped) null else HabitMark.Skipped) })
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onEdit(h) })
                        DropdownMenuItem(text = { Text("Analytics") }, onClick = { menu = false; onOpen(h.id) })
                        DropdownMenuItem(text = { Text("Archive") }, onClick = { menu = false; store.saveHabits(store.habits.map { if (it.id == h.id) it.copy(archived = true) else it }) })
                    }
                }
            }
        }
        if (rest.isNotEmpty()) {
            item { Text("Not today", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)) }
            items(rest, key = { "rest-" + it.id }) { h ->
                Panel(onClick = { onOpen(h.id) }) { Text(h.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

/** A week or a month of every habit, as a grid you can tap; page back through time, stacked or one habit at a time. */
@Composable
private fun PeriodHabits(store: LanternStore, active: List<Habit>, month: Boolean, back: Int, onBack: (Int) -> Unit, paged: Boolean, onPaged: (Boolean) -> Unit, onOpen: (String) -> Unit) {
    val today = store.today
    val first = if (month) {
        val m = today.monthNumber - 1 - back
        LocalDate(today.year + Math.floorDiv(m, 12), Math.floorMod(m, 12) + 1, 1)
    } else today.minus(today.dayOfWeek.ordinal + 7 * back, DateTimeUnit.DAY)
    val length = if (month) first.plus(1, DateTimeUnit.MONTH).minus(first).days else 7
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onBack(back + 1) }) { Icon(Icons.Rounded.ChevronLeft, "Earlier") }
        Text(
            if (month) "${monthName(first.monthNumber)} ${first.year}" else "${prettyDate(first)} to ${prettyDate(first.plus(6, DateTimeUnit.DAY))}",
            Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = { onBack((back - 1).coerceAtLeast(0)) }, enabled = back > 0) { Icon(Icons.Rounded.ChevronRight, "Later") }
        IconButton(onClick = { onPaged(!paged) }) { Icon(if (paged) Icons.Rounded.ViewAgenda else Icons.Rounded.ViewCarousel, if (paged) "Show all together" else "One habit at a time") }
    }
    @Composable fun grid(h: Habit) {
        Panel(onClick = { onOpen(h.id) }) {
            Text(h.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val days = store.habitLog[h.id].orEmpty(); val skips = store.habitSkips[h.id].orEmpty()
            val lead = if (month) first.dayOfWeek.ordinal else 0
            val cells = (0 until lead).map<Int, LocalDate?> { null } + (0 until length).map { first.plus(it, DateTimeUnit.DAY) }
            Row(Modifier.fillMaxWidth()) { listOf("M", "T", "W", "T", "F", "S", "S").forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center) } }
            cells.chunked(7).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    (0 until 7).forEach { i ->
                        val d = row.getOrNull(i)
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (d != null) {
                                val done = d in days; val skip = d in skips
                                val applies = h.days.isEmpty() || d.dayOfWeek.ordinal + 1 in h.days
                                Box(
                                    Modifier.size(30.dp).clip(CircleShape)
                                        .background(if (done) MaterialTheme.colorScheme.primary else if (skip) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f) else if (applies) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.background)
                                        .combinedClickable(
                                            enabled = d <= today,
                                            onClick = { store.setHabitMark(h.id, d, if (done) null else HabitMark.Done) },
                                            onLongClick = { store.setHabitMark(h.id, d, if (skip) null else HabitMark.Skipped) },
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) { Text("${d.dayOfMonth}", style = MaterialTheme.typography.labelSmall, color = if (done) MaterialTheme.colorScheme.onPrimary else if (d == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                }
            }
        }
    }
    if (paged && active.isNotEmpty()) {
        val pager = androidx.compose.foundation.pager.rememberPagerState { active.size }
        androidx.compose.foundation.pager.HorizontalPager(pager, Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), pageSpacing = 12.dp) { i -> grid(active[i]) }
    } else LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(active, key = { it.id }) { h -> grid(h) }
    }
}

@Composable
private fun NewHabitDialog(title: String, startName: String, startDays: List<Int>, action: String, onDismiss: () -> Unit, onCreate: (String, List<Int>) -> Unit) {
    var name by remember { mutableStateOf(startName) }
    var days by remember { mutableStateOf(startDays.toSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, singleLine = true, placeholder = { Text("What do you want to do?") }, modifier = Modifier.fillMaxWidth())
                Text(if (days.isEmpty()) "Every day" else "On the days you pick", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { i, label ->
                        FilterChip(i + 1 in days, { days = if (i + 1 in days) days - (i + 1) else days + (i + 1) }, { Text(label) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(name, days.sorted()) }, enabled = name.isNotBlank()) { Text(action) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
