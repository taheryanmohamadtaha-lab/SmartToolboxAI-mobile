package com.smarttoolbox.ai.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageSaver {
    data class Shot(val uri: Uri, val name: String, val size: Long, val width: Int, val height: Int, val date: Long)

    fun folder(ctx: Context): String = "Pictures/" + Prefs.get(ctx).shotFolder.trim().ifBlank { "SmartToolbox" }

    fun nameNow(ext: String, prefix: String = "Screenshot"): String =
        prefix + "_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + "." + ext

    fun save(ctx: Context, bmp: Bitmap, displayName: String?, fmt: String, quality: Int, relFolder: String = folder(ctx)): Uri? {
        val png = fmt == "png"
        val name = displayName ?: nameNow(if (png) "png" else "jpg")
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, if (png) "image/png" else "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relFolder)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val cr = ctx.contentResolver
        val uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            cr.openOutputStream(uri)!!.use { bmp.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, quality, it) }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            cr.update(uri, values, null, null)
            uri
        } catch (e: Exception) {
            cr.delete(uri, null, null)
            null
        }
    }

    private val proj = arrayOf(
        MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.WIDTH, MediaStore.Images.Media.HEIGHT, MediaStore.Images.Media.DATE_ADDED
    )

    fun list(ctx: Context): List<Shot> {
        val out = ArrayList<Shot>()
        ctx.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj,
            "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?", arrayOf(folder(ctx) + "%"),
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { c ->
            while (c.moveToNext()) {
                out.add(Shot(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, c.getLong(0)), c.getString(1) ?: "", c.getLong(2), c.getInt(3), c.getInt(4), c.getLong(5)))
            }
        }
        return out
    }

    fun info(ctx: Context, uri: Uri): Shot? {
        ctx.contentResolver.query(uri, proj, null, null, null)?.use { c ->
            if (c.moveToFirst()) return Shot(uri, c.getString(1) ?: "", c.getLong(2), c.getInt(3), c.getInt(4), c.getLong(5))
        }
        return null
    }

    fun rename(ctx: Context, uri: Uri, newName: String): Boolean = try {
        val v = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, newName) }
        ctx.contentResolver.update(uri, v, null, null) > 0
    } catch (e: Exception) { false }

    fun delete(ctx: Context, uri: Uri): Boolean = try { ctx.contentResolver.delete(uri, null, null) > 0 } catch (e: Exception) { false }

    fun loadBitmap(ctx: Context, uri: Uri): Bitmap? = try {
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) { null }
}
