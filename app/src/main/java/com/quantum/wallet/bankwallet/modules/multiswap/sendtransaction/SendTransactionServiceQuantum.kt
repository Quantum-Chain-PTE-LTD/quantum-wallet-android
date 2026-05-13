package com.quantum.wallet.bankwallet.modules.multiswap.sendtransaction

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.ethereum.CautionViewItem
import com.quantum.wallet.bankwallet.core.ethereum.CautionViewItemFactory
import com.quantum.wallet.bankwallet.core.ethereum.EvmCoinServiceFactory
import com.quantum.wallet.bankwallet.modules.evmfee.Cautions
import com.quantum.wallet.bankwallet.modules.evmfee.FeeSettingsError
import com.quantum.wallet.bankwallet.modules.evmfee.NumberInputWithButtons
import com.quantum.wallet.bankwallet.modules.multiswap.ui.DataField
import com.quantum.wallet.bankwallet.modules.multiswap.ui.DataFieldNonce
import com.quantum.wallet.bankwallet.modules.send.SendModule
import com.quantum.wallet.bankwallet.modules.send.quantum.settings.SendQuantumNonceService
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItem
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
import com.quantum.wallet.bankwallet.uiv3.components.info.TextBlock
import com.quantum.quantumkit.core.LegacyGasPriceProvider
import com.quantum.quantumkit.decorations.TransactionDecoration
import com.quantum.quantumkit.models.GasPrice
import com.quantum.quantumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger

class SendTransactionServiceQuantum(
    blockchainType: BlockchainType,
) : AbstractSendTransactionService(true, true) {

    private val token by lazy { App.marketKit.token(TokenQuery(BlockchainType.QuantumChain, TokenType.Native))!! }
    private val quantumKitWrapper by lazy {
        val account =
            App.accountManager.activeAccount ?: throw IllegalArgumentException("No active account")
        App.quantumKitManager.getQuantumKitWrapper(account, blockchainType)
    }
    private val quantumKit by lazy { quantumKitWrapper.quantumKit }
    private val gasPriceProvider by lazy { LegacyGasPriceProvider(quantumKit) }

    private val coinServiceFactory by lazy {
        EvmCoinServiceFactory(
            token,
            App.marketKit,
            App.currencyManager,
            App.coinManager
        )
    }
    private val baseCoinService = coinServiceFactory.baseCoinService
    private val cautionViewItemFactory by lazy { CautionViewItemFactory(baseCoinService) }

    val nonceService by lazy { SendQuantumNonceService(quantumKit) }

    private val _sendTransactionSettingsFlow = MutableStateFlow(
        SendTransactionSettings.Quantum
    )
    override val sendTransactionSettingsFlow = _sendTransactionSettingsFlow.asStateFlow()

    private var transactionData: TransactionData? = null
    private var userGasLimit: Long? = null
    private var gasLimit: Long? = null
    private var recommendedGasPrice: Long? = null
    private var userGasPrice: Long? = null
    private var estimatedFee: BigInteger? = null
    private var feeAmountData: SendModule.AmountData? = null
    private var cautions: List<CautionViewItem> = listOf()
    private var sendable = false
    private var loading = true
    private var fields = listOf<DataField>()

    // Private scope owns all network I/O (gas price + estimateGas), mirroring
    // `EvmFeeService.coroutineScope`. This decouples the work from the caller's
    // dispatcher (typically Main via `viewModelScope.launch`) — RxJava2 Singles
    // returned by the kit run synchronously on the subscribing thread, so any
    // call originating on Main would otherwise crash with
    // NetworkOnMainThreadException.
    private val feeScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var feeJob: Job? = null

    private val effectiveGasPrice: Long?
        get() = userGasPrice ?: recommendedGasPrice

    override fun createState() = SendTransactionServiceState(
        uuid = uuid,
        networkFee = feeAmountData,
        cautions = cautions,
        sendable = sendable,
        loading = loading,
        fields = fields,
    )

    override fun start(coroutineScope: CoroutineScope) {
        // Prefetch recommended gas price on our own scope (Default dispatcher)
        // so we never block whichever thread invoked `start`.
        feeScope.launch {
            try {
                recommendedGasPrice = gasPriceProvider.gasPriceSingle().await()
                recompute()
            } catch (_: Throwable) {
                // will retry on setSendTransactionData
            }
        }
        feeScope.launch {
            nonceService.start()
        }
        coroutineScope.launch {
            nonceService.stateFlow.collect { nonceState ->
                fields = emptyList()
                nonceState.dataOrNull?.let {
                    if (!it.default || it.fixed) {
                        fields = listOf(DataFieldNonce(it.nonce))
                    }
                }
                emitState()
            }
        }
    }

    fun decorate(transactionData: TransactionData): TransactionDecoration? {
        return quantumKit.decorate(transactionData)
    }

    fun fixNonce(nonce: Long) {
        nonceService.fixNonce(nonce)
    }

    fun setGasPrice(gasPrice: Long) {
        userGasPrice = gasPrice
        recompute()
    }

    fun resetGasPrice() {
        userGasPrice = null
        recompute()
    }

    /**
     * Schedules a recompute of gas limit + fee on [feeScope]. Cancels any
     * in-flight job to avoid stale results racing the latest input.
     *
     * Network calls (`gasPriceSingle`, `estimateGas`) execute here — never on
     * the caller's thread — matching how `EvmFeeService` does its work in a
     * private `Dispatchers.Default` scope.
     */
    private fun recompute() {
        feeJob?.cancel()
        feeJob = feeScope.launch {
            val txData = transactionData ?: return@launch

            loading = true
            emitState()

            try {
                if (recommendedGasPrice == null) {
                    recommendedGasPrice = gasPriceProvider.gasPriceSingle().await()
                }
                val gasPrice = GasPrice.Legacy(
                    effectiveGasPrice ?: error("No gas price available")
                )

                val balance = quantumKit.accountState?.balance ?: BigInteger.ZERO

                // Mirror EvmCommonGasDataService / EvmFeeService:
                //  • When sending the full balance (no input data, value == balance),
                //    estimateGas with the real value would always fail with
                //    "insufficient funds for gas * price + value". Use a stub value
                //    of 1 wei for estimation, then deduct the fee from the value.
                //  • Try estimateGas with gasPrice first; fall back without gasPrice
                //    if the node rejects it (e.g. LowerThanBaseGasLimit).
                //  • Surcharge gas by 10% only when input is non-empty (contract call),
                //    matching EvmFeeModule.surcharged.
                val sendingMax = txData.input.isEmpty() && txData.value == balance && balance > BigInteger.ZERO
                val estimateData = if (sendingMax) {
                    com.quantum.quantumkit.models.TransactionData(txData.to, BigInteger.ONE, txData.input)
                } else {
                    txData
                }

                val limit = userGasLimit ?: gasLimit ?: run {
                    val estimated = try {
                        quantumKit.estimateGas(estimateData, gasPrice).await()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Throwable) {
                        quantumKit.estimateGas(estimateData, null).await()
                    }
                    if (txData.input.isNotEmpty()) {
                        estimated + estimated / 10
                    } else {
                        estimated
                    }
                }
                gasLimit = limit

                estimatedFee = limit.toBigInteger() * gasPrice.legacyGasPrice.toBigInteger()

                // When sending max, deduct fee from value so the tx fits the balance.
                val effectiveTxData = if (sendingMax) {
                    val adjustedValue = balance - estimatedFee!!
                    if (adjustedValue <= BigInteger.ZERO) {
                        cautions = cautionViewItemFactory.cautionViewItems(
                            listOf(),
                            listOf(FeeSettingsError.InsufficientBalance)
                        )
                        sendable = false
                        feeAmountData = baseCoinService.amountData(estimatedFee!!, txData.input.isNotEmpty())
                        loading = false
                        emitState()
                        return@launch
                    }
                    com.quantum.quantumkit.models.TransactionData(txData.to, adjustedValue, txData.input)
                } else {
                    txData
                }
                transactionData = effectiveTxData

                feeAmountData = baseCoinService.amountData(estimatedFee!!, txData.input.isNotEmpty())

                if (effectiveTxData.value + estimatedFee!! > balance) {
                    cautions = cautionViewItemFactory.cautionViewItems(
                        listOf(),
                        listOf(FeeSettingsError.InsufficientBalance)
                    )
                    sendable = false
                } else {
                    cautions = listOf()
                    sendable = true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                cautions = cautionViewItemFactory.cautionViewItems(listOf(), listOf(e))
                sendable = false
            }

            loading = false
            emitState()
        }
    }

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Quantum)

        transactionData = data.transactionData
        userGasLimit = data.gasLimit
        // Invalidate previous estimate so the new transactionData triggers a
        // fresh `estimateGas` call when `userGasLimit` is null.
        gasLimit = data.gasLimit
        recompute()
    }

    override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult.Quantum {
        val txData = transactionData ?: throw Exception("No transaction data")
        val gasPrice = GasPrice.Legacy(effectiveGasPrice ?: throw Exception("No gas price"))
        val limit = gasLimit ?: throw Exception("No gas limit")
        val nonce = nonceService.state.dataOrNull?.nonce

        // Force IO dispatcher so callers on Main (e.g. swap flow) can't
        // trigger NetworkOnMainThreadException through the kit's RxJava chain.
        val fullTransaction = withContext(Dispatchers.IO) {
            quantumKitWrapper
                .sendSingle(txData, gasPrice, limit, nonce).await()
        }
        return SendTransactionResult.Quantum(fullTransaction)
    }

    @Composable
    override fun GetSettingsContent(navController: NavController) {
        val currentGasPrice = effectiveGasPrice ?: 0L
        val isDefault = userGasPrice == null

        HSScaffold(
            title = stringResource(R.string.SendEvmSettings_Title),
            onBack = navController::popBackStack,
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Reset),
                    enabled = !isDefault,
                    onClick = { resetGasPrice() },
                    tint = ComposeAppTheme.colors.jacob
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
            ) {
                TextBlock(
                    text = stringResource(R.string.FeeSettings_GasPrice),
                )
                VSpacer(8.dp)
                NumberInputWithButtons(
                    value = currentGasPrice.toBigDecimal(),
                    decimals = 0,
                    textColor = ComposeAppTheme.colors.leah,
                    onValueChange = {
                        setGasPrice(it.toLong())
                    },
                    onClickIncrement = {
                        setGasPrice(currentGasPrice + 1)
                    },
                    onClickDecrement = {
                        if (currentGasPrice > 1) {
                            setGasPrice(currentGasPrice - 1)
                        }
                    }
                )

                VSpacer(32.dp)
            }
        }
    }

    @Composable
    override fun GetNonceSettingsContent(navController: NavController) {
        var nonceValue by remember { mutableStateOf(nonceService.state.dataOrNull?.nonce ?: 0L) }
        val isDefault = nonceService.state.dataOrNull?.default ?: true

        HSScaffold(
            title = stringResource(R.string.SendEvmSettings_Nonce),
            onBack = navController::popBackStack,
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Reset),
                    enabled = !isDefault,
                    onClick = {
                        /* reset not supported without coroutine scope */
                    },
                    tint = ComposeAppTheme.colors.jacob
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
            ) {
                TextBlock(
                    text = stringResource(R.string.SendEvmSettings_Nonce_Info),
                )
                VSpacer(8.dp)
                NumberInputWithButtons(
                    value = nonceValue.toBigDecimal(),
                    decimals = 0,
                    textColor = ComposeAppTheme.colors.leah,
                    onValueChange = {
                        nonceValue = it.toLong()
                        nonceService.setNonce(nonceValue)
                    },
                    onClickIncrement = {
                        nonceService.increment()
                        nonceValue = nonceService.state.dataOrNull?.nonce ?: nonceValue
                    },
                    onClickDecrement = {
                        nonceService.decrement()
                        nonceValue = nonceService.state.dataOrNull?.nonce ?: nonceValue
                    }
                )

                VSpacer(32.dp)
            }
        }
    }
}
