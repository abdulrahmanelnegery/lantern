package dev.dawem.core

import kotlinx.datetime.LocalDate

/** Short affirmations for the Today page; one per day, and a tap shows another. */
object Quotes {
    val affirmations = listOf(
        "You do not have to do it all today. Do the next small thing.",
        "Showing up counts, even on the slow days.",
        "Small steps, kept up, add up to a different life.",
        "Be as kind to yourself as you would be to a friend.",
        "Progress is not a straight line, and that is fine.",
        "You have done hard things before. This is one more.",
        "Rest is part of the work.",
        "Start before you feel ready.",
        "One honest page is worth more than a perfect plan.",
        "Today is a fresh page.",
        "You are allowed to change your mind and begin again.",
        "A little every day beats a lot once in a while.",
        "Notice what went well, however small.",
        "Done is a good place to start from.",
        "Your pace is the right pace.",
        "Breathe. You are exactly where you need to begin.",
        "What you do most days matters more than what you do some days.",
        "Be patient with the version of you that is still learning.",
        "Gratitude turns what we have into enough.",
        "Choose the thing that your future self will thank you for.",
        "Courage is taking the step while still unsure.",
        "You can write the next line without knowing the whole story.",
        "Let today be simple.",
        "Every habit starts as a decision you repeat.",
        "Keep the promise you made to yourself.",
        "Slow is smooth, and smooth becomes fast.",
        "The hardest part is opening the page. You are already here.",
        "Tidy one corner and the room feels lighter.",
        "Write it down, so you can let it go.",
        "Look how far you have come, not only how far there is to go.",
    )

    val tips = listOf(
        "Tie a new habit to one you already have: after coffee, read a page.",
        "Make the habit so small it is hard to skip: two minutes, not twenty.",
        "Missed a day? Do not miss two. Skipping on purpose keeps your streak honest.",
        "Write tomorrow's top three tasks tonight, then close the day.",
        "Put the date and one line in your journal even on a busy day.",
        "Tag notes as you write them; future you will search by tag.",
        "Review your week on Sunday: what worked, what to drop.",
        "Write it down now; it takes ten seconds now and an hour later.",
        "Keep tasks short and starting with a verb: call, write, buy.",
        "Hold a habit to see its options: skip a day, edit it, or open its analytics.",
        "Swipe a habit right to check it and left to undo.",
        "Copy your vault folder to your laptop and open it in any editor; it is all plain files.",
    )

    /** One affirmation for the day; [shuffle] picks another for the same day. */
    fun affirmation(date: LocalDate, shuffle: Int = 0): String = affirmations[Math.floorMod(date.toEpochDays() * 7 + shuffle, affirmations.size)]

    fun tip(date: LocalDate, shuffle: Int = 0): String = tips[Math.floorMod(date.toEpochDays() * 5 + shuffle, tips.size)]
}
