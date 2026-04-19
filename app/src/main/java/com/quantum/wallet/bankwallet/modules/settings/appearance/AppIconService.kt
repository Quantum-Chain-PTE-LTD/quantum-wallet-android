package com.quantum.wallet.bankwallet.modules.settings.appearance

import com.quantum.wallet.bankwallet.core.ILocalStorage
import com.quantum.wallet.bankwallet.ui.compose.Select
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppIconService(private val localStorage: ILocalStorage) {
    private val _optionsFlow = MutableStateFlow(
        Select(AppIcon.Main, listOf(AppIcon.Main))
    )
    val optionsFlow = _optionsFlow.asStateFlow()

    fun setAppIcon(appIcon: AppIcon) {
        localStorage.appIcon = AppIcon.Main
    }

    fun validateAndFixCurrentIcon() {
        localStorage.appIcon = AppIcon.Main
    }

    fun getAvailableIcons(): List<AppIcon> = listOf(AppIcon.Main)
}
