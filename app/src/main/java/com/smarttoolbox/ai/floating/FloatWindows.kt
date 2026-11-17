package com.smarttoolbox.ai.floating

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.smarttoolbox.ai.core.CalcEngine
import com.smarttoolbox.ai.data.CalcHistory
import com.smarttoolbox.ai.data.Colors
import com.smarttoolbox.ai.data.Lang
import com.smarttoolbox.ai.data.Note
import com.smarttoolbox.ai.data.NoteStore
import com.smarttoolbox.ai.data.Prefs

private const val MP = ViewGroup.LayoutParams.MATCH_PARENT
private const val WC = ViewGroup.LayoutParams.WRAP_CONTENT

abstract class FloatWindow(
    protected val ctx: Context,
    protected val wm: WindowManager,
    private val titleText: String,
    wDp: Int, hDp: Int, focusable: Boolean
) {
    protected val density = ctx.resources.displayMetrics.density
    protected fun dp(v: Int) = (v * density).toInt()
    protected val prefs: Prefs = Prefs.get(ctx)

    val lp = WindowManager.LayoutParams(
        dp(wDp), dp(hDp), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        (if (focusable) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START; x = dp(24); y = dp(110) }

    protected lateinit var root: FrameLayout
    protected lateinit var surface: LinearLayout
    protected lateinit var headerBar: LinearLayout
    protected lateinit var content: FrameLayout
    var onClose: (() -> Unit)? = null
    private var shown = false

    protected abstract fun buildContent(parent: FrameLayout)
    protected open fun onClosed() {}

    protected fun rounded(color: Int, r: Int): GradientDrawable = GradientDrawable().apply { setColor(color); cornerRadius = dp(r).toFloat() }
    protected fun setSurfaceColor(c: Int) { surface.background = rounded(c, 18) }

    protected fun addHeaderButton(glyph: String, onClick: () -> Unit): TextView {
        val tv = TextView(ctx).apply {
            text = glyph; setTextColor(Color.WHITE); textSize = 16f; gravity = Gravity.CENTER
            setOnClickListener { onClick() }
        }
        headerBar.addView(tv, LinearLayout.LayoutParams(dp(38), dp(40)))
        return tv
    }

    private fun update() { try { wm.updateViewLayout(root, lp) } catch (e: Exception) {} }

    fun show() {
        val accent = prefs.floatColor
        root = FrameLayout(ctx)
        surface = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; clipToOutline = true }
        setSurfaceColor(Color.WHITE)
        headerBar = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Colors.dark(accent), Colors.light(accent)))
            setPadding(dp(12), 0, 0, 0)
        }
        headerBar.addView(TextView(ctx).apply {
            text = titleText; setTextColor(Color.WHITE); textSize = 15f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER_VERTICAL
        }, LinearLayout.LayoutParams(0, dp(40), 1f))
        surface.addView(headerBar, LinearLayout.LayoutParams(MP, dp(40)))
        content = FrameLayout(ctx)
        surface.addView(content, LinearLayout.LayoutParams(MP, 0, 1f))
        root.addView(surface, FrameLayout.LayoutParams(MP, MP))

        val handle = TextView(ctx).apply { text = "◢"; setTextColor(0x99888888.toInt()); textSize = 16f; gravity = Gravity.CENTER }
        root.addView(handle, FrameLayout.LayoutParams(dp(30), dp(30), Gravity.BOTTOM or Gravity.END))
        var sw = 0; var sh = 0; var rx = 0f; var ry = 0f
        handle.setOnTouchListener { _, e ->
            when (e.action) {
                android.view.MotionEvent.ACTION_DOWN -> { sw = lp.width; sh = lp.height; rx = e.rawX; ry = e.rawY }
                android.view.MotionEvent.ACTION_MOVE -> {
                    lp.width = (sw + e.rawX - rx).toInt().coerceAtLeast(dp(220))
                    lp.height = (sh + e.rawY - ry).toInt().coerceAtLeast(dp(260))
                    update()
                }
            }
            true
        }

        buildContent(content)
        addHeaderButton("✕") { close() }

        var sx = 0; var sy = 0; var dx = 0f; var dy = 0f
        headerBar.setOnTouchListener { _, e ->
            when (e.action) {
                android.view.MotionEvent.ACTION_DOWN -> { sx = lp.x; sy = lp.y; dx = e.rawX; dy = e.rawY }
                android.view.MotionEvent.ACTION_MOVE -> { lp.x = (sx + e.rawX - dx).toInt(); lp.y = (sy + e.rawY - dy).toInt(); update() }
            }
            true
        }
        wm.addView(root, lp)
        shown = true
    }

    fun close() {
        if (!shown) return
        shown = false
        onClosed()
        try { wm.removeView(root) } catch (e: Exception) {}
        onClose?.invoke()
    }
}

class CalculatorWindow(ctx: Context, wm: WindowManager) :
    FloatWindow(ctx, wm, Lang.t(ctx, "Calculator", "ماشین‌حساب"), 280, 430, false) {

    private var expr = ""
    private var memory = 0.0
    private var dark = prefs.calcDark
    private var historyShown = false
    private lateinit var tvExpr: TextView
    private lateinit var tvRes: TextView
    private lateinit var histScroll: ScrollView
    private lateinit var histText: TextView
    private val keys = ArrayList<Pair<TextView, Int>>()

    override fun buildContent(parent: FrameLayout) {
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val area = FrameLayout(ctx)
        val disp = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.BOTTOM or Gravity.END
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        tvExpr = TextView(ctx).apply { textSize = 28f; gravity = Gravity.END; maxLines = 2 }
        tvRes = TextView(ctx).apply { textSize = 16f; gravity = Gravity.END }
        disp.addView(tvExpr, LinearLayout.LayoutParams(MP, WC))
        disp.addView(tvRes, LinearLayout.LayoutParams(MP, WC))
        histText = TextView(ctx).apply { textSize = 14f; gravity = Gravity.END; setPadding(dp(14), dp(8), dp(14), dp(8)) }
        histScroll = ScrollView(ctx).apply { visibility = View.GONE; addView(histText) }
        area.addView(disp, FrameLayout.LayoutParams(MP, MP))
        area.addView(histScroll, FrameLayout.LayoutParams(MP, MP))
        col.addView(area, LinearLayout.LayoutParams(MP, 0, 1.3f))

        val rows = listOf(
            listOf("MC", "MR", "M+", "M−"),
            listOf("C", "⌫", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("⧉", "0", ".", "=")
        )
        for (r in rows) {
            val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            for (k in r) {
                val lpk = LinearLayout.LayoutParams(0, MP, 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }
                row.addView(makeKey(k), lpk)
            }
            col.addView(row, LinearLayout.LayoutParams(MP, 0, 1f))
        }
        parent.addView(col, FrameLayout.LayoutParams(MP, MP))
        addHeaderButton("🕘") { toggleHistory() }
        addHeaderButton("◐") { dark = !dark; prefs.calcDark = dark; applyTheme() }
        applyTheme()
        refresh()
    }

    private fun makeKey(k: String): TextView {
        val type = when {
            k in listOf("÷", "×", "−", "+", "=") -> 1
            k.length == 1 && (k[0].isDigit() || k == ".") -> 0
            else -> 2
        }
        val tv = TextView(ctx).apply { text = k; gravity = Gravity.CENTER; textSize = 20f; setOnClickListener { press(k) } }
        keys.add(tv to type)
        return tv
    }

    private fun applyTheme() {
        val accent = prefs.floatColor
        setSurfaceColor(if (dark) 0xFF1E1B24.toInt() else Color.WHITE)
        val txt = if (dark) Color.WHITE else 0xFF1B1B1F.toInt()
        tvExpr.setTextColor(txt)
        histText.setTextColor(txt)
        tvRes.setTextColor(if (dark) 0xFFB9B3C4.toInt() else 0xFF6F6A78.toInt())
        for ((tv, type) in keys) {
            when (type) {
                0 -> { tv.background = rounded(if (dark) 0xFF2B2733.toInt() else 0xFFF1ECF6.toInt(), 12); tv.setTextColor(txt) }
                1 -> { tv.background = rounded(accent, 12); tv.setTextColor(Color.WHITE) }
                else -> {
                    tv.background = rounded(if (dark) 0xFF3A3345.toInt() else 0xFFE8DEF8.toInt(), 12)
                    tv.setTextColor(if (dark) 0xFFD0BCFF.toInt() else Colors.dark(accent))
                }
            }
        }
    }

    private fun current(): Double? = try { CalcEngine.eval(expr) } catch (e: Exception) { null }

    private fun press(k: String) {
        when (k) {
            "C" -> expr = ""
            "⌫" -> expr = expr.dropLast(1)
            "=" -> {
                try {
                    val f = CalcEngine.format(CalcEngine.eval(expr))
                    CalcHistory.add(prefs, "$expr = $f")
                    expr = f
                } catch (e: Exception) {
                    tvRes.text = Lang.t(ctx, "Error", "خطا")
                    return
                }
            }
            "MC" -> memory = 0.0
            "MR" -> expr += CalcEngine.format(memory)
            "M+" -> memory += current() ?: 0.0
            "M−" -> memory -= current() ?: 0.0
            "⧉" -> {
                val text = current()?.let { CalcEngine.format(it) } ?: expr
                ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("result", text))
                Toast.makeText(ctx, Lang.t(ctx, "Copied", "کپی شد"), Toast.LENGTH_SHORT).show()
            }
            else -> expr += k
        }
        refresh()
    }

    private fun refresh() {
        tvExpr.text = if (expr.isEmpty()) "0" else expr
        val c = current()
        val hasOp = expr.drop(1).any { it in "+−×÷%" }
        val mem = if (memory != 0.0) "M " else ""
        tvRes.text = mem + if (c != null && hasOp) "= " + CalcEngine.format(c) else ""
        if (historyShown) histText.text = CalcHistory.list(prefs).joinToString("\n")
    }

    private fun toggleHistory() {
        historyShown = !historyShown
        histScroll.visibility = if (historyShown) View.VISIBLE else View.GONE
        refresh()
    }
}

class NotesWindow(ctx: Context, wm: WindowManager) :
    FloatWindow(ctx, wm, Lang.t(ctx, "Notes", "یادداشت‌ها"), 300, 380, true) {

    private val store = NoteStore(ctx)
    private lateinit var edit: EditText
    private lateinit var counter: TextView
    private lateinit var pinBtn: TextView
    private var cur: Note = Note(System.currentTimeMillis(), "", false, System.currentTimeMillis())
    private var loading = false
    private val handler = Handler(Looper.getMainLooper())
    private val saver = Runnable { persist() }
    private val alphas = floatArrayOf(1f, 0.8f, 0.6f, 0.4f)
    private var alphaIdx = 0

    private fun glyph(t: String, onClick: () -> Unit) = TextView(ctx).apply {
        text = t; textSize = 17f; gravity = Gravity.CENTER; setTextColor(0xFF4A4458.toInt()); setOnClickListener { onClick() }
    }

    override fun buildContent(parent: FrameLayout) {
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val bar = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(0xFFF3EEF8.toInt()) }
        counter = TextView(ctx).apply { textSize = 13f; gravity = Gravity.CENTER; setTextColor(0xFF4A4458.toInt()) }
        bar.addView(glyph("◀") { move(-1) }, LinearLayout.LayoutParams(dp(34), dp(36)))
        bar.addView(counter, LinearLayout.LayoutParams(dp(46), dp(36)))
        bar.addView(glyph("▶") { move(1) }, LinearLayout.LayoutParams(dp(34), dp(36)))
        bar.addView(glyph("＋") { newNote() }, LinearLayout.LayoutParams(dp(34), dp(36)))
        bar.addView(glyph("⧉") { copy() }, LinearLayout.LayoutParams(dp(34), dp(36)))
        bar.addView(glyph("↗") { share() }, LinearLayout.LayoutParams(dp(34), dp(36)))
        bar.addView(glyph("🗑") { delete() }, LinearLayout.LayoutParams(dp(34), dp(36)))
        edit = EditText(ctx).apply {
            gravity = Gravity.TOP or Gravity.START
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(0xFF1B1B1F.toInt())
            hint = Lang.t(ctx, "Write a note…", "یادداشت بنویسید…")
            setPadding(dp(12), dp(8), dp(12), dp(24))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (!loading) { handler.removeCallbacks(saver); handler.postDelayed(saver, 400) }
                }
            })
        }
        col.addView(bar, LinearLayout.LayoutParams(MP, dp(36)))
        col.addView(edit, LinearLayout.LayoutParams(MP, 0, 1f))
        parent.addView(col, FrameLayout.LayoutParams(MP, MP))
        pinBtn = addHeaderButton("☆") { togglePin() }
        addHeaderButton("◐") { alphaIdx = (alphaIdx + 1) % alphas.size; root.alpha = alphas[alphaIdx] }
        cur = store.all().firstOrNull() ?: cur
        load(cur)
    }

    private fun load(n: Note) {
        cur = n
        loading = true
        edit.setText(n.text)
        edit.setSelection(edit.text.length)
        loading = false
        pinBtn.text = if (n.pinned) "★" else "☆"
        val all = store.all()
        val i = all.indexOfFirst { it.id == n.id }
        counter.text = if (i >= 0) "${i + 1}/${all.size}" else "${all.size + 1}/${all.size + 1}"
    }

    private fun persist() {
        val text = edit.text.toString()
        if (text.isBlank()) store.delete(cur.id) else { cur = cur.copy(text = text, updated = System.currentTimeMillis()); store.upsert(cur) }
        val all = store.all()
        val i = all.indexOfFirst { it.id == cur.id }
        counter.text = if (i >= 0) "${i + 1}/${all.size}" else "0/${all.size}"
    }

    private fun move(d: Int) {
        handler.removeCallbacks(saver); persist()
        val all = store.all()
        if (all.isEmpty()) return
        val i = all.indexOfFirst { it.id == cur.id }
        val ni = if (i < 0) 0 else (i + d + all.size) % all.size
        load(all[ni])
    }

    private fun newNote() {
        handler.removeCallbacks(saver); persist()
        val now = System.currentTimeMillis()
        load(Note(now, "", false, now))
    }

    private fun togglePin() {
        handler.removeCallbacks(saver)
        cur = cur.copy(pinned = !cur.pinned)
        pinBtn.text = if (cur.pinned) "★" else "☆"
        persist()
    }

    private fun copy() {
        ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("note", edit.text.toString()))
        Toast.makeText(ctx, Lang.t(ctx, "Copied", "کپی شد"), Toast.LENGTH_SHORT).show()
    }

    private fun share() {
        val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, edit.text.toString())
        ctx.startActivity(Intent.createChooser(i, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun delete() {
        handler.removeCallbacks(saver)
        store.delete(cur.id)
        val now = System.currentTimeMillis()
        load(store.all().firstOrNull() ?: Note(now, "", false, now))
    }

    override fun onClosed() { handler.removeCallbacks(saver); persist() }
}
