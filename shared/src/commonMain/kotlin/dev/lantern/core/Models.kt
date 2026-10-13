package dev.lantern.core

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: String,
    val title: String,
    val body: String,
    val created: String,
    val updated: String,
    val tags: List<String> = emptyList(),
    val pinned: Boolean = false,
)

/** One day's journal: free text, a mood from 1 (low) to 5 (great), and tags. */
data class JournalEntry(val date: LocalDate, val body: String, val mood: Int? = null, val tags: List<String> = emptyList())

data class Task(
    val id: String,
    val title: String,
    val done: Boolean = false,
    val due: LocalDate? = null,
    /** 0 none, 1 low, 2 medium, 3 high. */
    val priority: Int = 0,
    val list: String = "",
    /** The day it was ticked off, for history. */
    val doneOn: LocalDate? = null,
)

@Serializable
data class Habit(
    val id: String,
    val name: String,
    /** Days of the week it applies to, 1 = Monday .. 7 = Sunday; empty means every day. */
    val days: List<Int> = emptyList(),
    val archived: Boolean = false,
)

/** How the app looks; stored in the vault's `.lantern/settings.json` so it travels with the data. */
@Serializable
data class AppSettings(
    /** One of the palette ids the app knows; an unknown id falls back to the default. */
    val palette: String = "zimam",
    /** "system", "light" or "dark". */
    val mode: String = "system",
    /** The cards on the Today page, in order; an id not listed here is added at the end when the app knows it. */
    val homeOrder: List<String> = emptyList(),
    /** Cards hidden from the Today page. */
    val homeHidden: List<String> = emptyList(),
)

/** What a habit's day was marked as. A day with no mark is simply not done (yet). */
enum class HabitMark { Done, Skipped }
