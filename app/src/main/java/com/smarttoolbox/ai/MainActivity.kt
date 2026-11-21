package com.smarttoolbox.ai

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smarttoolbox.ai.data.Prefs
import com.smarttoolbox.ai.ui.AppRoot
import java.util.Locale

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs.get(app)
    var themeMode by mutableIntStateOf(prefs.themeMode); private set
    var accent by mutableIntStateOf(prefs.appAccent); private set
    var lang by mutableStateOf(prefs.lang); private set

    fun changeTheme(m: Int) { themeMode = m; prefs.themeMode = m }
    fun changeAccent(c: Int) { accent = c; prefs.appAccent = c }
    fun changeLang(l: String) { lang = l; prefs.lang = l }
    fun reload() { themeMode = prefs.themeMode; accent = prefs.appAccent; lang = prefs.lang }
    fun isFa(): Boolean = lang == "fa" || (lang == "system" && Locale.getDefault().language == "fa")
}

class MainActivity : ComponentActivity() {
    private var pendingEdit by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        pendingEdit = intent?.getStringExtra("edit_uri")?.let { Uri.parse(it) }
        setContent {
            val vm: MainViewModel = viewModel()
            AppRoot(vm, pendingEdit) { pendingEdit = null }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingEdit = intent.getStringExtra("edit_uri")?.let { Uri.parse(it) }
    }
}
