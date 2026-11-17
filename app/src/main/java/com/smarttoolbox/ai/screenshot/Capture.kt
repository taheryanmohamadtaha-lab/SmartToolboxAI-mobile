package com.smarttoolbox.ai.screenshot

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.MediaActionSound
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.smarttoolbox.ai.MainActivity
import com.smarttoolbox.ai.R
import com.smarttoolbox.ai.ToolboxApp
import com.smarttoolbox.ai.data.ImageSaver
import com.smarttoolbox.ai.data.Lang
import com.smarttoolbox.ai.data.Prefs
import com.smarttoolbox.ai.floating.FloatingService

/** Asks the user for MediaProjection consent (required for every capture session) and starts [CaptureService]. */
class ScreenshotRequestActivity : ComponentActivity() {
    companion object {
        fun launch(ctx: Context, goHome: Boolean) {
            ctx.startActivity(
                Intent(ctx, ScreenshotRequestActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("go_home", goHome)
            )
        }
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val data = r.data
        if (r.resultCode == RESULT_OK && data != null) {
            val goHome = intent.getBooleanExtra("go_home", false)
            val prefs = Prefs.get(this)
            val delay = if (goHome) maxOf(prefs.shotDelay, 3) else prefs.shotDelay
            ContextCompat.startForegroundService(
                this, Intent(this, CaptureService::class.java).putExtra("code", r.resultCode).putExtra("data", data).putExtra("delay", delay)
            )
            if (goHome) startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            Toast.makeText(this, Lang.t(this, "Screen capture permission was not granted", "اجازه گرفتن تصویر صفحه داده نشد"), Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        val mpm = getSystemService(MediaProjectionManager::class.java)
        launcher.launch(mpm.createScreenCaptureIntent())
    }
}

class CaptureService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var projection: MediaProjection? = null
    private var vd: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var finished = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val n = NotificationCompat.Builder(this, ToolboxApp.CH_CAPTURE)
            .setSmallIcon(R.drawable.ic_camera)
            .setContentTitle(Lang.t(this, "Capturing screen…", "در حال گرفتن اسکرین‌شات…"))
            .setOngoing(true).build()
        startForeground(2, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        val code = intent?.getIntExtra("code", 0) ?: 0
        val data = intent?.let { IntentCompat.getParcelableExtra(it, "data", Intent::class.java) }
        val delay = intent?.getIntExtra("delay", 0) ?: 0
        if (data == null) { finish(); return START_NOT_STICKY }
        try { begin(code, data, delay) } catch (e: Exception) {
            Toast.makeText(this, Lang.t(this, "Could not start capture", "شروع گرفتن تصویر ممکن نشد"), Toast.LENGTH_SHORT).show()
            finish()
        }
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun metrics(): Triple<Int, Int, Int> {
        val wm = getSystemService(WindowManager::class.java)
        return if (Build.VERSION.SDK_INT >= 30) {
            val b = wm.maximumWindowMetrics.bounds
            Triple(b.width(), b.height(), resources.configuration.densityDpi)
        } else {
            val dm = DisplayMetrics()
            wm.defaultDisplay.getRealMetrics(dm)
            Triple(dm.widthPixels, dm.heightPixels, dm.densityDpi)
        }
    }

    private fun begin(code: Int, data: Intent, delay: Int) {
        val mgr = getSystemService(MediaProjectionManager::class.java)
        val p = mgr.getMediaProjection(code, data)
        projection = p
        p.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() { handler.post { finish() } }
        }, handler)
        val (w, h, dpi) = metrics()
        val r = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        reader = r
        vd = p.createVirtualDisplay("toolbox-shot", w, h, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, r.surface, null, handler)
        FloatingService.instance?.setTemporarilyHidden(true)
        handler.postDelayed({ grab(r, w, h, 0) }, 700L + delay * 1000L)
    }

    private fun grab(r: ImageReader, w: Int, h: Int, attempt: Int) {
        if (finished) return
        val img = try { r.acquireLatestImage() } catch (e: Exception) { null }
        if (img == null) {
            if (attempt < 12) handler.postDelayed({ grab(r, w, h, attempt + 1) }, 150)
            else {
                Toast.makeText(this, Lang.t(this, "Capture failed", "گرفتن تصویر ناموفق بود"), Toast.LENGTH_SHORT).show()
                finish()
            }
            return
        }
        val bmp: Bitmap? = try {
            val pl = img.planes[0]
            val rowPad = pl.rowStride - pl.pixelStride * w
            val tmp = Bitmap.createBitmap(w + rowPad / pl.pixelStride, h, Bitmap.Config.ARGB_8888)
            tmp.copyPixelsFromBuffer(pl.buffer)
            Bitmap.createBitmap(tmp, 0, 0, w, h)
        } catch (e: Exception) { null } finally { img.close() }
        FloatingService.instance?.setTemporarilyHidden(false)
        if (bmp == null) { finish(); return }
        val prefs = Prefs.get(this)
        Thread {
            val uri = ImageSaver.save(this, bmp, null, prefs.shotFormat, prefs.shotQuality)
            handler.post { onSaved(uri, bmp); finish() }
        }.start()
    }

    @SuppressLint("MissingPermission")
    private fun onSaved(uri: Uri?, bmp: Bitmap) {
        val prefs = Prefs.get(this)
        if (uri == null) {
            Toast.makeText(this, Lang.t(this, "Could not save screenshot", "ذخیره اسکرین‌شات ممکن نشد"), Toast.LENGTH_SHORT).show()
            return
        }
        if (prefs.shotSound) try { MediaActionSound().play(MediaActionSound.SHUTTER_CLICK) } catch (e: Exception) {}
        Toast.makeText(this, Lang.t(this, "Screenshot saved", "اسکرین‌شات ذخیره شد"), Toast.LENGTH_SHORT).show()
        if (prefs.shotNotif) {
            val open = PendingIntent.getActivity(
                this, 7, Intent(this, MainActivity::class.java).putExtra("edit_uri", uri.toString()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val small = Bitmap.createScaledBitmap(bmp, bmp.width / 4, bmp.height / 4, true)
            val n = NotificationCompat.Builder(this, ToolboxApp.CH_SHOT)
                .setSmallIcon(R.drawable.ic_camera)
                .setContentTitle(Lang.t(this, "Screenshot saved", "اسکرین‌شات ذخیره شد"))
                .setContentText(Lang.t(this, "Tap to edit", "برای ویرایش بزنید"))
                .setStyle(NotificationCompat.BigPictureStyle().bigPicture(small))
                .setContentIntent(open).setAutoCancel(true).build()
            try { NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), n) } catch (e: SecurityException) {}
        }
    }

    private fun finish() {
        if (finished) return
        finished = true
        FloatingService.instance?.setTemporarilyHidden(false)
        try { vd?.release() } catch (e: Exception) {}
        try { reader?.close() } catch (e: Exception) {}
        try { projection?.stop() } catch (e: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() { finish(); super.onDestroy() }
}
