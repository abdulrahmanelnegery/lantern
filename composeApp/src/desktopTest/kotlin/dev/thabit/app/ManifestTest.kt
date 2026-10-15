package dev.thabit.app

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** The Android manifest is plain XML, so what the app must declare can be pinned without a device. */
class ManifestTest {
    private val manifest = File("src/androidMain/AndroidManifest.xml").readText()

    private fun filterActions(receiver: String): List<String> {
        val block = Regex("""<receiver android:name="dev\.thabit\.app\.$receiver"[\s\S]*?</receiver>""").find(manifest)?.value ?: return emptyList()
        return Regex("""<action android:name="([^"]+)"""").findAll(block).map { it.groupValues[1] }.toList()
    }

    @Test
    fun remindersAreSetAgainAfterRestartTimeChangeAndAppUpdate() {
        val actions = filterActions("ReminderBootReceiver")
        for (a in listOf("BOOT_COMPLETED", "TIMEZONE_CHANGED", "TIME_SET", "MY_PACKAGE_REPLACED")) {
            assertTrue("android.intent.action.$a" in actions, "the reminder receiver must listen for $a, found $actions")
        }
    }
}
