package com.smarttoolbox.ai.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import com.smarttoolbox.ai.MainViewModel
import com.smarttoolbox.ai.floating.FloatingService
import com.smarttoolbox.ai.floating.ServiceState

private val prodRoutes = setOf("notes", "clip", "stopwatch", "timer", "clock", "reminders", "converter", "translator", "qrscan", "qrgen")

@Composable
fun AppRoot(vm: MainViewModel, pendingEdit: Uri?, consume: () -> Unit) {
    val fa = vm.isFa()
    CompositionLocalProvider(
        LocalFa provides fa,
        LocalLayoutDirection provides if (fa) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        AppTheme(vm.themeMode, vm.accent) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { Nav(vm, pendingEdit, consume) }
        }
    }
}

@Composable
private fun Nav(vm: MainViewModel, pendingEdit: Uri?, consume: () -> Unit) {
    val ctx = LocalContext.current
    var route by rememberSaveable { mutableStateOf("home") }
    var editUri by remember { mutableStateOf<Uri?>(null) }
    var dialog by remember { mutableStateOf("") }
    val running by ServiceState.running.collectAsState()

    LaunchedEffect(pendingEdit) {
        if (pendingEdit != null) { editUri = pendingEdit; route = "editor"; consume() }
    }

    fun parent(r: String): String = when {
        r == "home" -> "home"
        r == "editor" -> "shot"
        r in prodRoutes -> "prod"
        else -> "home"
    }
    BackHandler(enabled = route != "home") { route = parent(route) }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { FloatingService.start(ctx) }

    fun startFloating() {
        if (!Settings.canDrawOverlays(ctx)) { dialog = "overlay"; return }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            dialog = "notif"; return
        }
        FloatingService.start(ctx)
    }

    val go: (String) -> Unit = { route = it }
    val back: () -> Unit = { route = parent(route) }

    when (route) {
        "home" -> DashboardScreen(running, { on -> if (on) startFloating() else FloatingService.start(ctx, FloatingService.ACTION_STOP) }, go)
        "floating" -> FloatingScreen(back)
        "shot" -> ShotStudioScreen(back) { editUri = it; route = "editor" }
        "editor" -> editUri?.let { EditorScreen(it, back) } ?: run { route = "shot" }
        "tools" -> MiniToolsScreen(back, go)
        "prod" -> ProductivityScreen(back, go)
        "sys" -> SystemInfoScreen(back)
        "shortcuts" -> ShortcutsScreen(back)
        "history" -> HistoryScreen(back) { editUri = it; route = "editor" }
        "settings" -> SettingsScreen(vm, back)
        "notes" -> NotesScreen(back)
        "clip" -> ClipboardScreen(back)
        "stopwatch" -> StopwatchScreen(back)
        "timer" -> TimerScreen(back)
        "clock" -> ClockScreen(back)
        "reminders" -> RemindersScreen(back)
        "converter" -> ConverterScreen(back)
        "translator" -> TranslatorScreen(back)
        "qrscan" -> QrScanScreen(back)
        "qrgen" -> QrGenScreen(back)
    }

    if (dialog == "overlay") AlertDialog(
        onDismissRequest = { dialog = "" },
        title = { Text(t("Display over other apps", "نمایش روی سایر برنامه‌ها")) },
        text = { Text(t("The floating button needs Android's \"Display over other apps\" permission so it can appear on top of other apps. Nothing is read from those apps. Turn it on for Smart Toolbox AI, then come back and switch the toolbox on.",
            "دکمه شناور برای نمایش روی برنامه‌های دیگر به مجوز «نمایش روی سایر برنامه‌ها» نیاز دارد. هیچ اطلاعاتی از آن برنامه‌ها خوانده نمی‌شود. آن را برای جعبه‌ابزار هوشمند روشن کنید، برگردید و جعبه‌ابزار را فعال کنید.")) },
        confirmButton = { TextButton(onClick = {
            dialog = ""
            ctx.safeStart(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
        }) { Text(t("Open settings", "باز کردن تنظیمات")) } },
        dismissButton = { TextButton(onClick = { dialog = "" }) { Text(t("Cancel", "انصراف")) } }
    )
    if (dialog == "notif") AlertDialog(
        onDismissRequest = { dialog = "" },
        title = { Text(t("Notification permission", "مجوز اعلان")) },
        text = { Text(t("Android shows a persistent notification while the floating toolbox is running (with Hide and Stop buttons). Please allow notifications.",
            "اندروید هنگام اجرای جعبه‌ابزار شناور یک اعلان دائمی (با دکمه‌های مخفی و توقف) نشان می‌دهد. لطفاً اعلان‌ها را مجاز کنید.")) },
        confirmButton = { TextButton(onClick = { dialog = ""; notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text(t("Allow", "مجاز کردن")) } },
        dismissButton = { TextButton(onClick = { dialog = ""; FloatingService.start(ctx) }) { Text(t("Skip", "رد کردن")) } }
    )
}
