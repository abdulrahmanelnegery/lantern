# Changelog

## 0.8.0

- **Reminders for habits and tasks** (Android). Give a habit or a task a time; a notification appears then and stays until you press Done (ticks the habit for today or finishes the task, and a repeating task leaves its next copy) or Later (ten minutes). One exact alarm is set for the soonest reminder (a few minutes late if exact alarms are not allowed), set again after a restart or a change of time, and when the app opens or closes. Notification permission is asked for only when a reminder exists.
- **Habit charts in Insights.** A twelve-week heatmap of how much of each day's habits you kept, and the average for each week as bars.

## 0.7.0

- **Dawem is now Thabit** (ثابت, "steady"), by Zimam Labs. Same app, same data: the Android app id is unchanged, so it updates in place, and the signing key is the same. The vault now lives in `Zimam/Thabit` (and `~/Zimam/Thabit` on Linux); `Zimam/Dawem`, `Dawem` and `Lantern` folders are copied in and checked, never deleted or overwritten, and stay as a backup. Settings can remove an old folder after a further check. The `.lantern` folder inside a vault keeps its name.
- **A new Home page.** A hero with today's progress ring, a quick-add row, your week, today's habits, upcoming and overdue tasks, a 14-day mood line, recent and pinned notes, on this day, a weekly summary, the affirmation, streaks and what's new. Order and visibility are set in Settings (Home page).
- **The final icon** (the infinity mark with the diamond) on the launcher, the splash, About and the README.
- **A new look.** Warm dark and ivory surfaces, the orange accent, a glow behind the Home hero, large rounded cards with a hairline border and a small diamond accent on every screen. The other palettes still work.
- **A new About page**, a better opening screen with the name clearly apart from the mark, a first-run tour you can reopen, and a one-time "what's new" after each update.
- **Exports stay in the Zimam folder:** the vault zip is saved in `Zimam/Thabit/exports` and is left out of the next export.
- **Tasks:** repeat daily, weekly or monthly (finishing one leaves the next open), steps, tags and a reminder time.

## 0.6.1

- Internal: package rename (`dev.lantern` to `dev.dawem`) and a split into modules (`:core`, `:ui`, one module each for notes, journal, tasks and habits, and the app). No change in behaviour; your vault, settings and the app's identity on the phone are unchanged.

## 0.6.0

- **Habits page.** Today view: swipe a habit right to mark it done, left to undo it, or hold it for skip, edit, analytics and archive. Skipped days do not break a streak. Week and Month views show every habit as a grid you can tap (hold a day to skip it), page back through time, and switch between all habits together or one habit at a time.
- **Today page.** An affirmation and a tip (tap to change), a streaks card (best streak, habits done today, journal streak), habits, tasks and the journal. Settings has a "Today page" section to show, hide and reorder each card.
- **Notes** open in a reading view with Markdown shown (headings, lists, checklists, quotes, code, bold and italic); the pencil switches to editing.
- Back goes to Today from any other section, then closes.

## 0.5.2

- **Money tracking is gone from Dawem.** It is becoming its own app, Hasad, so Dawem stays about notes, journal, tasks and habits. The Money section, the money card on Today and the "Spent" totals in Insights are removed.
- Files you already have in a vault's `money/` folder are left exactly where they are (Dawem simply no longer reads or writes them); delete the folder yourself when you no longer need it.
- Under the hood: skipped days for habits, streak and stats functions that count them, and a bank of quotes and tips, ready for the habits page coming next. The release is signed with every APK signature scheme.

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
