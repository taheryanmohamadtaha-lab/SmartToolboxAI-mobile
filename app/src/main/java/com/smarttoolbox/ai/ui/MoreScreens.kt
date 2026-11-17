package com.smarttoolbox.ai.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.smarttoolbox.ai.MainViewModel
import com.smarttoolbox.ai.core.ToolActions
import com.smarttoolbox.ai.data.CalcHistory
import com.smarttoolbox.ai.data.ClipStore
import com.smarttoolbox.ai.data.Colors
import com.smarttoolbox.ai.data.Lang
import com.smarttoolbox.ai.data.Prefs
import com.smarttoolbox.ai.data.ShortcutItem
import com.smarttoolbox.ai.data.ShortcutStore
import com.smarttoolbox.ai.floating.FloatingService

private data class AppEntry(val label: String, val pkg: String, val icon: Drawable)

private fun loadApps(ctx: Context): List<AppEntry> {
    val pm = ctx.packageManager
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(i, 0)
        .map { AppEntry(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm)) }
        .filter { it.pkg != ctx.packageName }
        .distinctBy { it.pkg }
        .sortedBy { it.label.lowercase() }
}

@Composable
fun ShortcutsScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { ShortcutStore(ctx) }
    var list by remember { mutableStateOf(store.all()) }
    var adding by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf("app") }
    var title by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("") }
    var color by remember { mutableIntStateOf(Colors.palette[0]) }
    var picking by remember { mutableStateOf(false) }
    val apps = remember(picking) { if (picking) loadApps(ctx) else emptyList() }
    fun reload() { list = store.all(); FloatingService.refresh(ctx) }

    ScreenFrame(
        t("Custom Shortcuts", "میانبرهای سفارشی"), back, scroll = false,
        actions = { IconButton(onClick = { title = ""; target = ""; icon = ""; type = "app"; adding = true }) { Icon(Icons.Filled.Add, null, tint = Color.White) } }
    ) {
        Text(t("Shortcuts marked \"in menu\" appear in the floating radial menu (the menu holds up to 12 items).", "میانبرهای دارای «در منو» در منوی شناور دایره‌ای نشان داده می‌شوند (حداکثر ۱۲ مورد)."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (list.isEmpty()) Text(t("No shortcuts yet. Tap + to add an app or a link.", "هنوز میانبری نیست. با + یک برنامه یا لینک اضافه کنید."))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.id }) { s ->
                SectionCard {
                    Row(Modifier.clickable { ToolActions.runShortcut(ctx, s) }, verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(CircleShape).background(Color(s.color)), contentAlignment = Alignment.Center) {
                            val d = if (s.type == "app") try { ctx.packageManager.getApplicationIcon(s.target) } catch (e: Exception) { null } else null
                            if (d != null) Image(d.toBitmap(96, 96).asImageBitmap(), null, Modifier.size(34.dp))
                            else Text(s.icon.ifBlank { s.title.take(1) }, color = Color.White, fontSize = 20.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                            Text(s.title, fontWeight = FontWeight.Bold)
                            Text(s.target, fontSize = 12.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { store.delete(s.id); reload() }) { Icon(Icons.Filled.Delete, null) }
                    }
                    SwitchRow(t("Show in floating menu", "نمایش در منوی شناور"), s.inMenu) { store.upsert(s.copy(inMenu = it)); reload() }
                }
            }
        }
    }

    if (adding) AlertDialog(
        onDismissRequest = { adding = false },
        title = { Text(t("New shortcut", "میانبر جدید")) },
        text = {
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(t("App", "برنامه"), type == "app") { type = "app"; target = "" }
                    Chip(t("Link", "لینک"), type == "url") { type = "url"; target = "" }
                }
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(t("Title", "عنوان")) })
                if (type == "app") TextButton(onClick = { picking = true }) { Text(if (target.isBlank()) t("Choose app…", "انتخاب برنامه…") else target) }
                else OutlinedTextField(target, { target = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("URL") })
                OutlinedTextField(icon, { icon = it.take(2) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(t("Icon (emoji or letter)", "آیکون (ایموجی یا حرف)")) })
                ColorRow(color) { color = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank() && target.isNotBlank()) {
                    store.upsert(ShortcutItem(System.currentTimeMillis(), title.trim(), type, target.trim(), icon, color, true))
                    reload()
                }
                adding = false
            }) { Text(t("Add", "افزودن")) }
        },
        dismissButton = { TextButton(onClick = { adding = false }) { Text(t("Cancel", "انصراف")) } }
    )

    if (picking) AlertDialog(
        onDismissRequest = { picking = false },
        title = { Text(t("Choose app", "انتخاب برنامه")) },
        text = {
            LazyColumn(Modifier.height(360.dp)) {
                items(apps, key = { it.pkg }) { a ->
                    Row(Modifier.fillMaxWidth().clickable { target = a.pkg; if (title.isBlank()) title = a.label; picking = false }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(a.icon.toBitmap(96, 96).asImageBitmap(), null, Modifier.size(36.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(a.label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { picking = false }) { Text(t("Close", "بستن")) } }
    )
}

@Composable
fun HistoryScreen(back: () -> Unit, onOpen: (Uri) -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs.get(ctx) }
    var tab by remember { mutableIntStateOf(0) }
    var calc by remember { mutableStateOf(CalcHistory.list(prefs)) }
    ScreenFrame(t("History", "تاریخچه"), back, scroll = false) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(t("Screenshots", "اسکرین‌شات‌ها"), tab == 0) { tab = 0 }
            Chip(t("Clipboard", "کلیپ‌بورد"), tab == 1) { tab = 1 }
            Chip(t("Calculator", "ماشین‌حساب"), tab == 2) { tab = 2 }
        }
        when (tab) {
            0 -> ShotGallery(onOpen)
            1 -> {
                val clips = remember { ClipStore(ctx).all() }
                if (clips.isEmpty()) Text(t("Clipboard history is empty.", "تاریخچه کلیپ‌بورد خالی است."))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(clips, key = { it.id }) { SectionCard { Text(it.text, maxLines = 3) } } }
            }
            else -> {
                if (calc.isEmpty()) Text(t("No calculations yet.", "هنوز محاسبه‌ای نیست."))
                else TextButton(onClick = { CalcHistory.clear(prefs); calc = emptyList() }) { Text(t("Clear", "پاک کردن")) }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) { items(calc) { Text(it, fontSize = 16.sp) } }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel, back: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs.get(ctx) }
    var haptic by remember { mutableStateOf(prefs.haptic) }
    var auto by remember { mutableStateOf(prefs.autoStart) }
    var fmt by remember { mutableStateOf(prefs.shotFormat) }
    var sound by remember { mutableStateOf(prefs.shotSound) }
    var notif by remember { mutableStateOf(prefs.shotNotif) }
    var folder by remember { mutableStateOf(prefs.shotFolder) }
    var confirmReset by remember { mutableStateOf(false) }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u ->
        if (u != null) {
            ctx.contentResolver.openOutputStream(u)?.use { it.write(prefs.exportJson().toByteArray()) }
            Toast.makeText(ctx, Lang.t(ctx, "Backup saved", "پشتیبان ذخیره شد"), Toast.LENGTH_SHORT).show()
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) {
            val s = ctx.contentResolver.openInputStream(u)?.bufferedReader()?.use { it.readText() }
            val ok = s != null && prefs.importJson(s)
            vm.reload()
            haptic = prefs.haptic; auto = prefs.autoStart; fmt = prefs.shotFormat; sound = prefs.shotSound; notif = prefs.shotNotif; folder = prefs.shotFolder
            FloatingService.refresh(ctx)
            Toast.makeText(ctx, if (ok) Lang.t(ctx, "Backup restored", "پشتیبان بازگردانی شد") else Lang.t(ctx, "Invalid backup file", "فایل پشتیبان نامعتبر است"), Toast.LENGTH_SHORT).show()
        }
    }

    ScreenFrame(t("Settings", "تنظیمات"), back) {
        SectionCard(t("Appearance", "ظاهر")) {
            Text(t("Theme", "تم"))
            ChipRow {
                listOf(0 to t("System", "سیستم"), 1 to t("Light", "روشن"), 2 to t("Dark", "تیره"), 3 to "AMOLED").forEach { (m, n) -> Chip(n, vm.themeMode == m) { vm.setTheme(m) } }
            }
            Text(t("Accent color", "رنگ تاکیدی"))
            ColorRow(vm.accent) { vm.setAccent(it) }
            Text(t("Language", "زبان"))
            ChipRow {
                Chip(t("System", "سیستم"), vm.lang == "system") { vm.setLang("system") }
                Chip("English", vm.lang == "en") { vm.setLang("en") }
                Chip("فارسی", vm.lang == "fa") { vm.setLang("fa") }
            }
        }
        SectionCard(t("General", "عمومی")) {
            SwitchRow(t("Haptic feedback", "بازخورد لمسی"), haptic) { haptic = it; prefs.haptic = it }
            SwitchRow(t("Start floating toolbox after reboot", "شروع جعبه‌ابزار پس از ری‌استارت"), auto, t("Where Android allows it", "در صورت اجازه اندروید")) {
                if (it && !Settings.canDrawOverlays(ctx)) {
                    Toast.makeText(ctx, Lang.t(ctx, "Grant the overlay permission first", "ابتدا مجوز نمایش روی برنامه‌ها را بدهید"), Toast.LENGTH_SHORT).show()
                    ctx.safeStart(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
                } else { auto = it; prefs.autoStart = it }
            }
        }
        SectionCard(t("Screenshots", "اسکرین‌شات")) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("PNG", fmt == "png") { fmt = "png"; prefs.shotFormat = "png" }
                Chip("JPEG", fmt == "jpg") { fmt = "jpg"; prefs.shotFormat = "jpg" }
            }
            OutlinedTextField(folder, { folder = it; prefs.shotFolder = it.filter { c -> c.isLetterOrDigit() || c == '_' || c == '-' || c == ' ' } }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(t("Folder inside Pictures", "پوشه داخل Pictures")) })
            SwitchRow(t("Shutter sound", "صدای شاتر"), sound) { sound = it; prefs.shotSound = it }
            SwitchRow(t("Notification after capture", "اعلان پس از گرفتن تصویر"), notif) { notif = it; prefs.shotNotif = it }
        }
        SectionCard(t("Backup and reset", "پشتیبان و بازنشانی")) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GradButton(t("Backup", "پشتیبان")) { export.launch("smart_toolbox_backup.json") }
                GradButton(t("Restore", "بازگردانی")) { importer.launch(arrayOf("application/json", "*/*")) }
            }
            GradButton(t("Reset configuration", "بازنشانی تنظیمات"), Modifier.fillMaxWidth()) { confirmReset = true }
        }
        SectionCard(t("About", "درباره")) {
            Text("Smart Toolbox AI 1.0.0", fontWeight = FontWeight.Bold)
            Text(t("All data (notes, clipboard, screenshots, settings) stays on this device. Nothing is uploaded. Permissions are requested only when a feature needs them: overlay (floating button), notifications (service notice), camera (QR scan), screen capture consent (screenshots), accessibility (optional: lock screen / recents / back).",
                "همه داده‌ها (یادداشت، کلیپ‌بورد، اسکرین‌شات، تنظیمات) روی همین دستگاه می‌ماند و چیزی آپلود نمی‌شود. مجوزها فقط هنگام نیاز خواسته می‌شوند: نمایش روی برنامه‌ها (دکمه شناور)، اعلان، دوربین (اسکن QR)، اجازه ضبط صفحه (اسکرین‌شات) و دسترس‌پذیری (اختیاری: قفل/اخیر/بازگشت)."), fontSize = 13.sp)
        }
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text(t("Reset all settings and data?", "همه تنظیمات و داده‌ها بازنشانی شود؟")) },
        text = { Text(t("Notes, clipboard history, shortcuts and settings will be erased. Screenshots in your gallery are kept.", "یادداشت‌ها، تاریخچه کلیپ‌بورد، میانبرها و تنظیمات پاک می‌شوند. اسکرین‌شات‌های گالری حفظ می‌شوند.")) },
        confirmButton = {
            TextButton(onClick = {
                prefs.reset(); vm.reload()
                haptic = prefs.haptic; auto = prefs.autoStart; fmt = prefs.shotFormat; sound = prefs.shotSound; notif = prefs.shotNotif; folder = prefs.shotFolder
                FloatingService.refresh(ctx)
                confirmReset = false
            }) { Text(t("Reset", "بازنشانی")) }
        },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(t("Cancel", "انصراف")) } }
    )
}
