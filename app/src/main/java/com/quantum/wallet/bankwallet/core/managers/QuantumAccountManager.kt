package com.quantum.wallet.bankwallet.core.managers

import com.quantum.wallet.bankwallet.core.AppLogger
import com.quantum.wallet.bankwallet.core.IAccountManager
import com.quantum.wallet.bankwallet.entities.Account
import com.quantum.wallet.bankwallet.entities.AccountOrigin
import com.quantum.wallet.bankwallet.entities.EnabledWallet
import com.quantum.qrc20kit.core.DataProvider
import com.quantum.qrc20kit.events.TransferEventInstance
import com.quantum.quantumkit.core.QuantumKit
import com.quantum.quantumkit.decorations.IncomingDecoration
import com.quantum.quantumkit.decorations.UnknownTransactionDecoration
import com.quantum.quantumkit.models.Address
import com.quantum.quantumkit.models.FullTransaction
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.util.concurrent.Executors

class QuantumAccountManager(
    private val blockchainType: BlockchainType,
    private val accountManager: IAccountManager,
    private val walletManager: WalletManager,
    private val marketKit: MarketKitWrapper,
    private val quantumKitManager: QuantumKitManager,
    private val tokenAutoEnableManager: TokenAutoEnableManager
) {
    private val logger = AppLogger("quantum-account-manager")
    private val singleDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val singleDispatcherCoroutineScope = CoroutineScope(singleDispatcher)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var transactionSubscriptionJob: Job? = null

    init {
        singleDispatcherCoroutineScope.launch {
            quantumKitManager.kitStartedObservable
                .asFlow()
                .collect { started ->
                    handleStarted(started)
                }
        }
    }

    private suspend fun handleStarted(started: Boolean) {
        try {
            if (started) {
                subscribeToTransactions()
            } else {
                stop()
            }
        } catch (exception: Exception) {
            logger.warning("error", exception)
        }
    }

    private fun stop() {
        transactionSubscriptionJob?.cancel()
    }

    private suspend fun subscribeToTransactions() {
        val quantumKitWrapper = quantumKitManager.quantumKitWrapper ?: return
        val account = accountManager.activeAccount ?: return

        transactionSubscriptionJob = coroutineScope.launch {
            quantumKitWrapper.quantumKit.allTransactionsFlowable.asFlow().cancellable()
                .collect { (fullTransactions, initial) ->
                    handle(fullTransactions, account, quantumKitWrapper, initial)
                }
        }
    }

    private fun handle(
        fullTransactions: List<FullTransaction>,
        account: Account,
        quantumKitWrapper: QuantumKitWrapper,
        initial: Boolean
    ) {
        val shouldAutoEnableTokens = tokenAutoEnableManager.isAutoEnabled(account, blockchainType)

        if (initial && account.origin == AccountOrigin.Restored && !account.isWatchAccount && !shouldAutoEnableTokens) {
            return
        }

        val address = quantumKitWrapper.quantumKit.receiveAddress

        val foundTokens = mutableSetOf<FoundToken>()
        val suspiciousTokenTypes = mutableSetOf<TokenType>()

        for (fullTransaction in fullTransactions) {
            when (val decoration = fullTransaction.decoration) {
                is IncomingDecoration -> {
                    foundTokens.add(FoundToken(TokenType.Native))
                }

                is UnknownTransactionDecoration -> {
                    if (decoration.internalTransactions.any { it.to == address }) {
                        foundTokens.add(FoundToken(TokenType.Native))
                    }

                    for (eventInstance in decoration.eventInstances) {
                        if (eventInstance !is TransferEventInstance) continue

                        if (eventInstance.to == address) {
                            val tokenType = TokenType.Qrc20(eventInstance.contractAddress.hex)

                            if (decoration.fromAddress == address) {
                                foundTokens.add(FoundToken(tokenType, eventInstance.tokenInfo))
                            } else {
                                suspiciousTokenTypes.add(tokenType)
                            }
                        }
                    }
                }
            }
        }

        handle(
            foundTokens = foundTokens.toList(),
            suspiciousTokenTypes = suspiciousTokenTypes.minus(foundTokens.map { it.tokenType }.toSet()).toList(),
            account = account,
            quantumKit = quantumKitWrapper.quantumKit
        )
    }

    private fun handle(
        foundTokens: List<FoundToken>,
        suspiciousTokenTypes: List<TokenType>,
        account: Account,
        quantumKit: QuantumKit
    ) {
        if (foundTokens.isEmpty() && suspiciousTokenTypes.isEmpty()) return

        try {
            val queries = (foundTokens.map { it.tokenType } + suspiciousTokenTypes).map { TokenQuery(blockchainType, it) }
            val tokens =
                if (queries.size >= 1000) {
                    queries.chunked(900) { chunk ->
                        marketKit.tokens(chunk)
                    }.flatten()
                } else {
                    marketKit.tokens(queries)
                }
            val tokenInfos = mutableListOf<TokenInfo>()

            foundTokens.forEach { foundToken ->
                val token = tokens.firstOrNull { it.type == foundToken.tokenType }
                if (token != null) {
                    tokenInfos.add(
                        TokenInfo(
                            type = foundToken.tokenType,
                            coinName = token.coin.name,
                            coinCode = token.coin.code,
                            tokenDecimals = token.decimals
                        )
                    )
                } else if (foundToken.tokenInfo != null) {
                    tokenInfos.add(
                        TokenInfo(
                            type = foundToken.tokenType,
                            coinName = foundToken.tokenInfo.tokenName,
                            coinCode = foundToken.tokenInfo.tokenSymbol,
                            tokenDecimals = foundToken.tokenInfo.tokenDecimal
                        )
                    )
                }
            }

            suspiciousTokenTypes.forEach { tokenType ->
                val token = tokens.firstOrNull { it.type == tokenType }
                if (token != null) {
                    tokenInfos.add(
                        TokenInfo(
                            type = tokenType,
                            coinName = token.coin.name,
                            coinCode = token.coin.code,
                            tokenDecimals = token.decimals
                        )
                    )
                }
            }
            coroutineScope.launch {
                handle(tokenInfos, account, quantumKit)
            }
        } catch (ex: Exception) {
            // ignore
        }
    }

    private suspend fun handle(tokenInfos: List<TokenInfo>, account: Account, quantumKit: QuantumKit) = withContext(Dispatchers.IO) {
        val existingWallets = walletManager.activeWallets
        val existingTokenTypeIds = existingWallets.map { it.token.type.id }
        val newTokenInfos = tokenInfos.filter { !existingTokenTypeIds.contains(it.type.id) }

        if (newTokenInfos.isEmpty()) return@withContext

        val userAddress = quantumKit.receiveAddress
        val dataProvider = DataProvider(quantumKit)

        val requests = newTokenInfos.map { tokenInfo ->
            val contractAddress = (tokenInfo.type as? TokenType.Qrc20)?.let {
                try {
                    Address(it.address)
                } catch (ex: Exception) {
                    null
                }
            }

            async {
                if (contractAddress != null) {
                    val balance = try {
                        dataProvider.getBalance(contractAddress, userAddress).await()
                    } catch (error: Throwable) {
                        null
                    }

                    if (balance == null || balance > BigInteger.ZERO) {
                        tokenInfo
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        }

        val enabledWallets = requests.awaitAll().filterNotNull().map { tokenInfo ->
            EnabledWallet(
                tokenQueryId = TokenQuery(blockchainType, tokenInfo.type).id,
                accountId = account.id,
                coinName = tokenInfo.coinName,
                coinCode = tokenInfo.coinCode,
                coinDecimals = tokenInfo.tokenDecimals,
                coinImage = null
            )
        }

        if (enabledWallets.isNotEmpty()) {
            walletManager.saveEnabledWallets(enabledWallets)
        }
    }

    data class TokenInfo(
        val type: TokenType,
        val coinName: String,
        val coinCode: String,
        val tokenDecimals: Int
    )

    data class FoundToken(
        val tokenType: TokenType,
        val tokenInfo: com.quantum.qrc20kit.events.TokenInfo? = null
    ) {
        override fun equals(other: Any?): Boolean {
            return other is FoundToken && tokenType.id == other.tokenType.id
        }

        override fun hashCode(): Int {
            return tokenType.id.hashCode()
        }
    }
}
