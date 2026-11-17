package com.smarttoolbox.ai.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color as AColor
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.smarttoolbox.ai.data.ImageSaver
import com.smarttoolbox.ai.data.Lang

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun bindCamera(ctx: Context, owner: LifecycleOwner, pv: PreviewView, onCode: (String) -> Unit) {
    val fut = ProcessCameraProvider.getInstance(ctx)
    fut.addListener({
        val provider = fut.get()
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
        val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build())
        val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
        var done = false
        analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { proxy ->
            val media = proxy.image
            if (media != null && !done) {
                val img = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                scanner.process(img)
                    .addOnSuccessListener { codes ->
                        val v = codes.firstOrNull()?.rawValue
                        if (v != null && !done) { done = true; onCode(v) }
                    }
                    .addOnCompleteListener { proxy.close() }
            } else proxy.close()
        }
        try {
            provider.unbindAll()
            provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        } catch (e: Exception) {}
    }, ContextCompat.getMainExecutor(ctx))
}

@Composable
fun QrScanScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var result by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val scanner = BarcodeScanning.getClient()
                scanner.process(InputImage.fromFilePath(ctx, uri))
                    .addOnSuccessListener { codes ->
                        val v = codes.firstOrNull()?.rawValue
                        if (v != null) result = v else Toast.makeText(ctx, Lang.t(ctx, "No code found in the image", "کدی در تصویر پیدا نشد"), Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {}
        }
    }
    ScreenFrame(t("QR / barcode scanner", "اسکنر QR / بارکد"), back) {
        val r = result
        if (r != null) {
            SectionCard(t("Result", "نتیجه")) {
                Text(r)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradButton(t("Copy", "کپی")) {
                        ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("qr", r))
                        Toast.makeText(ctx, Lang.t(ctx, "Copied", "کپی شد"), Toast.LENGTH_SHORT).show()
                    }
                    if (r.startsWith("http://") || r.startsWith("https://")) GradButton(t("Open", "باز کردن")) { ctx.safeStart(Intent(Intent.ACTION_VIEW, Uri.parse(r))) }
                    GradButton(t("Scan again", "اسکن مجدد")) { result = null }
                }
            }
        } else if (!granted) {
            SectionCard(t("Camera permission", "مجوز دوربین")) {
                Text(t("The camera is used only to read QR codes on your device. No image leaves your phone.", "دوربین فقط برای خواندن کد QR روی دستگاه شما استفاده می‌شود. هیچ تصویری از گوشی خارج نمی‌شود."))
                GradButton(t("Allow camera", "مجاز کردن دوربین")) { perm.launch(Manifest.permission.CAMERA) }
            }
        } else {
            AndroidView(
                factory = { c -> PreviewView(c).also { pv -> bindCamera(c, owner, pv) { result = it } } },
                modifier = Modifier.fillMaxWidth().height(380.dp).clip(RoundedCornerShape(20.dp))
            )
        }
        GradButton(t("Scan from an image", "اسکن از تصویر"), Modifier.fillMaxWidth()) { picker.launch("image/*") }
    }
}

private fun esc(s: String) = s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace(":", "\\:").replace("\"", "\\\"")

fun makeQr(text: String, size: Int = 768): Bitmap {
    val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8", EncodeHintType.MARGIN to 1)
    val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
    val px = IntArray(size * size)
    for (y in 0 until size) for (x in 0 until size) px[y * size + x] = if (m.get(x, y)) AColor.BLACK else AColor.WHITE
    return Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888)
}

@Composable
fun QrGenScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    var type by remember { mutableStateOf("text") }
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    var c by remember { mutableStateOf("") }
    var wifiSec by remember { mutableStateOf("WPA") }
    var bmp by remember { mutableStateOf<Bitmap?>(null) }

    fun payload(): String = when (type) {
        "url" -> if (a.contains("://")) a.trim() else "https://" + a.trim()
        "wifi" -> "WIFI:T:$wifiSec;S:${esc(a)};P:${esc(b)};;"
        "contact" -> "BEGIN:VCARD\nVERSION:3.0\nN:;$a;;;\nFN:$a\nTEL:$b\nEMAIL:$c\nEND:VCARD"
        else -> a
    }
    fun save(): Uri? {
        val bm = bmp ?: return null
        return ImageSaver.save(ctx, bm, ImageSaver.nameNow("png", "QR"), "png", 100, ImageSaver.folder(ctx) + "/QR")
    }

    ScreenFrame(t("Generate QR code", "ساخت کد QR"), back) {
        SectionCard {
            ChipRow {
                Chip(t("Text", "متن"), type == "text") { type = "text"; bmp = null }
                Chip("URL", type == "url") { type = "url"; bmp = null }
                Chip("Wi-Fi", type == "wifi") { type = "wifi"; bmp = null }
                Chip(t("Contact", "مخاطب"), type == "contact") { type = "contact"; bmp = null }
            }
            when (type) {
                "wifi" -> {
                    OutlinedTextField(a, { a = it }, Modifier.fillMaxWidth(), label = { Text(t("Network name (SSID)", "نام شبکه")) })
                    OutlinedTextField(b, { b = it }, Modifier.fillMaxWidth(), label = { Text(t("Password", "رمز")) })
                    ChipRow { listOf("WPA", "WEP", "nopass").forEach { s -> Chip(s, wifiSec == s) { wifiSec = s } } }
                }
                "contact" -> {
                    OutlinedTextField(a, { a = it }, Modifier.fillMaxWidth(), label = { Text(t("Name", "نام")) })
                    OutlinedTextField(b, { b = it }, Modifier.fillMaxWidth(), label = { Text(t("Phone", "تلفن")) })
                    OutlinedTextField(c, { c = it }, Modifier.fillMaxWidth(), label = { Text("Email") })
                }
                else -> OutlinedTextField(a, { a = it }, Modifier.fillMaxWidth(), label = { Text(if (type == "url") "URL" else t("Text", "متن")) })
            }
            GradButton(t("Generate", "ساخت"), Modifier.fillMaxWidth()) {
                if (a.isNotBlank()) bmp = try { makeQr(payload()) } catch (e: Exception) { null }
            }
        }
        bmp?.let { bm ->
            SectionCard {
                Image(bm.asImageBitmap(), null, Modifier.fillMaxWidth().height(300.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradButton(t("Save", "ذخیره")) {
                        val u = save()
                        Toast.makeText(ctx, if (u != null) Lang.t(ctx, "Saved to Pictures", "در گالری ذخیره شد") else Lang.t(ctx, "Save failed", "ذخیره ناموفق بود"), Toast.LENGTH_SHORT).show()
                    }
                    GradButton(t("Share", "اشتراک")) {
                        save()?.let { u -> ctx.safeStart(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, u).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null)) }
                    }
                }
            }
        }
    }
}
