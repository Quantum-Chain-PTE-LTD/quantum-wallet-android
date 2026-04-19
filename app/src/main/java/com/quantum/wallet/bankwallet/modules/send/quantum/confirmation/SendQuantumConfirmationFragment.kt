package com.quantum.wallet.bankwallet.modules.send.quantum.confirmation

import android.os.Parcelable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.AppLogger
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.slideFromBottom
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.modules.confirm.ConfirmTransactionScreen
import com.quantum.wallet.bankwallet.modules.confirm.ErrorBottomSheet
import com.quantum.wallet.bankwallet.modules.send.quantum.SendQuantumData
import com.quantum.wallet.bankwallet.modules.send.quantum.SendQuantumModule
import com.quantum.wallet.bankwallet.modules.sendevmtransaction.SendEvmTransactionView
import com.quantum.wallet.bankwallet.ui.compose.components.ButtonPrimaryYellow
import com.quantum.wallet.core.helpers.HudHelper
import com.quantum.quantumkit.models.Address
import com.quantum.quantumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class SendQuantumConfirmationFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            SendQuantumConfirmationScreen(navController, input)
        }
    }

    @Parcelize
    data class Input(
        val transactionDataParcelable: SendQuantumModule.TransactionDataParcelable,
        val additionalInfo: SendQuantumData.AdditionalInfo?,
        val blockchainType: BlockchainType,
        val sendEntryPointDestId: Int
    ) : Parcelable {
        val transactionData: TransactionData
            get() = TransactionData(
                Address(transactionDataParcelable.toAddress),
                transactionDataParcelable.value,
                transactionDataParcelable.input
            )

        constructor(
            sendData: SendQuantumData,
            blockchainType: BlockchainType,
            sendEntryPointDestId: Int
        ) : this(
            SendQuantumModule.TransactionDataParcelable(sendData.transactionData),
            sendData.additionalInfo,
            blockchainType,
            sendEntryPointDestId
        )
    }
}

@Composable
private fun SendQuantumConfirmationScreen(
    navController: NavController,
    input: SendQuantumConfirmationFragment.Input
) {
    val logger = remember { AppLogger("send-quantum") }

    val currentBackStackEntry = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(R.id.sendQuantumConfirmationFragment)
    }
    val viewModel = viewModel<SendQuantumConfirmationViewModel>(
        viewModelStoreOwner = currentBackStackEntry,
        factory = SendQuantumConfirmationViewModel.Factory(
            input.transactionData,
            input.additionalInfo,
            input.blockchainType,
        )
    )
    val uiState = viewModel.uiState

    ConfirmTransactionScreen(
        title = stringResource(R.string.Send_Confirmation_Title),
        initialLoading = uiState.initialLoading,
        onClickBack = { navController.popBackStack() },
        onClickFeeSettings = {
            navController.slideFromBottom(R.id.sendQuantumSettingsFragment)
        },
        onClickNonceSettings = {
            navController.slideFromBottom(R.id.sendQuantumNonceSettingsFragment)
        },
        buttonsSlot = {
            val coroutineScope = rememberCoroutineScope()
            val view = LocalView.current
            var sendButtonTitle by remember { mutableIntStateOf(R.string.Send_Confirmation_Send_Button) }
            var buttonEnabled by remember { mutableStateOf(true) }

            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp),
                title = stringResource(sendButtonTitle),
                onClick = {
                    logger.info("click send button")
                    sendButtonTitle = R.string.Send_Sending
                    buttonEnabled = false

                    coroutineScope.launch {
                        try {
                            logger.info("sending tx")
                            viewModel.send()
                            logger.info("success")
                            stat(page = StatPage.SendConfirmation, event = StatEvent.Send)

                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                            delay(1200)

                            navController.popBackStack(input.sendEntryPointDestId, true)
                        } catch (t: Throwable) {
                            logger.warning("failed", t)
                            navController.slideFromBottom(R.id.errorBottomSheet, ErrorBottomSheet.Input(t.message ?: t.javaClass.simpleName))
                        }

                        sendButtonTitle = R.string.Send_Confirmation_Send_Button
                        buttonEnabled = true
                    }
                },
                enabled = uiState.sendEnabled && buttonEnabled
            )
        }
    ) {
        SendEvmTransactionView(
            navController,
            uiState.sectionViewItems,
            uiState.cautions,
            uiState.transactionFields,
            uiState.networkFee,
            StatPage.SendConfirmation
        )
    }
}
