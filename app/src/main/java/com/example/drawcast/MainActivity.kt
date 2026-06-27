package com.amishsxt.drawcast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.amishsxt.drawcast.navigation.DrawcastNavHost
import com.amishsxt.drawcast.screens.SplashScreen
import com.amishsxt.drawcast.ui.settings.AppThemeState
import com.amishsxt.drawcast.ui.settings.ThemeMode
import com.amishsxt.drawcast.core.AppLogger
import com.example.compose.AppTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        // Restore saved theme before splash so the correct color shows
        val prefs = getSharedPreferences("drawcast_prefs", MODE_PRIVATE)
        val saved = prefs.getString("theme_mode", ThemeMode.DEFAULT.name)
        AppThemeState.themeMode = ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.DEFAULT

        AppLogger.d(TAG, "onCreate")
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkTheme = when (AppThemeState.themeMode) {
                ThemeMode.DEFAULT -> isSystemInDarkTheme()
                ThemeMode.LIGHT   -> false
                ThemeMode.DARK    -> true
            }

            AppTheme(darkTheme = darkTheme, dynamicColor = false) {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "splash") {
                    composable("splash") {
                        SplashScreen(onNavigateToMain = {
                            navController.navigate("main") {
                                popUpTo("splash") { inclusive = true }
                            }
                        })
                    }
                    composable("main") {
                        DrawcastNavHost()
                    }
                }
            }
        }
    }
}
