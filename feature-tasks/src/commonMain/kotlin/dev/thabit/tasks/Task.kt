package dev.thabit.tasks

import kotlinx.datetime.LocalDate

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
    /** "" (once), "daily", "weekly" or "monthly": finishing a repeating task makes the next one. */
    val repeat: String = "",
    val tags: List<String> = emptyList(),
    /** A reminder time on the due day, "HH:MM", or "" for none. */
    val remindAt: String = "",
    val subtasks: List<Subtask> = emptyList(),
)

/** One step inside a task. */
data class Subtask(val title: String, val done: Boolean = false)
