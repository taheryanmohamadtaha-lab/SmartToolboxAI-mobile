package com.smarttoolbox.ai.ui

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.provider.Settings
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.smarttoolbox.ai.core.ToolActions
import kotlinx.coroutines.delay

@Composable
fun MiniToolsScreen(back: () -> Unit, go: (String) -> Unit) {
    val ctx = LocalContext.current
    var dialog by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    ScreenFrame(t("Mini Tools", "ابزارهای کوچک"), back) {
        SectionCard(t("Screen utilities", "ابزارهای صفحه")) {
            ActionRow(Icons.Filled.BrightnessMedium, t("Brightness", "روشنایی"), t("Opens display settings", "باز کردن تنظیمات نمایش")) { ctx.safeStart(Intent(Settings.ACTION_DISPLAY_SETTINGS)) }
            ActionRow(Icons.Filled.ScreenRotation, t("Orientation", "چرخش صفحه"), t("Auto-rotate is in display settings", "چرخش خودکار در تنظیمات نمایش است")) { ctx.safeStart(Intent(Settings.ACTION_DISPLAY_SETTINGS)) }
            ActionRow(Icons.Filled.Timer, t("Screen timeout", "زمان خاموشی صفحه"), t("Opens display settings", "باز کردن تنظیمات نمایش")) { ctx.safeStart(Intent(Settings.ACTION_DISPLAY_SETTINGS)) }
            ActionRow(Icons.Filled.ZoomIn, t("Screen magnification", "بزرگ‌نمایی صفحه"), t("Accessibility settings", "تنظیمات دسترس‌پذیری")) { ctx.safeStart(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            ActionRow(Icons.Filled.Lock, t("Lock screen", "قفل صفحه"), t("Needs the accessibility helper", "نیاز به سرویس دسترس‌پذیری دارد")) { ToolActions.run(ctx, "lock") }
            ActionRow(Icons.Filled.Info, t("Display information", "اطلاعات نمایشگر")) { dialog = "display" }
        }
        SectionCard(t("Smart shortcuts", "میانبرهای هوشمند")) {
            ActionRow(Icons.Filled.CameraAlt, t("Open camera", "باز کردن دوربین")) { ctx.safeStart(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) }
            ActionRow(Icons.Filled.FlashlightOn, t("Flashlight", "چراغ‌قوه")) { ToolActions.run(ctx, "torch") }
            ActionRow(Icons.Filled.Settings, t("Open settings", "باز کردن تنظیمات")) { ToolActions.run(ctx, "settings") }
            ActionRow(Icons.Filled.History, t("Recent apps", "برنامه‌های اخیر"), t("Needs the accessibility helper", "نیاز به سرویس دسترس‌پذیری دارد")) { ToolActions.run(ctx, "recents") }
            ActionRow(Icons.Filled.Share, t("Share text", "اشتراک‌گذاری متن")) { text = ""; dialog = "share" }
            ActionRow(Icons.Filled.Search, t("Quick search", "جستجوی سریع")) { text = ""; dialog = "search" }
            ActionRow(Icons.Filled.Apps, t("Favorite apps", "برنامه‌های محبوب"), t("Add them as custom shortcuts", "آن‌ها را به‌عنوان میانبر اضافه کنید")) { go("shortcuts") }
        }
        SectionCard(t("Accessibility helper", "سرویس دسترس‌پذیری")) {
            Text(t("Only used for Lock screen, Recent apps and Back. It never reads your screen or collects data.", "فقط برای قفل صفحه، برنامه‌های اخیر و بازگشت استفاده می‌شود. هرگز صفحه را نمی‌خواند و داده‌ای جمع نمی‌کند."), fontSize = 13.sp)
            GradButton(t("Open accessibility settings", "باز کردن تنظیمات دسترس‌پذیری")) { ctx.safeStart(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
    }
    if (dialog == "share" || dialog == "search") AlertDialog(
        onDismissRequest = { dialog = "" },
        title = { Text(if (dialog == "share") t("Share text", "اشتراک‌گذاری متن") else t("Quick search", "جستجوی سریع")) },
        text = { OutlinedTextField(text, { text = it }) },
        confirmButton = {
            TextButton(onClick = {
                val d = dialog; dialog = ""
                if (text.isNotBlank()) {
                    if (d == "share") ctx.safeStart(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
                    else ctx.safeStart(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(text))))
                }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = { dialog = "" }) { Text(t("Cancel", "انصراف")) } }
    )
    if (dialog == "display") {
        val dm = ctx.resources.displayMetrics
        AlertDialog(
            onDismissRequest = { dialog = "" },
            title = { Text(t("Display information", "اطلاعات نمایشگر")) },
            text = {
                Text(
                    t("Resolution: ", "وضوح: ") + "${dm.widthPixels} × ${dm.heightPixels}\n" +
                        t("Density: ", "تراکم: ") + "${dm.densityDpi} dpi (x${dm.density})\n" +
                        t("Orientation: ", "جهت: ") + if (dm.widthPixels > dm.heightPixels) t("Landscape", "افقی") else t("Portrait", "عمودی")
                )
            },
            confirmButton = { TextButton(onClick = { dialog = "" }) { Text("OK") } }
        )
    }
}

@Composable
fun SystemInfoScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(2000); tick++ } }

    val rows = remember(tick) {
        val bat = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = bat?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = bat?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val temp = (bat?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val status = bat?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        val net = when {
            caps == null -> "Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Connected"
        }
        val st = StatFs(Environment.getDataDirectory().path)
        val mi = ActivityManager.MemoryInfo().also { ctx.getSystemService(ActivityManager::class.java).getMemoryInfo(it) }
        listOf(
            "bat" to (if (level >= 0) "${level * 100 / scale}%" else "—"),
            "temp" to "%.1f °C".format(temp),
            "chg" to if (charging) "Yes" else "No",
            "net" to net,
            "sto" to Formatter.formatFileSize(ctx, st.availableBytes) + " / " + Formatter.formatFileSize(ctx, st.totalBytes),
            "ram" to Formatter.formatFileSize(ctx, mi.availMem) + " / " + Formatter.formatFileSize(ctx, mi.totalMem),
            "dev" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "and" to "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        )
    }
    val labels = mapOf(
        "bat" to t("Battery", "باتری"), "temp" to t("Battery temperature", "دمای باتری"), "chg" to t("Charging", "در حال شارژ"),
        "net" to t("Network", "شبکه"), "sto" to t("Storage (free / total)", "حافظه (آزاد / کل)"), "ram" to t("RAM (free / total)", "رم (آزاد / کل)"),
        "dev" to t("Device", "دستگاه"), "and" to t("Android version", "نسخه اندروید")
    )
    ScreenFrame(t("System Information", "اطلاعات سیستم"), back) {
        SectionCard {
            rows.forEach { (k, v) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(labels[k] ?: k, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(v, fontWeight = FontWeight.Medium)
                }
            }
        }
        SectionCard(t("Shortcuts", "میانبرها")) {
            ActionRow(Icons.Filled.Wifi, t("Wi-Fi / Internet", "وای‌فای / اینترنت")) { ctx.safeStart(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)) }
            ActionRow(Icons.Filled.Bluetooth, "Bluetooth") { ctx.safeStart(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
        }
    }
}
