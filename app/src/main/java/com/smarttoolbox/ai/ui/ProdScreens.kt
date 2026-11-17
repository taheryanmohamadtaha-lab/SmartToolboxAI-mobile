package com.smarttoolbox.ai.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttoolbox.ai.data.ClipItem
import com.smarttoolbox.ai.data.ClipStore
import com.smarttoolbox.ai.data.Lang
import com.smarttoolbox.ai.data.Note
import com.smarttoolbox.ai.data.NoteStore
import com.smarttoolbox.ai.data.Reminders
import com.smarttoolbox.ai.floating.FloatingService
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun copyText(ctx: Context, text: String) {
    ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("text", text))
    Toast.makeText(ctx, Lang.t(ctx, "Copied", "کپی شد"), Toast.LENGTH_SHORT).show()
}

@Composable
fun ProductivityScreen(back: () -> Unit, go: (String) -> Unit) {
    val ctx = LocalContext.current
    ScreenFrame(t("Productivity", "بهره‌وری"), back) {
        SectionCard(t("Floating tools", "ابزارهای شناور")) {
            ActionRow(Icons.Filled.Calculate, t("Floating calculator", "ماشین‌حساب شناور"), t("Opens above other apps", "روی برنامه‌های دیگر باز می‌شود")) { openFloating(ctx, FloatingService.ACTION_CALC) }
            ActionRow(Icons.Filled.Notes, t("Floating notepad", "یادداشت شناور"), t("Auto-saved, transparent window", "ذخیره خودکار، پنجره شفاف")) { openFloating(ctx, FloatingService.ACTION_NOTES) }
        }
        SectionCard(t("Tools", "ابزارها")) {
            ActionRow(Icons.Filled.Notes, t("Notes", "یادداشت‌ها"), t("Search, pin, share", "جستجو، سنجاق، اشتراک")) { go("notes") }
            ActionRow(Icons.Filled.ContentPaste, t("Clipboard manager", "مدیریت کلیپ‌بورد")) { go("clip") }
            ActionRow(Icons.Filled.Translate, t("Translator", "مترجم")) { go("translator") }
            ActionRow(Icons.Filled.Timer, t("Stopwatch", "کرنومتر")) { go("stopwatch") }
            ActionRow(Icons.Filled.Timer, t("Countdown timer", "تایمر معکوس")) { go("timer") }
            ActionRow(Icons.Filled.AccessTime, t("Digital clock", "ساعت دیجیتال")) { go("clock") }
            ActionRow(Icons.Filled.Event, t("Calendar", "تقویم")) {
                ctx.safeStart(Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.calendar/time/" + System.currentTimeMillis())))
            }
            ActionRow(Icons.Filled.Alarm, t("Quick reminders", "یادآورهای سریع")) { go("reminders") }
            ActionRow(Icons.Filled.SwapHoriz, t("Unit converter", "مبدل واحد")) { go("converter") }
        }
        SectionCard(t("QR codes", "کد QR")) {
            ActionRow(Icons.Filled.QrCodeScanner, t("Scan QR / barcode", "اسکن QR / بارکد")) { go("qrscan") }
            ActionRow(Icons.Filled.QrCode, t("Generate QR code", "ساخت کد QR")) { go("qrgen") }
        }
    }
}

@Composable
fun NotesScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { NoteStore(ctx) }
    var notes by remember { mutableStateOf(store.all()) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Note?>(null) }
    var text by remember { mutableStateOf("") }
    fun reload() { notes = store.all() }

    ScreenFrame(
        t("Notes", "یادداشت‌ها"), back, scroll = false,
        actions = {
            IconButton(onClick = { val now = System.currentTimeMillis(); editing = Note(now, "", false, now); text = "" }) { Icon(Icons.Filled.Add, null, tint = androidx.compose.ui.graphics.Color.White) }
        }
    ) {
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(t("Search notes", "جستجوی یادداشت")) })
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(notes.filter { query.isBlank() || it.text.contains(query, true) }, key = { it.id }) { n ->
                SectionCard {
                    Text(n.text, maxLines = 4, modifier = Modifier.clickable { editing = n; text = n.text })
                    Row {
                        IconButton(onClick = { store.upsert(n.copy(pinned = !n.pinned)); reload() }) {
                            Icon(if (n.pinned) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { copyText(ctx, n.text) }) { Icon(Icons.Filled.ContentCopy, null) }
                        IconButton(onClick = { ctx.safeStart(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, n.text), null)) }) { Icon(Icons.Filled.Share, null) }
                        IconButton(onClick = { store.delete(n.id); reload() }) { Icon(Icons.Filled.Delete, null) }
                    }
                }
            }
        }
    }
    editing?.let { n ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(t("Note", "یادداشت")) },
            text = { OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth().height(220.dp)) },
            confirmButton = {
                TextButton(onClick = {
                    if (text.isNotBlank()) store.upsert(n.copy(text = text, updated = System.currentTimeMillis())) else store.delete(n.id)
                    editing = null; reload()
                }) { Text(t("Save", "ذخیره")) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text(t("Cancel", "انصراف")) } }
        )
    }
}

@Composable
fun ClipboardScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { ClipStore(ctx) }
    var items by remember { mutableStateOf(store.all()) }
    var query by remember { mutableStateOf("") }
    var manual by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    fun reload() { items = store.all() }

    ScreenFrame(t("Clipboard manager", "مدیریت کلیپ‌بورد"), back, scroll = false) {
        Text(
            t("Nothing is monitored in the background. Tap the button to save what you copied, or type an item. History stays on this device.",
                "هیچ چیزی در پس‌زمینه پایش نمی‌شود. با دکمه، متن کپی‌شده را ذخیره کنید یا خودتان بنویسید. تاریخچه فقط روی همین دستگاه می‌ماند."),
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GradButton(t("Save current clipboard", "ذخیره کلیپ‌بورد فعلی")) {
                val c = ctx.getSystemService(ClipboardManager::class.java).primaryClip
                val s = c?.getItemAt(0)?.coerceToText(ctx)?.toString()
                if (s.isNullOrBlank()) Toast.makeText(ctx, Lang.t(ctx, "Clipboard is empty", "کلیپ‌بورد خالی است"), Toast.LENGTH_SHORT).show()
                else { store.add(s); reload() }
            }
            GradButton(t("Clear all", "پاک کردن همه")) { confirmClear = true }
        }
        OutlinedTextField(
            manual, { manual = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(t("Add an item", "افزودن مورد")) },
            trailingIcon = { IconButton(onClick = { store.add(manual); manual = ""; reload() }) { Icon(Icons.Filled.Add, null) } }
        )
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(t("Search history", "جستجو در تاریخچه")) })
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items.filter { query.isBlank() || it.text.contains(query, true) }, key = { it.id }) { c: ClipItem ->
                SectionCard {
                    Text(c.text, maxLines = 4)
                    Row {
                        IconButton(onClick = { store.toggleFav(c.id); reload() }) {
                            Icon(if (c.fav) Icons.Filled.Star else Icons.Filled.StarBorder, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { copyText(ctx, c.text) }) { Icon(Icons.Filled.ContentCopy, null) }
                        IconButton(onClick = { store.delete(c.id); reload() }) { Icon(Icons.Filled.Delete, null) }
                    }
                }
            }
        }
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text(t("Clear all clipboard history?", "همه تاریخچه کلیپ‌بورد پاک شود؟")) },
        confirmButton = { TextButton(onClick = { store.clear(); reload(); confirmClear = false }) { Text(t("Clear", "پاک کردن")) } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(t("Cancel", "انصراف")) } }
    )
}

private fun fmtTime(ms: Long): String {
    val cs = (ms / 10) % 100; val s = (ms / 1000) % 60; val m = (ms / 60000) % 60; val h = ms / 3600000
    return if (h > 0) "%d:%02d:%02d.%02d".format(h, m, s, cs) else "%02d:%02d.%02d".format(m, s, cs)
}

@Composable
fun StopwatchScreen(back: () -> Unit) {
    var running by remember { mutableStateOf(false) }
    var acc by remember { mutableLongStateOf(0L) }
    var startAt by remember { mutableLongStateOf(0L) }
    var shown by remember { mutableLongStateOf(0L) }
    val laps = remember { mutableStateListOf<Long>() }
    LaunchedEffect(running) {
        while (running) { shown = acc + (System.currentTimeMillis() - startAt); delay(30) }
    }
    ScreenFrame(t("Stopwatch", "کرنومتر"), back) {
        SectionCard {
            Text(fmtTime(shown), fontSize = 44.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GradButton(if (running) t("Pause", "توقف") else t("Start", "شروع")) {
                    if (running) { acc += System.currentTimeMillis() - startAt; shown = acc; running = false }
                    else { startAt = System.currentTimeMillis(); running = true }
                }
                GradButton(t("Lap", "دور")) { if (running) laps.add(0, shown) }
                GradButton(t("Reset", "ریست")) { running = false; acc = 0; shown = 0; laps.clear() }
            }
            laps.forEachIndexed { i, l -> Text("#${laps.size - i}   ${fmtTime(l)}") }
        }
    }
}

@Composable
fun TimerScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    var minutes by remember { mutableStateOf("5") }
    var endAt by remember { mutableLongStateOf(0L) }
    var rid by remember { mutableStateOf(0) }
    var left by remember { mutableLongStateOf(0L) }
    LaunchedEffect(endAt) {
        while (endAt > 0) {
            left = endAt - System.currentTimeMillis()
            if (left <= 0) { left = 0; endAt = 0; break }
            delay(250)
        }
    }
    ScreenFrame(t("Countdown timer", "تایمر معکوس"), back) {
        SectionCard {
            Text(if (endAt > 0) "%02d:%02d".format(left / 60000, (left / 1000) % 60) else t("Ready", "آماده"), fontSize = 44.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(minutes, { minutes = it.filter { c -> c.isDigit() }.take(4) }, singleLine = true, label = { Text(t("Minutes", "دقیقه")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GradButton(t("Start", "شروع")) {
                    val m = minutes.toLongOrNull() ?: 0L
                    if (m > 0) {
                        val end = System.currentTimeMillis() + m * 60000
                        val r = Reminders.add(ctx, Lang.t(ctx, "Timer finished", "تایمر تمام شد"), end)
                        rid = r.id; endAt = end
                    }
                }
                GradButton(t("Cancel", "لغو")) { if (endAt > 0) Reminders.remove(ctx, rid); endAt = 0; left = 0 }
            }
            Text(t("You will get a notification when the time is up, even if the app is closed.", "پایان زمان با اعلان اطلاع داده می‌شود، حتی اگر برنامه بسته باشد."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ClockScreen(back: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(500) } }
    ScreenFrame(t("Digital clock", "ساعت دیجیتال"), back) {
        SectionCard {
            Text(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(now)), fontSize = 52.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(DateFormat.getDateInstance(DateFormat.FULL).format(Date(now)))
        }
    }
}

@Composable
fun RemindersScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf("") }
    var mins by remember { mutableStateOf("15") }
    var list by remember { mutableStateOf(Reminders.all(ctx)) }
    ScreenFrame(t("Quick reminders", "یادآورهای سریع"), back) {
        SectionCard {
            OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), label = { Text(t("Reminder text", "متن یادآور")) })
            ChipRow { listOf("5", "15", "30", "60", "120").forEach { m -> Chip("$m ${t("min", "دقیقه")}", mins == m) { mins = m } } }
            GradButton(t("Set reminder", "تنظیم یادآور")) {
                val m = mins.toLongOrNull() ?: 15L
                if (text.isNotBlank()) {
                    Reminders.add(ctx, text, System.currentTimeMillis() + m * 60000)
                    text = ""; list = Reminders.all(ctx)
                }
            }
            Text(t("Notifications must be allowed for reminders to show.", "برای نمایش یادآور باید اعلان‌ها مجاز باشند."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        list.forEach { r ->
            SectionCard {
                Text(r.text, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(SimpleDateFormat("HH:mm  yyyy/MM/dd", Locale.getDefault()).format(Date(r.time)), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton(onClick = { Reminders.remove(ctx, r.id); list = Reminders.all(ctx) }) { Icon(Icons.Filled.Delete, null) }
                }
            }
        }
    }
}

private val unitTable: Map<String, List<Pair<String, Double>>> = mapOf(
    "length" to listOf("m" to 1.0, "km" to 1000.0, "cm" to 0.01, "mm" to 0.001, "mi" to 1609.344, "ft" to 0.3048, "in" to 0.0254, "yd" to 0.9144),
    "weight" to listOf("kg" to 1.0, "g" to 0.001, "lb" to 0.45359237, "oz" to 0.028349523, "t" to 1000.0),
    "volume" to listOf("L" to 1.0, "mL" to 0.001, "gal(US)" to 3.785411784, "cup" to 0.2365882365, "m³" to 1000.0),
    "speed" to listOf("m/s" to 1.0, "km/h" to 0.2777777778, "mph" to 0.44704, "kn" to 0.5144444444),
    "temp" to listOf("°C" to 0.0, "°F" to 0.0, "K" to 0.0)
)

private fun convert(cat: String, from: String, to: String, v: Double): Double {
    if (cat == "temp") {
        val c = when (from) { "°F" -> (v - 32) * 5 / 9; "K" -> v - 273.15; else -> v }
        return when (to) { "°F" -> c * 9 / 5 + 32; "K" -> c + 273.15; else -> c }
    }
    val list = unitTable[cat] ?: return v
    val f = list.first { it.first == from }.second
    val t = list.first { it.first == to }.second
    return v * f / t
}

@Composable
fun ConverterScreen(back: () -> Unit) {
    var cat by remember { mutableStateOf("length") }
    var fromU by remember { mutableStateOf("m") }
    var toU by remember { mutableStateOf("km") }
    var value by remember { mutableStateOf("1") }
    val names = mapOf("length" to t("Length", "طول"), "weight" to t("Weight", "وزن"), "volume" to t("Volume", "حجم"), "speed" to t("Speed", "سرعت"), "temp" to t("Temperature", "دما"))
    ScreenFrame(t("Unit converter", "مبدل واحد"), back) {
        SectionCard {
            ChipRow { unitTable.keys.forEach { k -> Chip(names[k] ?: k, cat == k) { cat = k; fromU = unitTable[k]!![0].first; toU = unitTable[k]!![1].first } } }
            OutlinedTextField(value, { value = it }, Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            Text(t("From", "از"))
            ChipRow { unitTable[cat]!!.forEach { (u, _) -> Chip(u, fromU == u) { fromU = u } } }
            Text(t("To", "به"))
            ChipRow { unitTable[cat]!!.forEach { (u, _) -> Chip(u, toU == u) { toU = u } } }
            val v = value.toDoubleOrNull()
            Text(
                if (v != null) com.smarttoolbox.ai.core.CalcEngine.format(convert(cat, fromU, toU, v)) + " " + toU else "—",
                fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun TranslatorScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("en") }
    val langs = listOf("en" to "English", "fa" to "فارسی", "ar" to "العربية", "tr" to "Türkçe", "fr" to "Français", "es" to "Español", "de" to "Deutsch")
    ScreenFrame(t("Translator", "مترجم"), back) {
        SectionCard {
            OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth().height(160.dp), label = { Text(t("Text", "متن")) })
            Text(t("Translate to", "ترجمه به"))
            ChipRow { langs.forEach { (c, n) -> Chip(n, target == c) { target = c } } }
            GradButton(t("Translate", "ترجمه")) {
                if (text.isNotBlank()) ctx.safeStart(Intent(Intent.ACTION_VIEW, Uri.parse("https://translate.google.com/?sl=auto&tl=$target&op=translate&text=" + Uri.encode(text))))
            }
            Text(t("Translation needs the internet and opens in your browser or the Google Translate app. Your text is sent only when you tap Translate.", "ترجمه به اینترنت نیاز دارد و در مرورگر یا برنامه گوگل ترنسلیت باز می‌شود. متن فقط با زدن «ترجمه» ارسال می‌شود."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
