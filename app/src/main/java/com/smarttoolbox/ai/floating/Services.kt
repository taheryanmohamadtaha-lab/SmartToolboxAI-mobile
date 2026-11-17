package com.smarttoolbox.ai.floating

import android.accessibilityservice.AccessibilityService
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.smarttoolbox.ai.MainActivity
import com.smarttoolbox.ai.R
import com.smarttoolbox.ai.ToolboxApp
import com.smarttoolbox.ai.data.Lang
import com.smarttoolbox.ai.data.Prefs
import com.smarttoolbox.ai.data.Reminders
import kotlinx.coroutines.flow.MutableStateFlow

object ServiceState {
    val running = MutableStateFlow(false)
    val hidden = MutableStateFlow(false)
}

class ToolboxAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onUnbind(intent: Intent?): Boolean { instance = null; return super.onUnbind(intent) }

    companion object {
        var instance: ToolboxAccessibilityService? = null
        fun global(ctx: Context, action: Int) {
            val s = instance
            if (s == null) {
                Toast.makeText(ctx, Lang.t(ctx, "Enable \"Smart Toolbox AI\" in Accessibility settings to use this tool", "برای استفاده از این ابزار، «جعبه‌ابزار هوشمند» را در تنظیمات دسترس‌پذیری فعال کنید"), Toast.LENGTH_LONG).show()
                try { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) {}
            } else s.performGlobalAction(action)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Reminders.rescheduleAll(context)
        if (Prefs.get(context).autoStart && Settings.canDrawOverlays(context)) FloatingService.start(context)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    @android.annotation.SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val text = intent.getStringExtra("text") ?: return
        val id = intent.getIntExtra("id", 0)
        val open = PendingIntent.getActivity(context, id, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, ToolboxApp.CH_REMIND)
            .setSmallIcon(R.drawable.ic_build)
            .setContentTitle(Lang.t(context, "Reminder", "یادآوری"))
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try { NotificationManagerCompat.from(context).notify(id, n) } catch (e: SecurityException) {}
        Reminders.drop(context, id)
    }
}
