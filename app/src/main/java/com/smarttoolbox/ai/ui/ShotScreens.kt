package com.smarttoolbox.ai.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.net.Uri
import android.text.format.Formatter
import android.util.Size
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.smarttoolbox.ai.data.ImageSaver
import com.smarttoolbox.ai.data.Lang
import com.smarttoolbox.ai.data.Prefs
import com.smarttoolbox.ai.screenshot.ScreenshotRequestActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

@Composable
fun Thumb(uri: Uri, modifier: Modifier) {
    val ctx = LocalContext.current
    val bmp by produceState<Bitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            try { ctx.contentResolver.loadThumbnail(uri, Size(320, 320), null) } catch (e: Exception) { null }
        }
    }
    val b = bmp
    if (b != null) Image(b.asImageBitmap(), null, modifier, contentScale = ContentScale.Crop)
    else Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
}

@Composable
fun ShotGallery(onOpen: (Uri) -> Unit, header: LazyGridScope.() -> Unit = {}) {
    val ctx = LocalContext.current
    var shots by remember { mutableStateOf<List<ImageSaver.Shot>>(emptyList()) }
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { tick++; onPauseOrDispose { } }
    LaunchedEffect(tick) { shots = withContext(Dispatchers.IO) { ImageSaver.list(ctx) } }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        header()
        if (shots.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Text(t("No screenshots yet.", "هنوز اسکرین‌شاتی نیست."), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(shots, key = { it.uri.toString() }) { s ->
            Thumb(s.uri, Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).clickable { onOpen(s.uri) })
        }
    }
}

@Composable
fun ShotStudioScreen(back: () -> Unit, onOpen: (Uri) -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs.get(ctx) }
    var delay by remember { mutableIntStateOf(prefs.shotDelay) }
    var fmt by remember { mutableStateOf(prefs.shotFormat) }
    var quality by remember { mutableFloatStateOf(prefs.shotQuality.toFloat()) }
    ScreenFrame(t("Screenshot Studio", "استودیو اسکرین‌شات"), back, scroll = false) {
        ShotGallery(onOpen) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionCard(t("Capture", "گرفتن تصویر")) {
                        Text(t("Delay", "تأخیر"))
                        ChipRow {
                            listOf(0, 3, 5, 10).forEach { d -> Chip(if (d == 0) t("None", "بدون") else "$d s", delay == d) { delay = d; prefs.shotDelay = d } }
                        }
                        Text(t("Format", "فرمت"))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip("PNG", fmt == "png") { fmt = "png"; prefs.shotFormat = "png" }
                            Chip("JPEG", fmt == "jpg") { fmt = "jpg"; prefs.shotFormat = "jpg" }
                        }
                        if (fmt == "jpg") SliderRow(t("JPEG quality", "کیفیت JPEG"), quality, 40f..100f, "${quality.toInt()}%", { prefs.shotQuality = quality.toInt() }) { quality = it }
                        GradButton(t("Take screenshot", "گرفتن اسکرین‌شات"), Modifier.fillMaxWidth()) { ScreenshotRequestActivity.launch(ctx, true) }
                        Text(
                            t("Android asks for permission each time. After you allow, the app goes to the home screen and captures after at least 3 seconds. You can also use the camera icon in the floating menu.",
                                "اندروید هر بار اجازه می‌خواهد. پس از تأیید، برنامه به صفحه اصلی می‌رود و حداقل پس از ۳ ثانیه تصویر می‌گیرد. می‌توانید از آیکون دوربین در منوی شناور هم استفاده کنید."),
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private class ToolOpt(val id: String, val icon: ImageVector)

@Composable
private fun IconToggle(icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp)) }
}

@Composable
fun EditorScreen(startUri: Uri, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs.get(ctx) }
    var uri by remember { mutableStateOf(startUri) }
    var base by remember { mutableStateOf<Bitmap?>(null) }
    val anns = remember { mutableStateListOf<Ann>() }
    val undo = remember { mutableStateListOf<Snap>() }
    val redo = remember { mutableStateListOf<Snap>() }
    var tool by remember { mutableStateOf("pen") }
    var color by remember { mutableIntStateOf(0xFFE53935.toInt()) }
    var strokeSel by remember { mutableIntStateOf(2) }
    var cur by remember { mutableStateOf<Ann?>(null) }
    var crop by remember { mutableStateOf<ShapeAnn?>(null) }
    var textPos by remember { mutableStateOf<PointF?>(null) }
    var dialog by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var pct by remember { mutableFloatStateOf(100f) }
    var info by remember { mutableStateOf<ImageSaver.Shot?>(null) }

    LaunchedEffect(startUri) { base = withContext(Dispatchers.IO) { ImageSaver.loadBitmap(ctx, startUri) } }
    LaunchedEffect(uri) { info = withContext(Dispatchers.IO) { ImageSaver.info(ctx, uri) } }

    fun pushUndo() {
        val b = base ?: return
        undo.add(Snap(b, anns.toList()))
        if (undo.size > 15) undo.removeAt(0)
        redo.clear()
    }
    fun doUndo() {
        val b = base ?: return
        if (undo.isEmpty()) return
        redo.add(Snap(b, anns.toList()))
        val s = undo.removeAt(undo.lastIndex)
        base = s.bmp; anns.clear(); anns.addAll(s.anns)
    }
    fun doRedo() {
        val b = base ?: return
        if (redo.isEmpty()) return
        undo.add(Snap(b, anns.toList()))
        val s = redo.removeAt(redo.lastIndex)
        base = s.bmp; anns.clear(); anns.addAll(s.anns)
    }
    fun flatten(): Bitmap? {
        val b = base ?: return null
        val out = b.copy(Bitmap.Config.ARGB_8888, true)
        val c = android.graphics.Canvas(out)
        anns.forEach { drawAnn(c, b, it) }
        return out
    }
    fun ensureSaved(): Uri? {
        val dirty = anns.isNotEmpty() || undo.isNotEmpty()
        if (!dirty) return uri
        val f = flatten() ?: return null
        val ext = if (prefs.shotFormat == "png") "png" else "jpg"
        val stem = (info?.name ?: "Screenshot").substringBeforeLast('.')
        val nu = ImageSaver.save(ctx, f, "${stem}_edited_${System.currentTimeMillis() % 100000}.$ext", prefs.shotFormat, prefs.shotQuality) ?: return null
        uri = nu; base = f; anns.clear(); undo.clear(); redo.clear(); crop = null
        return nu
    }
    fun toast(en: String, fa: String) = Toast.makeText(ctx, Lang.t(ctx, en, fa), Toast.LENGTH_SHORT).show()

    val colors = listOf(0xFFE53935, 0xFFFDD835, 0xFF43A047, 0xFF1E88E5, 0xFFFFFFFF, 0xFF000000).map { it.toInt() }
    val tools = listOf(
        ToolOpt("pen", Icons.Filled.Brush), ToolOpt("arrow", Icons.AutoMirrored.Filled.ArrowForward), ToolOpt("line", Icons.Filled.HorizontalRule),
        ToolOpt("rect", Icons.Filled.CropSquare), ToolOpt("circle", Icons.Filled.RadioButtonUnchecked), ToolOpt("text", Icons.Filled.TextFields),
        ToolOpt("pixel", Icons.Filled.GridOn), ToolOpt("blur", Icons.Filled.BlurOn), ToolOpt("crop", Icons.Filled.Crop)
    )

    ScreenFrame(t("Edit screenshot", "ویرایش اسکرین‌شات"), onBack, scroll = false, padding = 8.dp) {
        val b = base
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF222222)), contentAlignment = Alignment.Center) {
            if (b != null) {
                val sc = min(constraints.maxWidth.toFloat() / b.width, constraints.maxHeight.toFloat() / b.height)
                val wDp = with(LocalDensity.current) { (b.width * sc).toDp() }
                val hDp = with(LocalDensity.current) { (b.height * sc).toDp() }
                Canvas(
                    Modifier.size(wDp, hDp).pointerInput(tool, color, strokeSel, b) {
                        val f = b.width.toFloat() / size.width
                        val w = b.width / 180f * strokeSel
                        if (tool == "text") {
                            detectTapGestures { off -> textPos = PointF(off.x * f, off.y * f); input = ""; dialog = "text" }
                        } else {
                            detectDragGestures(
                                onDragStart = { o ->
                                    val s = PointF(o.x * f, o.y * f)
                                    cur = if (tool == "pen") StrokeAnn(listOf(s), color, w) else ShapeAnn(tool, s, s, color, w)
                                },
                                onDrag = { ch, _ ->
                                    ch.consume()
                                    val p = PointF(ch.position.x * f, ch.position.y * f)
                                    cur = when (val c = cur) {
                                        is StrokeAnn -> c.copy(pts = c.pts + p)
                                        is ShapeAnn -> c.copy(b = p)
                                        else -> c
                                    }
                                },
                                onDragEnd = {
                                    val c = cur
                                    cur = null
                                    if (c is ShapeAnn && c.kind == "crop") crop = c
                                    else if (c != null) { pushUndo(); anns.add(c) }
                                },
                                onDragCancel = { cur = null }
                            )
                        }
                    }
                ) {
                    drawIntoCanvas { cv ->
                        val n = cv.nativeCanvas
                        n.save()
                        val s = size.width / b.width
                        n.scale(s, s)
                        n.drawBitmap(b, 0f, 0f, null)
                        anns.forEach { drawAnn(n, b, it) }
                        cur?.let { drawAnn(n, b, it) }
                        crop?.let { drawAnn(n, b, it) }
                        n.restore()
                    }
                }
            } else Text("…", color = Color.White)
        }

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tools.forEach { o -> IconToggle(o.icon, tool == o.id) { tool = o.id } }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            colors.forEach { c ->
                Box(
                    Modifier.size(if (c == color) 34.dp else 28.dp).clip(CircleShape).background(Color(c))
                        .clickable { color = c }
                )
            }
            listOf(1 to "S", 2 to "M", 4 to "L").forEach { (v, l) -> Chip(l, strokeSel == v) { strokeSel = v } }
        }
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            IconButton(onClick = { doUndo() }, enabled = undo.isNotEmpty()) { Icon(Icons.AutoMirrored.Filled.Undo, null) }
            IconButton(onClick = { doRedo() }, enabled = redo.isNotEmpty()) { Icon(Icons.AutoMirrored.Filled.Redo, null) }
            IconButton(onClick = {
                val f = flatten()
                if (f != null) {
                    pushUndo()
                    base = Bitmap.createBitmap(f, 0, 0, f.width, f.height, Matrix().apply { postRotate(90f) }, true)
                    anns.clear(); crop = null
                }
            }) { Icon(Icons.Filled.RotateRight, null) }
            IconButton(onClick = { pct = 100f; dialog = "resize" }) { Icon(Icons.Filled.AspectRatio, null) }
            if (crop != null) IconButton(onClick = {
                val f = flatten(); val cr = crop
                if (f != null && cr != null) {
                    val l = min(cr.a.x, cr.b.x).toInt().coerceIn(0, f.width - 1)
                    val t0 = min(cr.a.y, cr.b.y).toInt().coerceIn(0, f.height - 1)
                    val r = max(cr.a.x, cr.b.x).toInt().coerceIn(l + 1, f.width)
                    val bt = max(cr.a.y, cr.b.y).toInt().coerceIn(t0 + 1, f.height)
                    pushUndo()
                    base = Bitmap.createBitmap(f, l, t0, r - l, bt - t0)
                    anns.clear(); crop = null
                }
            }) { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = { if (ensureSaved() != null) toast("Saved", "ذخیره شد") else toast("Save failed", "ذخیره ناموفق بود") }) { Icon(Icons.Filled.Save, null) }
            IconButton(onClick = {
                ensureSaved()?.let { u ->
                    ctx.safeStart(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("image/*").putExtra(Intent.EXTRA_STREAM, u).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null))
                }
            }) { Icon(Icons.Filled.Share, null) }
            IconButton(onClick = {
                ensureSaved()?.let { u ->
                    ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newUri(ctx.contentResolver, "image", u))
                    toast("Image copied", "تصویر کپی شد")
                }
            }) { Icon(Icons.Filled.ContentCopy, null) }
            IconButton(onClick = { dialog = "info" }) { Icon(Icons.Filled.Info, null) }
            IconButton(onClick = { input = (info?.name ?: "").substringBeforeLast('.'); dialog = "rename" }) { Icon(Icons.Filled.Edit, null) }
            IconButton(onClick = { dialog = "delete" }) { Icon(Icons.Filled.Delete, null, tint = Color(0xFFE53935)) }
        }
    }

    when (dialog) {
        "text" -> AlertDialog(
            onDismissRequest = { dialog = "" },
            title = { Text(t("Add text", "افزودن متن")) },
            text = { OutlinedTextField(input, { input = it }) },
            confirmButton = {
                TextButton(onClick = {
                    val p = textPos; val bb = base
                    if (p != null && bb != null && input.isNotBlank()) { pushUndo(); anns.add(TextAnn(input, p, color, bb.width / 18f * (0.5f + strokeSel * 0.25f))) }
                    dialog = ""
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { dialog = "" }) { Text(t("Cancel", "انصراف")) } }
        )
        "resize" -> AlertDialog(
            onDismissRequest = { dialog = "" },
            title = { Text(t("Resize", "تغییر اندازه")) },
            text = {
                Column {
                    val bb = base
                    Text("${pct.toInt()}%" + if (bb != null) "  (${(bb.width * pct / 100).toInt()}×${(bb.height * pct / 100).toInt()})" else "")
                    Slider(pct, { pct = it }, valueRange = 25f..200f)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val f = flatten()
                    if (f != null) {
                        pushUndo()
                        base = Bitmap.createScaledBitmap(f, (f.width * pct / 100).toInt().coerceAtLeast(1), (f.height * pct / 100).toInt().coerceAtLeast(1), true)
                        anns.clear(); crop = null
                    }
                    dialog = ""
                }) { Text(t("Apply", "اعمال")) }
            },
            dismissButton = { TextButton(onClick = { dialog = "" }) { Text(t("Cancel", "انصراف")) } }
        )
        "rename" -> AlertDialog(
            onDismissRequest = { dialog = "" },
            title = { Text(t("Rename", "تغییر نام")) },
            text = { OutlinedTextField(input, { input = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    val ext = (info?.name ?: "x.png").substringAfterLast('.', "png")
                    if (input.isNotBlank() && ImageSaver.rename(ctx, uri, "${input.trim()}.$ext")) {
                        info = ImageSaver.info(ctx, uri)
                    } else toast("Rename failed", "تغییر نام ناموفق بود")
                    dialog = ""
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { dialog = "" }) { Text(t("Cancel", "انصراف")) } }
        )
        "delete" -> AlertDialog(
            onDismissRequest = { dialog = "" },
            title = { Text(t("Delete screenshot?", "اسکرین‌شات حذف شود؟")) },
            confirmButton = {
                TextButton(onClick = {
                    dialog = ""
                    if (ImageSaver.delete(ctx, uri)) onBack() else toast("Could not delete", "حذف ممکن نشد")
                }) { Text(t("Delete", "حذف")) }
            },
            dismissButton = { TextButton(onClick = { dialog = "" }) { Text(t("Cancel", "انصراف")) } }
        )
        "info" -> AlertDialog(
            onDismissRequest = { dialog = "" },
            title = { Text(t("Image info", "اطلاعات تصویر")) },
            text = {
                val bb = base
                Text(
                    (info?.name ?: "") + "\n" +
                        t("Resolution: ", "وضوح: ") + (if (bb != null) "${bb.width} × ${bb.height}" else "—") + "\n" +
                        t("File size: ", "حجم فایل: ") + Formatter.formatFileSize(ctx, info?.size ?: 0L)
                )
            },
            confirmButton = { TextButton(onClick = { dialog = "" }) { Text("OK") } }
        )
    }
}
