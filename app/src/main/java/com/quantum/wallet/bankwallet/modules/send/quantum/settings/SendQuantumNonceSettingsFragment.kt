package com.quantum.wallet.bankwallet.modules.send.quantum.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.modules.send.quantum.confirmation.SendQuantumConfirmationViewModel

class SendQuantumNonceSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SendQuantumNonceSettingsScreen(navController)
    }
}

@Composable
fun SendQuantumNonceSettingsScreen(navController: NavController) {
    val viewModelStoreOwner = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(R.id.sendQuantumConfirmationFragment)
    }

    val viewModel = viewModel<SendQuantumConfirmationViewModel>(
        viewModelStoreOwner = viewModelStoreOwner,
    )

    viewModel.sendTransactionService.GetNonceSettingsContent(navController)
}
