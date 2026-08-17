package com.qihe.clipflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.qihe.clipflow.data.preferences.AppPreferences
import com.qihe.clipflow.navigation.ClipFlowNavHost
import com.qihe.clipflow.ui.theme.ClipFlowTheme
import com.qihe.clipflow.ui.theme.ThemeMode
import com.qihe.clipflow.ui.theme.resolveDarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splashScreen.setKeepOnScreenCondition { false }

        setContent {
            val preferences = remember { AppPreferences(applicationContext) }
            val themeModeKey by preferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM.key)
            val dynamicColor by preferences.dynamicColor.collectAsState(initial = true)
            val isSystemDark = isSystemInDarkTheme()
            ClipFlowTheme(
                darkTheme = resolveDarkTheme(ThemeMode.fromKey(themeModeKey), isSystemDark),
                dynamicColor = dynamicColor,
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ClipFlowNavHost()
                }
            }
        }
    }
}
