package com.amishsxt.drawcast.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode(val label: String) {
    DEFAULT("Default"),
    LIGHT("Light"),
    DARK("Dark")
}

object AppThemeState {
    var themeMode by mutableStateOf(ThemeMode.DEFAULT)
}
