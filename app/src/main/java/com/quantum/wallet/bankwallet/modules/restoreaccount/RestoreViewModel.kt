package com.quantum.wallet.bankwallet.modules.restoreaccount

import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.ViewModelUiState
import com.quantum.wallet.bankwallet.core.nativeTokenQueries
import com.quantum.wallet.bankwallet.core.order
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.core.stats.statAccountType
import com.quantum.wallet.bankwallet.core.supported
import com.quantum.wallet.bankwallet.core.supports
import com.quantum.wallet.bankwallet.entities.AccountOrigin
import com.quantum.wallet.bankwallet.entities.AccountType
import com.quantum.wallet.bankwallet.entities.Wallet
import com.quantum.wallet.bankwallet.modules.enablecoin.restoresettings.BirthdayHeightConfig
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType

class RestoreViewModel: ViewModelUiState<RestoreViewModel.UiState>() {
    private val marketKit = App.marketKit
    private val accountFactory = App.accountFactory
    private val accountManager = App.accountManager
    private val tokenAutoEnableManager = App.tokenAutoEnableManager
    private val walletManager = App.walletManager

    var accountTypes: List<AccountType> = listOf()

    var accountType: AccountType? = null
        private set

    var accountName: String = ""
        private set

    var manualBackup: Boolean = false
        private set

    var fileBackup: Boolean = false
        private set

    var birthdayHeightConfig: BirthdayHeightConfig? = null
        private set

    var statPage: StatPage? = null
        private set

    var cancelBirthdayHeightConfig: Boolean = false

    private var openSelectCoinsScreen = false
    private var restored = false

    override fun createState() = UiState(
        openSelectCoinsScreen = openSelectCoinsScreen,
        restored = restored,
    )

    fun setAccountData(accountType: AccountType?, accountName: String, manualBackup: Boolean, fileBackup: Boolean, statPage: StatPage) {
        this.accountType = accountType
        this.accountName = accountName
        this.manualBackup = manualBackup
        this.fileBackup = fileBackup
        this.statPage = statPage
    }

    fun setAccountType(accountType: AccountType) {
        this.accountType = accountType
    }

    fun setBirthdayHeightConfig(config: BirthdayHeightConfig?) {
        birthdayHeightConfig = config
    }

    fun requestOpenSelectCoinsScreen() {
        when (val tmpAccountType = accountType) {
            is AccountType.TronPrivateKey,
            is AccountType.StellarSecretKey -> {
                restoreWithSingleCoin(tmpAccountType)
                restored = true
                emitState()
            }

            else -> {
                openSelectCoinsScreen = true
                emitState()
            }
        }
    }

    fun openSelectCoinsScreenHandled() {
        openSelectCoinsScreen = false
        emitState()
    }

    private fun restoreWithSingleCoin(accountType: AccountType) {
        val allowedBlockchainTypes = BlockchainType.supported.filter { it.supports(accountType) }
        val tokenQueries = allowedBlockchainTypes
            .map { it.nativeTokenQueries }
            .flatten()

        val nativeTokens = marketKit.tokens(tokenQueries).toMutableList()

        // Add hardcoded QUANTUM native token if MarketKit doesn't return it
        if (allowedBlockchainTypes.contains(BlockchainType.QuantumChain) &&
            nativeTokens.none { it.blockchainType == BlockchainType.QuantumChain }
        ) {
            nativeTokens.add(
                Token(
                    coin = Coin("quantum-chain", "Quantum", "QUANTUM"),
                    blockchain = Blockchain(BlockchainType.QuantumChain, "Quantum Chain", null),
                    type = TokenType.Native,
                    decimals = 18
                )
            )
        }

        val tokens = nativeTokens
            .filter { it.supports(accountType) }
            .sortedBy { it.type.order }

        val blockchains = tokens.map { it.blockchain }.toSet()

        val account = accountFactory.account(
            accountName,
            accountType,
            AccountOrigin.Restored,
            manualBackup,
            fileBackup,
        )
        accountManager.save(account)

        blockchains.forEach { blockchain ->
            tokenAutoEnableManager.markAutoEnable(account, blockchain.type)
        }

        val wallets = tokens.map { Wallet(it, account) }
        walletManager.save(wallets)

        statPage?.let { stat(page = it, event = StatEvent.ImportWallet(accountType.statAccountType)) }
    }

    data class UiState(
        val openSelectCoinsScreen: Boolean,
        val restored: Boolean
    )
}