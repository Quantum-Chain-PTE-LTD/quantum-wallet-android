package com.quantum.wallet.bankwallet.modules.theme

import androidx.appcompat.app.AppCompatDelegate
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.ILocalStorage
import com.quantum.wallet.bankwallet.ui.compose.Select
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ThemeService(private val localStorage: ILocalStorage) {
    private val themes by lazy { ThemeType.entries }

    val selectedTheme: ThemeType
        get() = localStorage.currentTheme

    private val _optionsFlow = MutableStateFlow(
        Select(selectedTheme, themes)
    )
    val optionsFlow = _optionsFlow.asStateFlow()

    fun setThemeType(themeType: ThemeType) {
        App.pinComponent.keepUnlocked()
        localStorage.currentTheme = ThemeType.Dark

        _optionsFlow.update {
            Select(ThemeType.Dark, themes)
        }

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}
