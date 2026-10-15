package dev.thabit.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dev.thabit.core.Reminder
import dev.thabit.core.Reminders
import dev.thabit.core.Vault
import dev.thabit.tasks.Recurrence
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import okio.FileSystem
import okio.Path.Companion.toPath

private const val CHANNEL = "reminders"
private const val PREFS = "thabit"
private const val SNOOZE_MINUTES = 10

/**
 * Sets one alarm for the soonest reminder (a habit's time on its days, a task's time on its due day) and shows it as a notification
 * that stays until you tick it off. The vault location is saved by the activity so the receivers can open the same files.
 */
object ReminderAlarms {
    private fun vault(context: Context): Vault? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("vault", null)?.let { Vault(FileSystem.SYSTEM, it.toPath()) }

    private fun pending(context: Context, requestCode: Int, intent: Intent) =
        PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /** True when something has a reminder time, so the activity knows to ask for notification permission. */
    fun anyReminder(context: Context): Boolean = vault(context)?.let { Reminders.any(it.habits(), it.tasks()) } ?: false

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun loadSnooze(p: android.content.SharedPreferences, zone: TimeZone): Reminder? {
        val kind = p.getString("snoozeKind", null) ?: return null
        val at = p.getLong("snoozeAt", 0).takeIf { it > 0 } ?: return null
        return Reminder(Reminder.Kind.valueOf(kind), p.getString("snoozeRef", "") ?: "", p.getString("snoozeTitle", "") ?: "", Instant.fromEpochMilliseconds(at).toLocalDateTime(zone))
    }

    /**
     * One scheduling pass: shows what came due since the previous pass (a Doze delay, a switched-off phone, two at the same minute),
     * keeps a snooze across restarts, then sets the one alarm for the soonest reminder left. [snoozed] starts a new ten minute snooze.
     */
    fun schedule(context: Context, snoozed: Reminder? = null) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        manager.cancel(pending(context, 0, intent))
        val zone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val nowLocal = now.toLocalDateTime(zone)
        val v = vault(context) ?: return
        val p = prefs(context)
        snoozed?.let { s ->
            val at = now.plus(kotlin.time.Duration.parse("${SNOOZE_MINUTES}m"))
            p.edit().putString("snoozeKind", s.kind.name).putString("snoozeRef", s.refId).putString("snoozeTitle", s.title).putLong("snoozeAt", at.toEpochMilliseconds()).apply()
        }
        val lastPass = p.getLong("lastPass", 0).takeIf { it > 0 }?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(zone) }
        val plan = Reminders.plan(v.habits(), v.tasks(), v.habitLog(), v.habitSkips(), nowLocal, lastPass, loadSnooze(p, zone))
        plan.showNow.forEach { show(context, it.kind.name, it.refId, it.title) }
        val edit = p.edit().putLong("lastPass", now.toEpochMilliseconds())
        if (plan.snooze == null) edit.remove("snoozeKind").remove("snoozeAt")
        edit.apply()
        val next = plan.next ?: return
        intent.putExtra("kind", next.kind.name).putExtra("ref", next.refId).putExtra("title", next.title)
        val millis = next.at.toInstant(zone).toEpochMilliseconds()
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        val pi = pending(context, 0, intent)
        if (exact) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi) else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
    }

    private fun idOf(kind: String, ref: String) = (kind + ref).hashCode()

    /** A notification that stays (ongoing) until you tick the habit or task off, or snooze it. */
    fun show(context: Context, kind: String, ref: String, title: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_HIGH))
        val id = idOf(kind, ref)
        fun action(code: Int, what: String) = pending(context, id + code, Intent(context, ReminderActionReceiver::class.java).putExtra("what", what).putExtra("kind", kind).putExtra("ref", ref).putExtra("title", title))
        val open = PendingIntent.getActivity(context, 1, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title).setContentText(if (kind == Reminder.Kind.Habit.name) "Time for this habit" else "This task is due")
            .setOngoing(true).setAutoCancel(false).setContentIntent(open).setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, "Done", action(1, "done")).addAction(0, "Later", action(2, "later")).build()
        manager.notify(id, n)
    }

    fun cancel(context: Context, kind: String, ref: String) =
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(idOf(kind, ref))

    /** Ticks the habit (today) or finishes the task, in the vault files. */
    fun markDone(context: Context, kind: String, ref: String) {
        val v = vault(context) ?: return
        val today = v.today()
        if (kind == Reminder.Kind.Habit.name) v.setHabitDone(ref, today, true)
        else v.saveTasks(Recurrence.complete(v.tasks(), ref, true, today))
    }
}

/** Fires at a reminder: shows what came due and sets the alarm for the next one. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // The scheduling pass shows everything that came due, this reminder included, then sets the next alarm.
        ReminderAlarms.schedule(context)
    }
}

/** The Done and Later buttons on a reminder. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra("kind") ?: return; val ref = intent.getStringExtra("ref") ?: return
        ReminderAlarms.cancel(context, kind, ref)
        if (intent.getStringExtra("what") == "done") { ReminderAlarms.markDone(context, kind, ref); ReminderAlarms.schedule(context) }
        else ReminderAlarms.schedule(context, Reminder(Reminder.Kind.valueOf(kind), ref, intent.getStringExtra("title") ?: "", kotlinx.datetime.LocalDateTime(2000, 1, 1, 0, 0)))
    }
}

/** After a restart or a change of time or zone the alarm is gone: set it again. */
class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { ReminderAlarms.schedule(context) }
}
