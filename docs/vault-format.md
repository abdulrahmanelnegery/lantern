# Vault format (for importers)

A Thabit vault is one folder of plain files (`Zimam/Thabit` on the phone). Since 0.9.0 Thabit only reads and writes the habits, tasks and settings files. The notes and journal folders below were written by Thabit 0.8 and earlier (and by earlier versions); Thabit no longer touches them, so an importer such as Athar can rely on this layout. Thabit never deletes or rewrites those files, and its vault export includes them.

```
notes/<slug>-<id>.md             one note per file: Markdown with a small header
journal/<year>/<yyyy-mm-dd>.md   one journal entry per day
tasks/Tasks.md                   a Markdown checklist
habits/habits.json               what is tracked
habits/log.csv                   one row per done or skipped day
.lantern/settings.json           app settings; `.lantern/` is the settings and trash folder, name kept for compatibility
.lantern/trash/                  notes moved to the trash (left out of exports)
exports/                         zips made by Export (left out of the next export)
```

## The header

Notes and journal entries start with an optional header: a first line `---`, flat `key: value` lines, then a line `---`. Line endings may be CRLF. A header line that starts with a space is not a key (a multi-line value is ignored, not an error). A value in double quotes uses `\"`, `\\` and `\n` escapes; single quotes are stripped. A file without a header is plain body text.

## Notes (`notes/<slug>-<id>.md`)

| Key | Meaning |
|---|---|
| `id` | 8 characters from `abcdefghjkmnpqrstuvwxyz23456789`; also the end of the file name. If missing, the part of the file name after the last `-` |
| `title` | the note title. If missing, the first non-blank line of the body without leading `#`, else the file name |
| `created`, `updated` | local time, `yyyy-MM-ddTHH:mm:ss` (seconds precision) |
| `tags` | a list written `[a, "b c", d]`; an item containing `,` `[` or `]` is in double quotes |
| `pinned` | `true` only when pinned, absent otherwise |

Other keys (for example from Obsidian) were ignored by Thabit and may be present. The file name slug is lower case letters and digits joined by `-` (non-Latin letters are kept).

## Journal (`journal/<year>/<yyyy-mm-dd>.md`)

Keys: `date` (yyyy-MM-dd), `mood` (1 to 5, optional), `tags` (list, optional). The date also comes from the file name. A file existed only when it had text, a mood or tags.

## Habits and tasks

`habits/habits.json` is a JSON array of `{ "id", "name", "days" (1 = Monday to 7 = Sunday; empty = every day), "archived", "remindAt" ("HH:MM" or "") }`. `habits/log.csv` has the header `habit,date,mark`, with `mark` either `done` or `skip` (an older file may have only two columns, meaning done).

`tasks/Tasks.md` is `# Tasks` followed by `- [ ] title` or `- [x] title` lines with trailing markers: `due:yyyy-MM-dd`, `p:<priority>`, `list:<name with _ for spaces>`, `done:yyyy-MM-dd`, `repeat:<rule>`, `tags:a,b`, `at:HH:MM`, `id:<id>`. An indented `- [ ]` line under a task is a step of that task.
