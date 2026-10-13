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
)

@Serializable
data class Habit(
    val id: String,
    val name: String,
    /** Days of the week it applies to, 1 = Monday .. 7 = Sunday; empty means every day. */
    val days: List<Int> = emptyList(),
    val archived: Boolean = false,
)

@Serializable
data class Account(val id: String, val name: String, val currency: String = "USD", val openingMinor: Long = 0)

/** Money amounts are whole minor units (cents): positive is income, negative is spending. */
data class Txn(
    val id: String,
    val date: LocalDate,
    val amountMinor: Long,
    val accountId: String,
    val category: String = "",
    val note: String = "",
)
