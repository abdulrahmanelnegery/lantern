package dev.thabit.app

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The README says nothing leaves the phone: no network permission and no Google Auto Backup. */
class ManifestPrivacyTest {
    private val manifest = File("src/androidMain/AndroidManifest.xml").readText()

    @Test
    fun autoBackupIsOff() {
        assertTrue("""android:allowBackup="false"""" in manifest)
    }

    @Test
    fun theAppHasNoNetworkPermission() {
        assertFalse("android.permission.INTERNET" in manifest)
    }
}
