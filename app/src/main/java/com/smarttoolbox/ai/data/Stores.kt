package com.smarttoolbox.ai.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.smarttoolbox.ai.floating.ReminderReceiver
import org.json.JSONArray
import org.json.JSONObject

data class Note(val id: Long, val text: String, val pinned: Boolean, val updated: Long)

class NoteStore(ctx: Context) {
    private val p = Prefs.get(ctx)
    fun all(): List<Note> {
        val a = p.jsonArray("notes")
        val out = ArrayList<Note>()
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            out.add(Note(o.getLong("id"), o.getString("text"), o.optBoolean("pinned"), o.optLong("updated")))
        }
        return out.sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updated })
    }
    fun upsert(n: Note) = write(all().filter { it.id != n.id } + n)
    fun delete(id: Long) = write(all().filter { it.id != id })
    private fun write(l: List<Note>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("id", it.id).put("text", it.text).put("pinned", it.pinned).put("updated", it.updated)) }
        p.putJson("notes", a)
    }
}

data class ClipItem(val id: Long, val text: String, val fav: Boolean, val time: Long)

class ClipStore(ctx: Context) {
    private val p = Prefs.get(ctx)
    fun all(): List<ClipItem> {
        val a = p.jsonArray("clips")
        val out = ArrayList<ClipItem>()
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            out.add(ClipItem(o.getLong("id"), o.getString("text"), o.optBoolean("fav"), o.optLong("time")))
        }
        return out.sortedWith(compareByDescending<ClipItem> { it.fav }.thenByDescending { it.time })
    }
    fun add(text: String) {
        if (text.isBlank()) return
        val old = all()
        val prev = old.firstOrNull { it.text == text }
        val now = System.currentTimeMillis()
        write((listOf(ClipItem(now, text, prev?.fav ?: false, now)) + old.filter { it.text != text }).take(200))
    }
    fun toggleFav(id: Long) = write(all().map { if (it.id == id) it.copy(fav = !it.fav) else it })
    fun delete(id: Long) = write(all().filter { it.id != id })
    fun clear() = write(emptyList())
    private fun write(l: List<ClipItem>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("id", it.id).put("text", it.text).put("fav", it.fav).put("time", it.time)) }
        p.putJson("clips", a)
    }
}

data class ShortcutItem(val id: Long, val title: String, val type: String, val target: String, val icon: String, val color: Int, val inMenu: Boolean)

class ShortcutStore(ctx: Context) {
    private val p = Prefs.get(ctx)
    fun all(): List<ShortcutItem> {
        val a = p.jsonArray("shortcuts")
        val out = ArrayList<ShortcutItem>()
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            out.add(ShortcutItem(o.getLong("id"), o.getString("title"), o.getString("type"), o.getString("target"), o.optString("icon"), o.optInt("color"), o.optBoolean("inMenu")))
        }
        return out
    }
    fun upsert(s: ShortcutItem) {
        val l = all()
        write(if (l.any { it.id == s.id }) l.map { if (it.id == s.id) s else it } else l + s)
    }
    fun delete(id: Long) = write(all().filter { it.id != id })
    private fun write(l: List<ShortcutItem>) {
        val a = JSONArray()
        l.forEach {
            a.put(JSONObject().put("id", it.id).put("title", it.title).put("type", it.type).put("target", it.target)
                .put("icon", it.icon).put("color", it.color).put("inMenu", it.inMenu))
        }
        p.putJson("shortcuts", a)
    }
}

object CalcHistory {
    fun list(p: Prefs): List<String> {
        val a = p.jsonArray("calc_history")
        return (0 until a.length()).map { a.getString(it) }
    }
    fun add(p: Prefs, line: String) {
        val l = (listOf(line) + list(p)).take(50)
        p.putJson("calc_history", JSONArray(l))
    }
    fun clear(p: Prefs) = p.putJson("calc_history", JSONArray())
}

data class Reminder(val id: Int, val text: String, val time: Long)

object Reminders {
    fun all(ctx: Context): List<Reminder> {
        val a = Prefs.get(ctx).jsonArray("reminders")
        val out = ArrayList<Reminder>()
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            out.add(Reminder(o.getInt("id"), o.getString("text"), o.getLong("time")))
        }
        return out.sortedBy { it.time }
    }
    private fun save(ctx: Context, l: List<Reminder>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("id", it.id).put("text", it.text).put("time", it.time)) }
        Prefs.get(ctx).putJson("reminders", a)
    }
    private fun pi(ctx: Context, r: Reminder): PendingIntent = PendingIntent.getBroadcast(
        ctx, r.id, Intent(ctx, ReminderReceiver::class.java).putExtra("id", r.id).putExtra("text", r.text),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    fun add(ctx: Context, text: String, time: Long): Reminder {
        val r = Reminder(((System.currentTimeMillis() / 1000) % 100000000).toInt(), text, time)
        save(ctx, all(ctx) + r)
        schedule(ctx, r)
        return r
    }
    fun schedule(ctx: Context, r: Reminder) {
        ctx.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.time, pi(ctx, r))
    }
    fun remove(ctx: Context, id: Int) {
        all(ctx).firstOrNull { it.id == id }?.let { ctx.getSystemService(AlarmManager::class.java).cancel(pi(ctx, it)) }
        drop(ctx, id)
    }
    fun drop(ctx: Context, id: Int) = save(ctx, all(ctx).filter { it.id != id })
    fun rescheduleAll(ctx: Context) {
        val now = System.currentTimeMillis()
        all(ctx).forEach { if (it.time > now) schedule(ctx, it) }
    }
}
