package dev.dawem.habits

import kotlinx.serialization.Serializable

@Serializable
data class Habit(
    val id: String,
    val name: String,
    /** Days of the week it applies to, 1 = Monday .. 7 = Sunday; empty means every day. */
    val days: List<Int> = emptyList(),
    val archived: Boolean = false,
)

/** What a habit's day was marked as. A day with no mark is simply not done (yet). */
enum class HabitMark { Done, Skipped }
