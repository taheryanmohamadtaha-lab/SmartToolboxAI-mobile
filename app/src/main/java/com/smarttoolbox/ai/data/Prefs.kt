package com.smarttoolbox.ai.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.graphics.ColorUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class Prefs private constructor(ctx: Context) {
    val sp: SharedPreferences = ctx.applicationContext.getSharedPreferences("toolbox_prefs", Context.MODE_PRIVATE)

    companion object {
        @Volatile private var inst: Prefs? = null
        fun get(ctx: Context): Prefs = inst ?: synchronized(this) { inst ?: Prefs(ctx).also { inst = it } }
        const val DEFAULT_MENU = "home,shot,calc,notes,torch,lock,recents,settings"
        val DEFAULT_ACCENT: Int = 0xFF8E24AA.toInt()
    }

    private fun put(k: String, v: Any) {
        val e = sp.edit()
        when (v) {
            is Int -> e.putInt(k, v)
            is Boolean -> e.putBoolean(k, v)
            is Float -> e.putFloat(k, v)
            is String -> e.putString(k, v)
            else -> {}
        }
        e.apply()
    }

    var floatSize: Int get() = sp.getInt("float_size", 56); set(v) = put("float_size", v)
    var floatAlpha: Float get() = sp.getFloat("float_alpha", 0.92f); set(v) = put("float_alpha", v)
    var floatColor: Int get() = sp.getInt("float_color", DEFAULT_ACCENT); set(v) = put("float_color", v)
    var floatIcon: Int get() = sp.getInt("float_icon", 0); set(v) = put("float_icon", v)
    var floatSide: Int get() = sp.getInt("float_side", 1); set(v) = put("float_side", v)
    var floatX: Int get() = sp.getInt("float_x", -1); set(v) = put("float_x", v)
    var floatY: Int get() = sp.getInt("float_y", 300); set(v) = put("float_y", v)
    var snapEdge: Boolean get() = sp.getBoolean("snap", true); set(v) = put("snap", v)
    var autoStart: Boolean get() = sp.getBoolean("auto_start", false); set(v) = put("auto_start", v)
    var haptic: Boolean get() = sp.getBoolean("haptic", true); set(v) = put("haptic", v)
    var menuItems: String get() = sp.getString("menu_items", DEFAULT_MENU) ?: DEFAULT_MENU; set(v) = put("menu_items", v)
    var themeMode: Int get() = sp.getInt("theme_mode", 0); set(v) = put("theme_mode", v)
    var appAccent: Int get() = sp.getInt("app_accent", DEFAULT_ACCENT); set(v) = put("app_accent", v)
    var lang: String get() = sp.getString("lang", "system") ?: "system"; set(v) = put("lang", v)
    var shotFormat: String get() = sp.getString("shot_format", "png") ?: "png"; set(v) = put("shot_format", v)
    var shotQuality: Int get() = sp.getInt("shot_quality", 92); set(v) = put("shot_quality", v)
    var shotFolder: String get() = sp.getString("shot_folder", "SmartToolbox") ?: "SmartToolbox"; set(v) = put("shot_folder", v)
    var shotSound: Boolean get() = sp.getBoolean("shot_sound", true); set(v) = put("shot_sound", v)
    var shotNotif: Boolean get() = sp.getBoolean("shot_notif", true); set(v) = put("shot_notif", v)
    var shotDelay: Int get() = sp.getInt("shot_delay", 0); set(v) = put("shot_delay", v)
    var calcDark: Boolean get() = sp.getBoolean("calc_dark", false); set(v) = put("calc_dark", v)
    var noteAlpha: Float get() = sp.getFloat("note_alpha", 0.92f); set(v) = put("note_alpha", v)

    fun jsonArray(key: String): JSONArray = try { JSONArray(sp.getString(key, "[]")) } catch (e: Exception) { JSONArray() }
    fun putJson(key: String, a: JSONArray) = put(key, a.toString())

    fun exportJson(): String {
        val o = JSONObject()
        for ((k, v) in sp.all) {
            val e = JSONObject()
            when (v) {
                is Int -> e.put("t", "i").put("v", v)
                is Boolean -> e.put("t", "b").put("v", v)
                is Float -> e.put("t", "f").put("v", v.toDouble())
                is Long -> e.put("t", "l").put("v", v)
                is String -> e.put("t", "s").put("v", v)
                else -> continue
            }
            o.put(k, e)
        }
        return o.toString()
    }

    fun importJson(s: String): Boolean = try {
        val o = JSONObject(s)
        val ed = sp.edit()
        for (k in o.keys()) {
            val e = o.getJSONObject(k)
            when (e.getString("t")) {
                "i" -> ed.putInt(k, e.getInt("v"))
                "b" -> ed.putBoolean(k, e.getBoolean("v"))
                "f" -> ed.putFloat(k, e.getDouble("v").toFloat())
                "l" -> ed.putLong(k, e.getLong("v"))
                "s" -> ed.putString(k, e.getString("v"))
            }
        }
        ed.apply()
        true
    } catch (e: Exception) { false }

    fun reset() { sp.edit().clear().apply() }
}

object Colors {
    val palette: List<Int> = listOf(
        0xFF8E24AAL, 0xFF651FFFL, 0xFF1E88E5L, 0xFF00ACC1L, 0xFF00897BL,
        0xFF43A047L, 0xFFFB8C00L, 0xFFE53935L, 0xFFD81B60L, 0xFF3949ABL
    ).map { it.toInt() }

    fun dark(c: Int): Int {
        val h = FloatArray(3); ColorUtils.colorToHSL(c, h)
        h[2] = (h[2] * 0.78f).coerceIn(0f, 1f)
        return ColorUtils.HSLToColor(h)
    }

    fun light(c: Int): Int {
        val h = FloatArray(3); ColorUtils.colorToHSL(c, h)
        h[0] = (h[0] + 28f) % 360f
        h[2] = (h[2] * 1.12f + 0.03f).coerceIn(0f, 0.9f)
        return ColorUtils.HSLToColor(h)
    }
}

object Lang {
    fun isFa(ctx: Context): Boolean {
        val l = Prefs.get(ctx).lang
        return l == "fa" || (l == "system" && Locale.getDefault().language == "fa")
    }
    fun t(ctx: Context, en: String, fa: String): String = if (isFa(ctx)) fa else en
}
