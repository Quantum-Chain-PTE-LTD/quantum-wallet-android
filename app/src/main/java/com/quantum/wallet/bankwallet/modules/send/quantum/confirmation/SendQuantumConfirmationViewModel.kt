package com.quantum.wallet.bankwallet.modules.send.quantum.confirmation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.ViewModelUiState
import com.quantum.wallet.bankwallet.core.ethereum.CautionViewItem
import com.quantum.wallet.bankwallet.core.ethereum.EvmCoinServiceFactory
import com.quantum.wallet.bankwallet.core.managers.RecentAddressManager
import com.quantum.wallet.bankwallet.entities.Address
import com.quantum.wallet.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import com.quantum.wallet.bankwallet.modules.multiswap.sendtransaction.SendTransactionServiceQuantum
import com.quantum.wallet.bankwallet.modules.multiswap.ui.DataField
import com.quantum.wallet.bankwallet.modules.send.SendModule
import com.quantum.wallet.bankwallet.modules.send.quantum.SendQuantumData
import com.quantum.wallet.bankwallet.modules.sendevmtransaction.SectionViewItem
import com.quantum.wallet.bankwallet.modules.sendevmtransaction.SendQuantumTransactionViewItemFactory
import com.quantum.quantumkit.decorations.OutgoingDecoration
import com.quantum.quantumkit.models.TransactionData
import com.quantum.qrc20kit.decorations.OutgoingQip20Decoration
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SendQuantumConfirmationViewModel(
    private val sendQuantumTransactionViewItemFactory: SendQuantumTransactionViewItemFactory,
    val sendTransactionService: SendTransactionServiceQuantum,
    private val transactionData: TransactionData,
    private val additionalInfo: SendQuantumData.AdditionalInfo?,
    private val recentAddressManager: RecentAddressManager,
    private val blockchainType: BlockchainType
) : ViewModelUiState<SendQuantumConfirmationUiState>() {
    private var initialLoading = true
    private var sendTransactionState = sendTransactionService.stateFlow.value

    private val transactionDecoration = sendTransactionService.decorate(transactionData)
    private val sectionViewItems = sendQuantumTransactionViewItemFactory.getItems(
        transactionData,
        additionalInfo,
        transactionDecoration
    )

    init {
        viewModelScope.launch {
            sendTransactionService.stateFlow.collect { transactionState ->
                sendTransactionState = transactionState
                initialLoading = initialLoading && transactionState.loading

                emitState()
            }
        }

        sendTransactionService.start(viewModelScope)

        viewModelScope.launch {
            sendTransactionService.setSendTransactionData(SendTransactionData.Quantum(transactionData, null))
        }
    }

    override fun createState() = SendQuantumConfirmationUiState(
        networkFee = sendTransactionState.networkFee,
        cautions = sendTransactionState.cautions,
        sendEnabled = sendTransactionState.sendable,
        transactionFields = sendTransactionState.fields,
        sectionViewItems = sectionViewItems,
        initialLoading = initialLoading,
    )

    suspend fun send() = withContext(Dispatchers.Default) {
        sendTransactionService.sendTransaction()

        val address = when (transactionDecoration) {
            is OutgoingQip20Decoration -> {
                transactionDecoration.to.qip55
            }

            is OutgoingDecoration -> {
                transactionDecoration.to.qip55
            }

            else -> null
        }
        address?.let {
            recentAddressManager.setRecentAddress(Address(address), blockchainType)
        }
    }

    class Factory(
        private val transactionData: TransactionData,
        private val additionalInfo: SendQuantumData.AdditionalInfo?,
        private val blockchainType: BlockchainType
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val sendTransactionService = SendTransactionServiceQuantum(blockchainType)
            val feeToken = App.marketKit.token(TokenQuery(BlockchainType.QuantumChain, TokenType.Native))!!
            val coinServiceFactory = EvmCoinServiceFactory(
                feeToken,
                App.marketKit,
                App.currencyManager,
                App.coinManager
            )

            val sendQuantumTransactionViewItemFactory = SendQuantumTransactionViewItemFactory(
                coinServiceFactory,
                App.contactsRepository,
                blockchainType
            )

            return SendQuantumConfirmationViewModel(
                sendQuantumTransactionViewItemFactory,
                sendTransactionService,
                transactionData,
                additionalInfo,
                App.recentAddressManager,
                blockchainType
            ) as T
        }
    }

}

data class SendQuantumConfirmationUiState(
    val networkFee: SendModule.AmountData?,
    val cautions: List<CautionViewItem>,
    val sendEnabled: Boolean,
    val transactionFields: List<DataField>,
    val sectionViewItems: List<SectionViewItem>,
    val initialLoading: Boolean
)
