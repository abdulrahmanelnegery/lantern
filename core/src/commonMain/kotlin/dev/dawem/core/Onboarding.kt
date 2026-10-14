package dev.dawem.core

/** What to show when Dawem opens: the first-install tour, the one-time notes after an update, or nothing. */
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
        "0.7.1" to listOf(
            "A new look: warm dark and ivory surfaces, an orange glow on the Home hero, rounded cards with a small diamond accent.",
            "A new About page, and exports now go to Zimam/Dawem/exports.",
        ),
        "0.7.0" to listOf(
            "A new Home page: today's progress, your week, mood, quick add, tasks, notes and a look back.",
            "Your vault now lives in one Zimam folder (Zimam/Dawem). Old folders are copied and checked, never deleted.",
            "A first-run tour, these notes after each update, and a better opening screen.",
        ),
        "0.6.1" to listOf("Internal tidy-up; nothing changes in how Dawem works."),
        "0.6.0" to listOf("Habits page with swipes, week and month views, and a Today page you can arrange."),
    )

    /** The notes for [version], or for the newest older version that has some. */
    fun forVersion(version: String): List<String> =
        notes[version] ?: notes.keys.filter { Onboarding.compare(it, version) <= 0 }.maxWithOrNull { a, b -> Onboarding.compare(a, b) }?.let { notes[it] }.orEmpty()
}
