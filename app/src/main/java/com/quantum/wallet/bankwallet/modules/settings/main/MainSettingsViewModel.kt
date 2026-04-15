package com.quantum.wallet.bankwallet.modules.settings.main

import androidx.lifecycle.viewModelScope
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.IAccountManager
import com.quantum.wallet.bankwallet.core.IBackupManager
import com.quantum.wallet.bankwallet.core.ITermsManager
import com.quantum.wallet.bankwallet.core.ViewModelUiState
import com.quantum.wallet.bankwallet.core.providers.AppConfigProvider
import com.quantum.wallet.bankwallet.core.providers.Translator
import com.quantum.wallet.core.IPinComponent
import com.quantum.wallet.core.ISystemInfoManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

class MainSettingsViewModel(
    private val backupManager: IBackupManager,
    private val systemInfoManager: ISystemInfoManager,
    private val termsManager: ITermsManager,
    private val pinComponent: IPinComponent,
    private val accountManager: IAccountManager,
    private val appConfigProvider: AppConfigProvider,
) : ViewModelUiState<MainSettingUiState>() {

    val fdroidSupportLink by lazy {
        appConfigProvider.simplexSupportChat
    }
    val vipSupportLink by lazy {
        appConfigProvider.telegramSupportChat
    }

    val appVersion: String
        get() {
            var appVersion = systemInfoManager.appVersion
            if (Translator.getString(R.string.is_release) == "false") {
                appVersion += " (${appConfigProvider.appBuild})"
            }

            return appVersion
        }

    val companyWebPage = appConfigProvider.companyWebPageLink

    private val appWebPageLink = appConfigProvider.appWebPageLink
    private val hasNonStandardAccount: Boolean
        get() = accountManager.hasNonStandardAccount

    private val allBackedUp: Boolean
        get() = backupManager.allBackedUp

    private val isPinSet: Boolean
        get() = pinComponent.isPinSet

    init {
        viewModelScope.launch {
            backupManager.allBackedUpFlowable.asFlow().collect {
                emitState()
            }
        }
        viewModelScope.launch {
            pinComponent.pinSetFlow.collect {
                emitState()
            }
        }

        viewModelScope.launch {
            termsManager.termsAcceptedSharedFlow.collect {
                emitState()
            }
        }
    }

    override fun createState(): MainSettingUiState {
        return MainSettingUiState(
            appWebPageLink = appWebPageLink,
            hasNonStandardAccount = hasNonStandardAccount,
            allBackedUp = allBackedUp,
            manageWalletShowAlert = !allBackedUp || hasNonStandardAccount,
            securityCenterShowAlert = !isPinSet,
            aboutAppShowAlert = !termsManager.allTermsAccepted,
        )
    }
}

data class MainSettingUiState(
    val appWebPageLink: String,
    val hasNonStandardAccount: Boolean,
    val allBackedUp: Boolean,
    val manageWalletShowAlert: Boolean,
    val securityCenterShowAlert: Boolean,
    val aboutAppShowAlert: Boolean,
)