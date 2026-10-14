package dev.thabit.core

/** What to show when Thabit opens: the first-install tour, the one-time notes after an update, or nothing. */
enum class Welcome { Tour, WhatsNew, Nothing }

object Onboarding {
    /** `0.7.0` > `0.6.1`; missing or non-numeric parts count as 0. */
    fun compare(a: String, b: String): Int {
        val x = a.split('.').map { it.toIntOrNull() ?: 0 }
        val y = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 }.compareTo(y.getOrElse(i) { 0 })
            if (d != 0) return d
        }
        return 0
    }

    /**
     * [lastSeen] is the version whose notes were closed last (empty when never), [tourDone] whether the tour was finished
     * or skipped, [hasData] whether the vault already holds anything (an update from a version that did not store these
     * two values), [current] the installed version.
     */
    fun decide(lastSeen: String, tourDone: Boolean, hasData: Boolean, current: String): Welcome = when {
        !tourDone && lastSeen.isEmpty() && !hasData -> Welcome.Tour
        lastSeen.isNotEmpty() && compare(current, lastSeen) <= 0 -> Welcome.Nothing // same version, or a downgrade
        else -> Welcome.WhatsNew
    }
}

/** Short notes per version, taken from the changelog; shown once after an update. */
object ReleaseNotes {
    private val notes: Map<String, List<String>> = mapOf(
        "0.8.0" to listOf(
            "Reminders for habits and tasks: a notification that stays until you press Done or Later.",
            "A twelve-week habit heatmap and weekly completion bars in Insights.",
        ),
        "0.7.0" to listOf(
            "Dawem is now Thabit. Same app and data; your vault is copied into Zimam/Thabit and the old folder is kept as a backup.",
            "A new Home page with today's progress, your week, mood, quick add, tasks, notes and a look back, in a new warm look.",
            "Tasks can repeat and have steps, tags and a reminder time. A first-run tour and these notes after each update.",
        ),
        "0.6.1" to listOf("Internal tidy-up; nothing changes in how Thabit works."),
        "0.6.0" to listOf("Habits page with swipes, week and month views, and a Today page you can arrange."),
    )

    /** The notes for [version], or for the newest older version that has some. */
    fun forVersion(version: String): List<String> =
        notes[version] ?: notes.keys.filter { Onboarding.compare(it, version) <= 0 }.maxWithOrNull { a, b -> Onboarding.compare(a, b) }?.let { notes[it] }.orEmpty()
}
