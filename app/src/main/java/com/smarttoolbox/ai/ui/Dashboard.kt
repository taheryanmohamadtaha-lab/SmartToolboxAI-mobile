package com.smarttoolbox.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Sec(val route: String, val icon: ImageVector, val en: String, val fa: String, val dEn: String, val dFa: String)

private val sections = listOf(
    Sec("floating", Icons.Filled.Layers, "Floating Assistant", "دستیار شناور", "Button, colors, menu", "دکمه، رنگ، منو"),
    Sec("shot", Icons.Filled.CameraAlt, "Screenshot Studio", "استودیو اسکرین‌شات", "Capture and edit", "گرفتن و ویرایش"),
    Sec("tools", Icons.Filled.Build, "Mini Tools", "ابزارهای کوچک", "Quick system tools", "ابزارهای سریع سیستم"),
    Sec("prod", Icons.Filled.Dashboard, "Productivity", "بهره‌وری", "Notes, QR, timers", "یادداشت، QR، تایمر"),
    Sec("sys", Icons.Filled.PhoneAndroid, "System Information", "اطلاعات سیستم", "Battery, storage, RAM", "باتری، حافظه، رم"),
    Sec("shortcuts", Icons.Filled.Apps, "Custom Shortcuts", "میانبرهای سفارشی", "Apps and links", "برنامه‌ها و لینک‌ها"),
    Sec("history", Icons.Filled.History, "History", "تاریخچه", "Shots, clipboard, math", "تصاویر، کلیپ‌بورد، محاسبات"),
    Sec("settings", Icons.Filled.Settings, "Settings", "تنظیمات", "Theme, language, backup", "تم، زبان، پشتیبان")
)

@Composable
fun DashboardScreen(running: Boolean, onToggle: (Boolean) -> Unit, go: (String) -> Unit) {
    val brush = accentBrush()
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(brush), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Build, null, tint = Color.White)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(t("Floating", "شناور"), fontSize = 12.sp, letterSpacing = 3.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(t("TOOLBOX", "جعبه‌ابزار"), fontWeight = FontWeight.Black, fontSize = 26.sp)
            }
            IconButton(onClick = { go("settings") }) { Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.primary) }
        }

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(brush).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MoreVert, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(t("Quick Assistive Touch Toolbox", "جعبه‌ابزار لمسی سریع"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(t("Turn on this service to manage your tools", "برای مدیریت ابزارها این سرویس را روشن کنید"), color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                }
                Text(if (running) "ON" else "OFF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.width(6.dp))
                Switch(
                    checked = running, onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = Color.White.copy(alpha = 0.35f), checkedThumbColor = Color.White, uncheckedTrackColor = Color.White.copy(alpha = 0.2f), uncheckedThumbColor = Color.White)
                )
            }
            Spacer(Modifier.size(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Tile(Icons.Filled.Apps, t("Manage Apps", "برنامه‌ها")) { go("shortcuts") }
                Tile(Icons.Filled.Layers, t("Interface", "رابط")) { go("floating") }
                Tile(Icons.Filled.Settings, t("Settings", "تنظیمات")) { go("settings") }
            }
        }

        sections.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { s -> SectionTile(s, Modifier.weight(1f)) { go(s.route) } }
            }
        }
    }
}

@Composable
private fun Tile(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(Modifier.size(62.dp).clip(RoundedCornerShape(18.dp)).background(Color.White), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.size(6.dp))
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
private fun SectionTile(s: Sec, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(accentBrush()), contentAlignment = Alignment.Center) {
                Icon(s.icon, null, tint = Color.White)
            }
            Text(t(s.en, s.fa), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(t(s.dEn, s.dFa), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
