package com.smarttoolbox.ai

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class ToolboxApp : Application() {
    companion object {
        const val CH_FLOAT = "floating"
        const val CH_CAPTURE = "capture"
        const val CH_SHOT = "shots"
        const val CH_REMIND = "reminders"
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_FLOAT, "Floating toolbox", NotificationManager.IMPORTANCE_MIN).apply { setShowBadge(false) })
        nm.createNotificationChannel(NotificationChannel(CH_CAPTURE, "Screen capture", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CH_SHOT, "Screenshots", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CH_REMIND, "Reminders", NotificationManager.IMPORTANCE_HIGH))
    }
}
