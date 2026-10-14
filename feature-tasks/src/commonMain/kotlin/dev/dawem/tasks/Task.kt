package dev.dawem.tasks

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
)
