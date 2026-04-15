package com.quantum.wallet.bankwallet.modules.settings.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App

object MainSettingsModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val viewModel = MainSettingsViewModel(
                App.backupManager,
                App.systemInfoManager,
                App.termsManager,
                App.pinComponent,
                App.accountManager,
                App.appConfigProvider,
            )

            return viewModel as T
        }
    }

}
