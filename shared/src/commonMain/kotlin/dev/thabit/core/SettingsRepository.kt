package dev.thabit.core

/** The one place settings are read and written: always normalised, always saved through the vault's atomic write. */
class SettingsRepository(private val vault: Vault) {
    var current: AppSettings = vault.settings().normalized()
        private set

    fun reload(): AppSettings { current = vault.settings().normalized(); return current }

    fun update(change: (AppSettings) -> AppSettings): AppSettings {
        val next = change(current).normalized()
        vault.saveSettings(next)
        current = next
        return next
    }
}
