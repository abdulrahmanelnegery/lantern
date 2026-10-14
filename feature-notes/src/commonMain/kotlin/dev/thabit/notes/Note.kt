package dev.thabit.notes

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
