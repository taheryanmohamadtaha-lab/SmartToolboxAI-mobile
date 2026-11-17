package com.smarttoolbox.ai.core

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.Settings
import android.accessibilityservice.AccessibilityService
import android.widget.Toast
import com.smarttoolbox.ai.MainActivity
import com.smarttoolbox.ai.R
import com.smarttoolbox.ai.data.Lang
import com.smarttoolbox.ai.data.Prefs
import com.smarttoolbox.ai.data.ShortcutItem
import com.smarttoolbox.ai.floating.ToolboxAccessibilityService
import com.smarttoolbox.ai.screenshot.ScreenshotRequestActivity

data class ToolDef(val id: String, val en: String, val fa: String, val icon: Int)

object Tools {
    val all = listOf(
        ToolDef("home", "Home", "خانه", R.drawable.ic_home),
        ToolDef("shot", "Screenshot", "اسکرین‌شات", R.drawable.ic_camera),
        ToolDef("calc", "Calculator", "ماشین‌حساب", R.drawable.ic_calc),
        ToolDef("notes", "Notes", "یادداشت", R.drawable.ic_note),
        ToolDef("torch", "Flashlight", "چراغ‌قوه", R.drawable.ic_flash),
        ToolDef("lock", "Lock screen", "قفل صفحه", R.drawable.ic_lock),
        ToolDef("recents", "Recent apps", "برنامه‌های اخیر", R.drawable.ic_apps),
        ToolDef("back", "Back", "بازگشت", R.drawable.ic_back),
        ToolDef("brightness", "Brightness", "روشنایی", R.drawable.ic_brightness),
        ToolDef("settings", "Settings", "تنظیمات", R.drawable.ic_settings),
        ToolDef("open", "Open Toolbox", "باز کردن برنامه", R.drawable.ic_build)
    )
    val floatIcons = listOf(R.drawable.ic_build, R.drawable.ic_apps, R.drawable.ic_brightness, R.drawable.ic_settings)
    fun find(id: String): ToolDef? = all.firstOrNull { it.id == id }
}

object Torch {
    private var on = false
    private var registered = false

    fun toggle(ctx: Context): Boolean {
        val cm = ctx.getSystemService(CameraManager::class.java)
        if (!registered) {
            registered = true
            cm.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) { on = enabled }
            }, null)
        }
        return try {
            val id = cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true } ?: return false
            on = !on
            cm.setTorchMode(id, on)
            true
        } catch (e: CameraAccessException) { false } catch (e: Exception) { false }
    }
}

object ToolActions {
    private fun start(ctx: Context, i: Intent) {
        try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (e: Exception) { Toast.makeText(ctx, Lang.t(ctx, "Not available on this device", "در این دستگاه در دسترس نیست"), Toast.LENGTH_SHORT).show() }
    }

    fun run(ctx: Context, id: String) {
        when (id) {
            "home" -> start(ctx, Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
            "shot" -> ScreenshotRequestActivity.launch(ctx, false)
            "torch" -> if (!Torch.toggle(ctx)) Toast.makeText(ctx, Lang.t(ctx, "No flashlight found", "چراغ‌قوه پیدا نشد"), Toast.LENGTH_SHORT).show()
            "lock" -> ToolboxAccessibilityService.global(ctx, AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            "recents" -> ToolboxAccessibilityService.global(ctx, AccessibilityService.GLOBAL_ACTION_RECENTS)
            "back" -> ToolboxAccessibilityService.global(ctx, AccessibilityService.GLOBAL_ACTION_BACK)
            "brightness" -> start(ctx, Intent(Settings.ACTION_DISPLAY_SETTINGS))
            "settings" -> start(ctx, Intent(Settings.ACTION_SETTINGS))
            "open" -> start(ctx, Intent(ctx, MainActivity::class.java))
        }
    }

    fun runShortcut(ctx: Context, s: ShortcutItem) {
        if (s.type == "app") {
            val i = ctx.packageManager.getLaunchIntentForPackage(s.target)
            if (i != null) start(ctx, i) else Toast.makeText(ctx, Lang.t(ctx, "App not found", "برنامه پیدا نشد"), Toast.LENGTH_SHORT).show()
        } else {
            var u = s.target.trim()
            if (!u.contains("://")) u = "https://$u"
            start(ctx, Intent(Intent.ACTION_VIEW, Uri.parse(u)))
        }
    }

    fun prefs(ctx: Context) = Prefs.get(ctx)
}
