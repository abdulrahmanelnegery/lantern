package dev.dawem.habits

import dev.dawem.ui.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import dev.dawem.habits.appliesOn
import dev.dawem.habits.Habit
import dev.dawem.habits.habitStats

/** One habit: streaks, how often you keep it, the days of the week you do it, and a month you can edit. */
@Composable
fun HabitDetailScreen(habitsState: HabitsState, today: kotlinx.datetime.LocalDate, habit: Habit, onBack: () -> Unit) {
    PlatformBackHandler(true, onBack)
    val done = habitsState.log[habit.id].orEmpty()
    val stats = habitStats(habit, done, today)
    var month by remember { mutableStateOf(LocalDate(today.year, today.monthNumber, 1)) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            Column(Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(if (habit.days.isEmpty()) "Every day" else habit.days.joinToString(" ") { dayName(kotlinx.datetime.DayOfWeek(it)) }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile("Streak", "${stats.current}", "days", Modifier.weight(1f))
            Tile("Best", "${stats.best}", "days", Modifier.weight(1f))
            Tile("Total", "${stats.total}", "times", Modifier.weight(1f))
        }
        Panel {
            Text("How often you keep it", style = MaterialTheme.typography.titleSmall)
            Rate("Last 7 days", stats.rate7)
            Rate("Last 30 days", stats.rate30)
            Rate("Last 90 days", stats.rate90)
        }
        Panel {
            Text("Days of the week", style = MaterialTheme.typography.titleSmall)
            val top = (stats.byWeekday.maxOrNull() ?: 0).coerceAtLeast(1)
            Row(Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                stats.byWeekday.forEachIndexed { i, n ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Text("$n", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(Modifier.fillMaxWidth().height((64f * n / top).coerceAtLeast(3f).dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = if (n == top) 1f else 0.55f)))
                        Text(dayName(kotlinx.datetime.DayOfWeek(i + 1)).take(1), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = month.minus(1, DateTimeUnit.MONTH) }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Earlier month") }
                Text("${monthName(month.monthNumber)} ${month.year}", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(onClick = { month = month.plus(1, DateTimeUnit.MONTH) }, enabled = month < LocalDate(today.year, today.monthNumber, 1)) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Later month") }
            }
            MonthGrid(month, today, done, habit) { d -> habitsState.setDone(habit.id, d, d !in done) }
            Text("Tap a day to mark or unmark it.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Tile(label: String, value: String, unit: String, modifier: Modifier) {
    Panel(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Rate(label: String, rate: Double) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("${(rate * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Box(Modifier.fillMaxWidth(rate.toFloat().coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary))
        }
    }
}

/** A calendar month: days done are filled, days the habit does not apply to are faint. */
@Composable
fun MonthGrid(month: LocalDate, today: LocalDate, done: Set<LocalDate>, habit: Habit, onTap: (LocalDate) -> Unit) {
    val lead = month.dayOfWeek.ordinal
    val length = month.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth
    Row(Modifier.fillMaxWidth()) {
        (1..7).forEach { Text(dayName(kotlinx.datetime.DayOfWeek(it)).take(1), Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    val cells = lead + length
    for (row in 0 until (cells + 6) / 7) {
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            for (col in 0 until 7) {
                val n = row * 7 + col - lead + 1
                Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                    if (n in 1..length) {
                        val d = LocalDate(month.year, month.monthNumber, n)
                        val future = d > today
                        val isDone = d in done
                        Box(
                            Modifier.fillMaxSize().clip(CircleShape)
                                .background(if (isDone) MaterialTheme.colorScheme.primary else if (habit.appliesOn(d) && !future) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.background)
                                .clickable(enabled = !future) { onTap(d) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$n", style = MaterialTheme.typography.labelMedium,
                                color = if (isDone) MaterialTheme.colorScheme.onPrimary else if (future || !habit.appliesOn(d)) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (d == today) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }
    }
}
