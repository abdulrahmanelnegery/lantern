<p align="center"><img src="docs/icon.png" width="120" alt="Thabit"></p>

<h1 align="center">Thabit</h1>

<p align="center">Private notes, journal, tasks and habits in plain files, by <a href="https://github.com/zimamlabs">Zimam Labs</a>. Thabit, formerly Dawem and Lantern.</p>

<p align="center">
  <a href="../../releases/latest"><img alt="Release" src="https://img.shields.io/github/v/release/zimamlabs/thabit?color=FF5A1F&labelColor=1A1613"></a>
  <a href="../../releases"><img alt="Downloads" src="https://img.shields.io/github/downloads/zimamlabs/thabit/total?color=FF5A1F&labelColor=1A1613"></a>
  <a href="LICENSE"><img alt="Licence: MIT" src="https://img.shields.io/badge/licence-MIT-FF5A1F?labelColor=1A1613"></a>
  <img alt="Android" src="https://img.shields.io/badge/Android-8.0%2B-FF5A1F?labelColor=1A1613">
</p>

## What it is

Notes, a journal, tasks and habits in one private app. Your data is plain files in a folder you own, so it works offline, opens in any editor and moves between your phone and laptop by copying the folder.

Thabit (ثابت, "steady") is an **Android** app today; Linux comes later. The desktop code stays in the repository and is tested, but no desktop package is published.

## Features

- **Home**: a hero with today's progress ring, a quick-add row, your week at a glance, today's habits, upcoming and overdue tasks, a mood line for the last 14 days, recent and pinned notes, "on this day" from earlier years, a weekly summary, an affirmation and tip, streaks and what's new; the cards are in an order you choose and can be hidden. The sections (Notes, Journal, Tasks, Habits, Insights, Search) are one tap away.
- **First start**: a short tour (reopen it from Settings or About) and a one-time "what's new" after each update.
- **Notes**: Markdown notes with tags, pinning and search; a reading view and an edit view.
- **Journal**: one entry a day with a mood, and a look back through the days.
- **Tasks**: lists and a checklist with due dates (today, tomorrow, next week) and priorities; repeat daily, weekly or monthly (finishing one leaves the next open); steps inside a task, tags and a reminder time.
- **Habits**: daily or chosen weekdays; swipe to check or undo, hold to skip; week and month views, streaks that skip days you skip on purpose, and a page for each habit with its history.
- **The Zimam look**: a splash screen, an About page and the Zimam icon.
- **Insights**: a day, week or month in numbers: what you wrote, did and kept up, with mood and habits by day, plus a twelve-week habit heatmap and weekly completion bars.
- **Reminders** (Android): give a habit or a task a time and a notification appears then and stays until you press Done (ticks the habit or finishes the task) or Later (ten minutes). Alarms are set again after a restart; notification permission is asked for only when a reminder exists, and without exact-alarm permission a reminder can come a few minutes late.
- **Search** across notes, journal entries and tasks, from one place.
- **Themes**: Zimam (warm paper and near-black with an orange accent) and five more palettes, each in light and dark, or follow the system.
- **Export** the whole vault as a zip from Settings.

## Languages

English today. Arabic and right-to-left screens are being added; this line will say when they are in.

## Screenshots

Screenshots: coming with the next release.

## Download

Get the files for your system from the [latest release](../../releases/latest). Each file has a matching `.sha256` checksum.

| System | File |
|---|---|
| Android phone (64-bit, almost every phone since 2016) | `Thabit_<version>_Android_arm64-v8a.apk` |
| Android, older 32-bit | `Thabit_<version>_Android_armeabi-v7a.apk` |
| Android emulator, Chromebook | `Thabit_<version>_Android_x86_64.apk` |

```
sha256sum -c Thabit_<version>_Android_arm64-v8a.apk.sha256
```

## Verify

Every APK is signed with Thabit's own key. Android installs it without a store because the signature is valid, and shows its usual "unknown source" notice for anything that did not come from Google Play. To check a file is really ours, compare its signing certificate with this fingerprint:

```
SHA-256: 09:7A:B4:82:7A:35:AF:FB:5A:0D:D5:AB:01:26:EA:77:10:6B:29:4A:83:B4:E6:A8:B8:32:91:33:64:F0:EB:EA
```

```
apksigner verify --print-certs Thabit_<version>_Android_arm64-v8a.apk
sha256sum -c Thabit_<version>_Android_arm64-v8a.apk.sha256
```

## Where your data lives

Everything lives in one vault folder inside a `Zimam` folder (`~/Zimam/Thabit` on Linux; `Zimam/Thabit` on the phone):

```
notes/<title>-<id>.md          Markdown with a small header (id, title, tags, dates)
journal/<year>/<date>.md       one file a day: mood in the header, text below
tasks/Tasks.md                 a Markdown checklist
habits/habits.json, log.csv    what you track, and the days you did it
.lantern/                      settings and the trash (the folder keeps its old name so existing vaults keep working)
```

Open the folder in Obsidian, a text editor or a spreadsheet; Thabit reads it back. To move to another device, copy the folder or sync it with any file-sync tool.

On Android the vault is private to the app by default. In the app you can allow *All files access* to keep it in the visible `Zimam/Thabit` folder instead. Older folders (`Thabit`, `Lantern`, the private vault) are copied in, every file is checked by count, size and SHA-256, and the old folder is kept as a backup: nothing is deleted, a file that already exists is never overwritten (both are kept), and if a check fails Thabit keeps using the old folder. *Settings* has a button to remove an old folder once it has been checked again. Vault exports are saved in an `exports` folder inside the vault (and are left out of the next export), so every visible file stays under `Zimam/Thabit`.

## Privacy

No account, no ads, no tracking. Thabit keeps your data on your phone in plain files you can open anywhere, and sends nothing over the network.

## Build

You need JDK 17 or newer and, for Android, the Android SDK (platform 36).

```
./gradlew :core:desktopTest :shared:desktopTest :feature-notes:desktopTest :feature-journal:desktopTest :feature-tasks:desktopTest :feature-habits:desktopTest :composeApp:desktopTest   # the vault and feature tests
./gradlew :composeApp:run                # run the desktop app
./gradlew :composeApp:assembleDebug      # Android debug APKs
```

The code is split into modules, each with one job:

| Module | What it holds |
|---|---|
| `core` | Plain-file helpers every feature shares: reading and atomic writing, front matter, CSV, ids, settings, the zip export |
| `ui` | The theme, shared components, the Markdown view and the back handler |
| `feature-notes`, `feature-journal`, `feature-tasks`, `feature-habits` | One feature each: its model, its files in the vault, its state and its screens |
| `shared` | The `Vault` facade that joins the features, and the cross-feature numbers for Insights |
| `composeApp` | The app: Home, Search, Insights, Settings, About, navigation and the Android and desktop entry points |

A feature depends only on `core` and `ui`, never on another feature; `scripts/check-modules.sh` checks this in CI. The vault format is plain files and does not depend on the module layout.

## Roadmap

Widgets, sync between devices and more insight charts. Notes and the journal will move to their own app, Athar, later; the files in your vault's `notes/` and `journal/` folders will stay untouched. Money tracking left Thabit and became its own app.

## Licence

MIT. See [LICENSE](LICENSE).
