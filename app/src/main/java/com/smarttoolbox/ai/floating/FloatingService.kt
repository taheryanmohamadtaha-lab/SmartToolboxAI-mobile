package com.smarttoolbox.ai.floating

import android.animation.ValueAnimator
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.smarttoolbox.ai.MainActivity
import com.smarttoolbox.ai.R
import com.smarttoolbox.ai.ToolboxApp
import com.smarttoolbox.ai.core.ToolActions
import com.smarttoolbox.ai.core.Tools
import com.smarttoolbox.ai.data.Colors
import com.smarttoolbox.ai.data.Lang
import com.smarttoolbox.ai.data.Prefs
import com.smarttoolbox.ai.data.ShortcutStore
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val MP = ViewGroup.LayoutParams.MATCH_PARENT

class FloatingService : Service() {
    companion object {
        const val ACTION_START = "com.smarttoolbox.ai.START"
        const val ACTION_STOP = "com.smarttoolbox.ai.STOP"
        const val ACTION_REFRESH = "com.smarttoolbox.ai.REFRESH"
        const val ACTION_TOGGLE_HIDE = "com.smarttoolbox.ai.TOGGLE_HIDE"
        const val ACTION_CALC = "com.smarttoolbox.ai.CALC"
        const val ACTION_NOTES = "com.smarttoolbox.ai.NOTES"
        private const val NOTIF_ID = 1
        var instance: FloatingService? = null

        fun start(ctx: Context, action: String = ACTION_START) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, FloatingService::class.java).setAction(action))
        }
        fun refresh(ctx: Context) { if (ServiceState.running.value) start(ctx, ACTION_REFRESH) }
    }

    private data class Entry(val id: String, val label: String, val iconRes: Int = 0, val drawable: Drawable? = null, val text: String? = null, val color: Int = 0)

    private lateinit var wm: WindowManager
    private lateinit var prefs: Prefs
    private val handler = Handler(Looper.getMainLooper())
    private var bubble: View? = null
    private var menuView: View? = null
    private var calc: CalculatorWindow? = null
    private var notes: NotesWindow? = null
    private var tempHidden = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = Prefs.get(this)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIF_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(NOTIF_ID, buildNotification())
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        when (action) {
            ACTION_STOP -> { shutdown(); return START_NOT_STICKY }
            ACTION_TOGGLE_HIDE -> setUserHidden(!ServiceState.hidden.value)
            ACTION_REFRESH -> if (bubble != null) buildBubble()
            ACTION_CALC -> showCalc()
            ACTION_NOTES -> showNotes()
            else -> {
                if (bubble == null) buildBubble()
                ServiceState.running.value = true
            }
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        closeMenu()
        if (bubble != null) buildBubble()
    }

    override fun onDestroy() {
        removeAll()
        ServiceState.running.value = false
        instance = null
        super.onDestroy()
    }

    // ---------- notification ----------
    private fun buildNotification(): Notification {
        val hidden = ServiceState.hidden.value
        fun pi(action: String, code: Int) = PendingIntent.getService(
            this, code, Intent(this, FloatingService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val open = PendingIntent.getActivity(this, 9, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, ToolboxApp.CH_FLOAT)
            .setSmallIcon(R.drawable.ic_build)
            .setContentTitle("Smart Toolbox AI")
            .setContentText(Lang.t(this, "Floating toolbox is active", "جعبه‌ابزار شناور فعال است"))
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(0, if (hidden) Lang.t(this, "Show", "نمایش") else Lang.t(this, "Hide", "مخفی"), pi(ACTION_TOGGLE_HIDE, 1))
            .addAction(0, Lang.t(this, "Stop", "توقف"), pi(ACTION_STOP, 2))
            .build()
    }

    private fun setUserHidden(h: Boolean) {
        ServiceState.hidden.value = h
        applyVisibility()
        getSystemService(android.app.NotificationManager::class.java).notify(NOTIF_ID, buildNotification())
    }

    fun setTemporarilyHidden(h: Boolean) { tempHidden = h; applyVisibility() }

    private fun applyVisibility() {
        bubble?.visibility = if (ServiceState.hidden.value || tempHidden || menuView != null) View.INVISIBLE else View.VISIBLE
    }

    // ---------- bubble ----------
    private fun removeBubble() {
        bubble?.let { try { wm.removeView(it) } catch (e: Exception) {} }
        bubble = null
    }

    private fun removeAll() {
        closeMenu()
        calc?.close(); notes?.close()
        removeBubble()
    }

    private fun shutdown() {
        removeAll()
        ServiceState.running.value = false
        ServiceState.hidden.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun maybeStop() {
        if (bubble == null && calc == null && notes == null) shutdown()
    }

    private fun buildBubble() {
        removeBubble()
        val size = dp(prefs.floatSize)
        val accent = prefs.floatColor
        val sw = resources.displayMetrics.widthPixels
        val sh = resources.displayMetrics.heightPixels
        val v = FrameLayout(this)
        v.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Colors.dark(accent), Colors.light(accent))).apply {
            shape = GradientDrawable.OVAL
            setStroke(dp(2), 0x88FFFFFF.toInt())
        }
        val icons = Tools.floatIcons
        val icon = ImageView(this).apply {
            setImageResource(icons[prefs.floatIcon.coerceIn(0, icons.size - 1)])
            setColorFilter(Color.WHITE)
            val pad = size / 4
            setPadding(pad, pad, pad, pad)
        }
        v.addView(icon, FrameLayout.LayoutParams(MP, MP))
        v.alpha = prefs.floatAlpha

        val lp = WindowManager.LayoutParams(
            size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val sx = prefs.floatX
            x = if (sx >= 0) sx.coerceIn(0, sw - size) else if (prefs.floatSide == 0) 0 else sw - size
            y = prefs.floatY.coerceIn(0, sh - size)
        }

        val slop = ViewConfiguration.get(this).scaledTouchSlop
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var moved = false
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { downX = e.rawX; downY = e.rawY; startX = lp.x; startY = lp.y; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX; val dy = e.rawY - downY
                    if (!moved && hypot(dx, dy) > slop) moved = true
                    if (moved) {
                        lp.x = (startX + dx).toInt(); lp.y = (startY + dy).toInt()
                        try { wm.updateViewLayout(v, lp) } catch (ex: Exception) {}
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) { haptic(v); openMenu() } else snap(v, lp)
                }
            }
            true
        }
        wm.addView(v, lp)
        bubble = v
        applyVisibility()
    }

    private fun haptic(v: View) { if (prefs.haptic) v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) }

    private fun snap(v: View, lp: WindowManager.LayoutParams) {
        val sw = resources.displayMetrics.widthPixels
        val sh = resources.displayMetrics.heightPixels
        val size = lp.width
        val tx = if (prefs.snapEdge) { if (lp.x + size / 2 < sw / 2) 0 else sw - size } else lp.x.coerceIn(0, sw - size)
        val ty = lp.y.coerceIn(0, sh - size)
        val sx = lp.x; val sy = lp.y
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 180
            addUpdateListener { a ->
                val f = a.animatedValue as Float
                lp.x = (sx + (tx - sx) * f).toInt(); lp.y = (sy + (ty - sy) * f).toInt()
                try { wm.updateViewLayout(v, lp) } catch (e: Exception) {}
            }
            start()
        }
        prefs.floatX = tx; prefs.floatY = ty
        prefs.floatSide = if (tx + size / 2 < sw / 2) 0 else 1
    }

    // ---------- radial menu ----------
    private fun entries(): List<Entry> {
        val out = ArrayList<Entry>()
        val fa = Lang.isFa(this)
        prefs.menuItems.split(",").filter { it.isNotBlank() }.forEach { id ->
            Tools.find(id)?.let { out.add(Entry(it.id, if (fa) it.fa else it.en, iconRes = it.icon)) }
        }
        ShortcutStore(this).all().filter { it.inMenu }.forEach { s ->
            var d: Drawable? = null
            if (s.type == "app") d = try { packageManager.getApplicationIcon(s.target) } catch (e: Exception) { null }
            out.add(Entry("sc:" + s.id, s.title, drawable = d, text = if (d == null) s.icon.ifBlank { s.title.take(1) } else null, color = s.color))
        }
        return out.take(12)
    }

    private fun openMenu() {
        if (menuView != null) return
        val list = entries()
        val accent = prefs.floatColor
        val root = FrameLayout(this)
        root.setBackgroundColor(0x99000000.toInt())
        root.setOnClickListener { closeMenu() }
        val panelSize = dp(300)
        val panel = FrameLayout(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xF2101014.toInt()); setStroke(dp(6), accent) }
            isClickable = true
        }
        root.addView(panel, FrameLayout.LayoutParams(panelSize, panelSize, Gravity.CENTER))
        val n = list.size
        val radius = dp(104).toFloat()
        list.forEachIndexed { i, e ->
            val ang = -Math.PI / 2 + 2 * Math.PI * i / n
            val item = makeItem(e)
            item.translationX = (radius * cos(ang)).toFloat()
            item.translationY = (radius * sin(ang)).toFloat()
            panel.addView(item, FrameLayout.LayoutParams(dp(52), dp(52), Gravity.CENTER))
        }
        val close = FrameLayout(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Colors.dark(accent), Colors.light(accent))).apply { shape = GradientDrawable.OVAL }
            setOnClickListener { closeMenu() }
        }
        close.addView(ImageView(this).apply { setImageResource(R.drawable.ic_close); setColorFilter(Color.WHITE); setPadding(dp(16), dp(16), dp(16), dp(16)) }, FrameLayout.LayoutParams(MP, MP))
        panel.addView(close, FrameLayout.LayoutParams(dp(58), dp(58), Gravity.CENTER))

        val lp = WindowManager.LayoutParams(
            MP, MP, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        wm.addView(root, lp)
        menuView = root
        applyVisibility()
        panel.scaleX = 0.7f; panel.scaleY = 0.7f; panel.alpha = 0f
        panel.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(160).start()
    }

    private fun makeItem(e: Entry): View {
        val accent = prefs.floatColor
        val item = FrameLayout(this)
        item.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(if (e.color != 0) e.color else Colors.light(accent)) }
        item.contentDescription = e.label
        item.setOnClickListener {
            haptic(item)
            closeMenu()
            handler.postDelayed({ runTool(e.id) }, 180)
        }
        item.setOnLongClickListener { Toast.makeText(this, e.label, Toast.LENGTH_SHORT).show(); true }
        when {
            e.drawable != null -> item.addView(ImageView(this).apply { setImageDrawable(e.drawable); setPadding(dp(8), dp(8), dp(8), dp(8)) }, FrameLayout.LayoutParams(MP, MP))
            e.iconRes != 0 -> item.addView(ImageView(this).apply { setImageResource(e.iconRes); setColorFilter(Color.WHITE); setPadding(dp(13), dp(13), dp(13), dp(13)) }, FrameLayout.LayoutParams(MP, MP))
            else -> item.addView(TextView(this).apply { text = e.text; textSize = 20f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }, FrameLayout.LayoutParams(MP, MP))
        }
        return item
    }

    private fun closeMenu() {
        menuView?.let { try { wm.removeView(it) } catch (e: Exception) {} }
        menuView = null
        applyVisibility()
    }

    private fun runTool(id: String) {
        when {
            id == "calc" -> showCalc()
            id == "notes" -> showNotes()
            id.startsWith("sc:") -> {
                val sid = id.removePrefix("sc:").toLongOrNull()
                ShortcutStore(this).all().firstOrNull { it.id == sid }?.let { ToolActions.runShortcut(this, it) }
            }
            else -> ToolActions.run(this, id)
        }
    }

    private fun showCalc() {
        if (calc != null) return
        calc = CalculatorWindow(this, wm).also { it.onClose = { calc = null; maybeStop() }; it.show() }
    }

    private fun showNotes() {
        if (notes != null) return
        notes = NotesWindow(this, wm).also { it.onClose = { notes = null; maybeStop() }; it.show() }
    }
}
