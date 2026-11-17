package com.smarttoolbox.ai.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.smarttoolbox.ai.core.Tools
import com.smarttoolbox.ai.data.Colors
import com.smarttoolbox.ai.data.Prefs
import com.smarttoolbox.ai.floating.FloatingService
import com.smarttoolbox.ai.floating.ServiceState

@Composable
fun FloatingScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs.get(ctx) }
    var size by remember { mutableFloatStateOf(prefs.floatSize.toFloat()) }
    var alpha by remember { mutableFloatStateOf(prefs.floatAlpha) }
    var color by remember { mutableIntStateOf(prefs.floatColor) }
    var icon by remember { mutableIntStateOf(prefs.floatIcon) }
    var side by remember { mutableIntStateOf(prefs.floatSide) }
    var snap by remember { mutableStateOf(prefs.snapEdge) }
    var auto by remember { mutableStateOf(prefs.autoStart) }
    var menu by remember { mutableStateOf(prefs.menuItems.split(",").filter { it.isNotBlank() }.toSet()) }
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { tick++; onPauseOrDispose { } }
    val canOverlay = remember(tick) { Settings.canDrawOverlays(ctx) }

    fun refresh() { FloatingService.refresh(ctx) }

    ScreenFrame(t("Floating Assistant", "دستیار شناور"), back) {
        if (!canOverlay) SectionCard(t("Permission needed", "مجوز لازم است")) {
            Text(t("Allow \"Display over other apps\" so the button can appear above other apps.", "برای نمایش دکمه روی برنامه‌های دیگر، مجوز «نمایش روی سایر برنامه‌ها» را بدهید."))
            GradButton(t("Grant permission", "دادن مجوز")) {
                ctx.safeStart(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
            }
        }
        SectionCard(t("Appearance", "ظاهر")) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(size.dp).alpha(alpha).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(Colors.dark(color)), Color(Colors.light(color))))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(Tools.floatIcons[icon.coerceIn(0, Tools.floatIcons.size - 1)]), null, tint = Color.White, modifier = Modifier.size((size * 0.5f).dp))
                }
            }
            SliderRow(t("Button size", "اندازه دکمه"), size, 36f..96f, "${size.toInt()} dp", { prefs.floatSize = size.toInt(); refresh() }) { size = it }
            SliderRow(t("Transparency", "شفافیت"), alpha, 0.25f..1f, "${(alpha * 100).toInt()}%", { prefs.floatAlpha = alpha; refresh() }) { alpha = it }
            Text(t("Color", "رنگ"))
            ColorRow(color) { color = it; prefs.floatColor = it; refresh() }
            Text(t("Icon", "آیکون"))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tools.floatIcons.forEachIndexed { i, res ->
                    Box(
                        Modifier.size(46.dp).clip(CircleShape)
                            .background(if (i == icon) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { icon = i; prefs.floatIcon = i; refresh() },
                        contentAlignment = Alignment.Center
                    ) { Icon(painterResource(res), null, tint = if (i == icon) Color.White else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp)) }
                }
            }
        }
        SectionCard(t("Position and behavior", "موقعیت و رفتار")) {
            Text(t("Side", "سمت"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(t("Left", "چپ"), side == 0) { side = 0; prefs.floatSide = 0; prefs.floatX = 0; refresh() }
                Chip(t("Right", "راست"), side == 1) { side = 1; prefs.floatSide = 1; prefs.floatX = -1; refresh() }
            }
            SwitchRow(t("Snap to screen edge", "چسبیدن به لبه صفحه"), snap) { snap = it; prefs.snapEdge = it }
            SwitchRow(t("Start after reboot", "شروع پس از ری‌استارت"), auto, t("Works where Android allows it", "در صورت اجازه اندروید")) {
                if (it && !Settings.canDrawOverlays(ctx)) {
                    Toast.makeText(ctx, t0(ctx, "Grant the overlay permission first", "ابتدا مجوز نمایش روی برنامه‌ها را بدهید"), Toast.LENGTH_SHORT).show()
                } else { auto = it; prefs.autoStart = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GradButton(t("Hide / show", "مخفی / نمایش")) { if (ServiceState.running.value) FloatingService.start(ctx, FloatingService.ACTION_TOGGLE_HIDE) }
            }
        }
        SectionCard(t("Menu tools (max 12)", "ابزارهای منو (حداکثر ۱۲)")) {
            Tools.all.forEach { tool ->
                Row(
                    Modifier.fillMaxWidth().clickable {
                        val n = if (tool.id in menu) menu - tool.id else menu + tool.id
                        if (n.size <= 12) { menu = n; prefs.menuItems = Tools.all.map { it.id }.filter { it in n }.joinToString(","); refresh() }
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = tool.id in menu, onCheckedChange = null)
                    Spacer(Modifier.width(8.dp))
                    Icon(painterResource(tool.icon), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(t(tool.en, tool.fa))
                }
            }
            Text(t("Custom shortcuts can be added to the menu from the Custom Shortcuts screen.", "میانبرهای سفارشی را از صفحه «میانبرهای سفارشی» به منو اضافه کنید."), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun t0(ctx: android.content.Context, en: String, fa: String) = com.smarttoolbox.ai.data.Lang.t(ctx, en, fa)
