# Changelog

## 0.5.1

- **Zimam** is the new default theme: warm paper in light, near-black in dark, with an orange accent. The five earlier palettes stay in Settings, and anyone who picked one keeps it.

## 0.5.0

- **Lantern is now Dawem** (داوم, "keep at it"), made by [Zimam](https://github.com/zimamlabs): a new name, a new icon (a loop and a semi-square), a short splash screen, "by Zimam" in the side menu and an **About** page.
- The vault folder is now `Dawem` (`~/Dawem` on Linux, `/Dawem` on the phone). On first start the old `Lantern` folder is renamed, so nothing is copied or lost. Inside the vault, the hidden `.lantern` settings folder keeps its name, so a vault opens the same in older versions.
- Release files start with `Dawem_`. The Linux package is now called `dawem`; remove the old `lantern-notes` package after installing it.
- The Android package id is unchanged, so installed copies update in place.

## 0.4.0

- Themes: five palettes (Amber, Ocean, Forest, Rose, Slate), each with a light and a dark version, and a System / Light / Dark switch. Pick them in Settings. The choice is saved in the vault (`.lantern/settings.json`), so it follows the data to another device.

## 0.3.0

- Insights (in the side menu): a day, a week or a month at a time, with journal days and words, average mood, notes written, tasks done and money spent, a chart of habits and mood by day, every habit's 30-day rate, and a history of each day.
- Habits open: tap a habit for its streak, best streak, total, how often you keep it over 7, 30 and 90 days, the days of the week you do it, and a calendar month you can edit by tapping days.
- Tasks remember the day they were done (`done:` in Tasks.md), so history can count them.

## 0.2.1

- A new icon: a page with a check mark, text lines and a small chart, for notes, tasks, the journal and the numbers in one mark (and a themed version for Android 13 and later).

## 0.2.0

- A side menu with the sections, your task lists, note tags, journal history, search and settings. On a wide screen it stays open on the left.
- Tasks: an add button at the bottom right, like Notes. Tasks have a due date, a priority and a list; tap a task to edit it. Lists open from the side menu.
- Search across notes, journal entries and tasks from one box.
- Journal history: every day you wrote, newest first, and edit any of them.
- Settings: where the vault is, an export of the whole vault as a zip, and (on Android) keeping the vault in a visible /Lantern folder.
- Wide screens keep the content at a readable width.

## 0.1.1

- A new icon: a glowing lantern on a night background, with a themed (monochrome) version for Android 13 and later.
- The Linux window and package use the same icon.

## 0.1.0

First release.

- A vault of plain files: Markdown notes and journal entries, a Markdown task list, JSON and CSV for habits and money.
- Today, Notes, Tasks, Habits and Money screens.
- Android (arm64-v8a, armeabi-v7a, x86_64) and Linux (`.deb`) builds.
