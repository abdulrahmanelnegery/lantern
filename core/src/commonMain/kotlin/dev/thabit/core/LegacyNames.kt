package dev.thabit.core

/** Old names, kept only to find and migrate existing data. Nothing else in the code or the interface uses them. */
object LegacyNames {
    /** Folders (relative to the storage root or home) where earlier versions kept a vault, newest first. */
    val oldVaultFolders = listOf("Zimam/Dawem", "Dawem", "Lantern")

    /** Names an earlier version gave the app's private vault folder. */
    val oldPrivateFolders = listOf("Dawem", "Lantern")
}
