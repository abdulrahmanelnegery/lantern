package dev.thabit.app

import dev.thabit.core.AppSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TodayOrderTest {
    @Test fun cardsRemovedWithNotesAndJournalDropOutOfASavedOrder() {
        val saved = AppSettings(homeOrder = listOf("journal", "mood", "tasks", "notes", "onthisday", "habits"), homeHidden = listOf("journal"))
        val order = todayOrder(saved)
        assertEquals(listOf("tasks", "habits"), order.take(2), "the user's order of the cards that remain is kept")
        for (gone in listOf("journal", "mood", "notes", "onthisday")) assertFalse(gone in order, "$gone is gone")
        assertEquals(TodayCards.map { it.first }.toSet(), order.toSet(), "every current card is still listed once")
        assertEquals(order.size, order.distinct().size)
    }
}
