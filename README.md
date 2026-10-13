# Lantern

Notes, a journal, tasks, habits and a money tracker in one private app. Your data is plain files in a folder you own, so it works offline, opens in any editor and moves between your phone and laptop by copying the folder.

Lantern runs on **Android** and **Linux** (Debian and Ubuntu) from one codebase.

## What is in it

- **Today**: mood, a journal entry for the day, today's habits and tasks, and the month's money at a glance.
- **Notes**: Markdown notes with tags, pinning and search.
- **Tasks**: a checklist with due dates (today, tomorrow, next week).
- **Habits**: daily or chosen weekdays, a 14-day grid you can tap, and streaks.
- **Money**: accounts, income and spending, monthly totals and a category breakdown.

## Your data

Everything lives in one vault folder (`~/Lantern` on Linux; `Lantern` on the phone):

```
notes/<title>-<id>.md          Markdown with a small header (id, title, tags, dates)
journal/<year>/<date>.md       one file a day: mood in the header, text below
tasks/Tasks.md                 a Markdown checklist
habits/habits.json, log.csv    what you track, and the days you did it
money/accounts.json, <year>.csv  accounts and transactions in minor units (cents)
.lantern/                      settings and the trash
```

Open the folder in Obsidian, a text editor or a spreadsheet; Lantern reads it back. To move to another device, copy the folder or sync it with any file-sync tool.

On Android the vault is private to the app by default. In the app you can allow *All files access* to keep it in a visible `/Lantern` folder instead.

## Install

Get the files for your system from the [latest release](../../releases/latest). Each file has a matching `.sha256` checksum.

| System | File |
|---|---|
| Android phone (64-bit, almost every phone since 2016) | `Lantern_<version>_Android_arm64-v8a.apk` |
| Android, older 32-bit | `Lantern_<version>_Android_armeabi-v7a.apk` |
| Android emulator, Chromebook | `Lantern_<version>_Android_x86_64.apk` |
| Debian, Ubuntu, Mint | `Lantern_<version>_Linux_amd64.deb` |

```
sha256sum -c Lantern_<version>_Linux_amd64.deb.sha256
sudo apt install ./Lantern_<version>_Linux_amd64.deb
```

## Build

You need JDK 17 or newer and, for Android, the Android SDK (platform 36).

```
./gradlew :shared:desktopTest            # the vault tests
./gradlew :composeApp:run                # run the desktop app
./gradlew :composeApp:assembleDebug      # Android debug APKs
./gradlew :composeApp:packageDeb         # Linux .deb
```

The code is split in two: `shared` holds the vault format and the logic (plain Kotlin, tested without a screen), and `composeApp` holds the screens for both platforms.

## Roadmap

Sync between devices, export to a zip, reminders, recurring tasks and transactions, charts, and search across everything from one place.

## Licence

MIT. See [LICENSE](LICENSE).
