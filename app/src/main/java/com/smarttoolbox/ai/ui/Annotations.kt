package com.smarttoolbox.ai.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

sealed class Ann { abstract val color: Int }
data class StrokeAnn(val pts: List<PointF>, override val color: Int, val w: Float) : Ann()
data class ShapeAnn(val kind: String, val a: PointF, val b: PointF, override val color: Int, val w: Float) : Ann()
data class TextAnn(val text: String, val at: PointF, override val color: Int, val size: Float) : Ann()
class Snap(val bmp: Bitmap, val anns: List<Ann>)

fun drawAnn(c: Canvas, base: Bitmap, a: Ann) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = a.color
    when (a) {
        is StrokeAnn -> {
            p.style = Paint.Style.STROKE; p.strokeWidth = a.w; p.strokeCap = Paint.Cap.ROUND; p.strokeJoin = Paint.Join.ROUND
            if (a.pts.size == 1) c.drawPoint(a.pts[0].x, a.pts[0].y, p)
            else {
                val path = Path()
                a.pts.forEachIndexed { i, pt -> if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y) }
                c.drawPath(path, p)
            }
        }
        is ShapeAnn -> {
            p.style = Paint.Style.STROKE; p.strokeWidth = a.w; p.strokeCap = Paint.Cap.ROUND
            val l = min(a.a.x, a.b.x); val r = max(a.a.x, a.b.x)
            val t = min(a.a.y, a.b.y); val b = max(a.a.y, a.b.y)
            when (a.kind) {
                "line" -> c.drawLine(a.a.x, a.a.y, a.b.x, a.b.y, p)
                "arrow" -> {
                    c.drawLine(a.a.x, a.a.y, a.b.x, a.b.y, p)
                    val ang = atan2(a.b.y - a.a.y, a.b.x - a.a.x)
                    val hl = a.w * 5f
                    for (s in listOf(-1, 1)) {
                        val th = ang + Math.PI.toFloat() + s * 0.5f
                        c.drawLine(a.b.x, a.b.y, a.b.x + hl * cos(th), a.b.y + hl * sin(th), p)
                    }
                }
                "rect" -> c.drawRect(l, t, r, b, p)
                "circle" -> c.drawOval(RectF(l, t, r, b), p)
                "crop" -> {
                    p.color = Color.WHITE
                    p.pathEffect = DashPathEffect(floatArrayOf(20f, 12f), 0f)
                    c.drawRect(l, t, r, b, p)
                }
                "pixel", "blur" -> censor(c, base, a)
            }
        }
        is TextAnn -> {
            p.style = Paint.Style.FILL; p.textSize = a.size; p.isFakeBoldText = true
            a.text.split("\n").forEachIndexed { i, line -> c.drawText(line, a.at.x, a.at.y + i * a.size * 1.15f, p) }
        }
    }
}

private fun censor(c: Canvas, base: Bitmap, a: ShapeAnn) {
    val l = min(a.a.x, a.b.x).toInt().coerceIn(0, base.width - 1)
    val t = min(a.a.y, a.b.y).toInt().coerceIn(0, base.height - 1)
    val r = max(a.a.x, a.b.x).toInt().coerceIn(l + 1, base.width)
    val b = max(a.a.y, a.b.y).toInt().coerceIn(t + 1, base.height)
    val w = r - l
    val h = b - t
    if (w < 2 || h < 2) return
    val src = Bitmap.createBitmap(base, l, t, w, h)
    val pix = a.kind == "pixel"
    val div = if (pix) (base.width / 45).coerceAtLeast(8) else (base.width / 60).coerceAtLeast(6)
    val small = Bitmap.createScaledBitmap(src, (w / div).coerceAtLeast(1), (h / div).coerceAtLeast(1), !pix)
    val paint = Paint().apply { isFilterBitmap = !pix }
    c.drawBitmap(small, null, Rect(l, t, r, b), paint)
}
