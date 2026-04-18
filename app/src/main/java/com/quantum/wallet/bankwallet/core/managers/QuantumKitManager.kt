package com.quantum.wallet.bankwallet.core.managers

import android.util.Log
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.BackgroundManager
import com.quantum.wallet.bankwallet.core.BackgroundManagerState
import com.quantum.wallet.bankwallet.core.UnsupportedAccountException
import com.quantum.wallet.bankwallet.entities.Account
import com.quantum.wallet.bankwallet.entities.AccountType
import com.quantum.qrc20kit.core.Qrc20Kit
import com.quantum.quantumkit.core.QuantumKit
import com.quantum.quantumkit.core.signer.Signer
import com.quantum.quantumkit.models.Address
import com.quantum.quantumkit.models.Chain
import com.quantum.quantumkit.models.FullTransaction
import com.quantum.quantumkit.models.GasPrice
import com.quantum.quantumkit.models.RpcSource
import com.quantum.quantumkit.models.TransactionData
import com.quantum.quantumkit.models.TransactionSource
import io.horizontalsystems.marketkit.models.BlockchainType
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class QuantumKitManager(
    private val backgroundManager: BackgroundManager,
) {
    private val chain = Chain.Quantum
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    private val kitStartedSubject = BehaviorSubject.createDefault(false)
    val kitStartedObservable: Observable<Boolean> = kitStartedSubject

    var quantumKitWrapper: QuantumKitWrapper? = null
        private set(value) {
            field = value
            kitStartedSubject.onNext(value != null)
        }

    private var useCount = 0
    var currentAccount: Account? = null
        private set
    private val quantumKitUpdatedSubject = PublishSubject.create<Unit>()

    val quantumKitUpdatedObservable: Observable<Unit>
        get() = quantumKitUpdatedSubject

    val statusInfo: Map<String, Any>?
        get() = quantumKitWrapper?.quantumKit?.statusInfo()

    @Synchronized
    fun getQuantumKitWrapper(account: Account): QuantumKitWrapper =
        getQuantumKitWrapper(account, BlockchainType.QuantumChain)

    @Synchronized
    fun getQuantumKitWrapper(account: Account, blockchainType: BlockchainType): QuantumKitWrapper {
        if (quantumKitWrapper != null && currentAccount != account) {
            stopKit()
        }

        if (this.quantumKitWrapper == null) {
            val accountType = account.type
            quantumKitWrapper = createKitInstance(accountType, account, blockchainType)
            useCount = 0
            currentAccount = account
            subscribeToEvents()
        }

        useCount++
        return this.quantumKitWrapper!!
    }

    private fun createKitInstance(
        accountType: AccountType,
        account: Account,
        blockchainType: BlockchainType
    ): QuantumKitWrapper {
        val rpcSource = RpcSource.quantumRpcHttp()
        val transactionSource = TransactionSource.quantum(listOf())

        val address: Address
        var signer: Signer? = null

        when (accountType) {
            is AccountType.Mnemonic -> {
                val seed: ByteArray = accountType.seed
                address = Signer.address(seed, chain)
                signer = Signer.getInstance(seed, chain)
            }
            else -> throw UnsupportedAccountException()
        }

        val quantumKit = QuantumKit.getInstance(
            App.instance,
            address,
            chain,
            rpcSource,
            transactionSource,
            account.id
        )

        Qrc20Kit.addTransactionSyncer(quantumKit)
        Qrc20Kit.addDecorators(quantumKit)

        quantumKit.start()

        return QuantumKitWrapper(quantumKit, blockchainType, signer)
    }

    @Synchronized
    fun unlink(account: Account) {
        if (account == currentAccount) {
            useCount -= 1

            if (useCount < 1) {
                Log.d("QuantumKitManager", "stopKit()")
                stopKit()
            }
        }
    }

    private fun subscribeToEvents() {
        job = coroutineScope.launch {
            backgroundManager.stateFlow.collect { state ->
                when (state) {
                    BackgroundManagerState.EnterForeground -> {
                        quantumKitWrapper?.quantumKit?.let { kit ->
                            kit.onEnterForeground()
                            delay(1000)
                            kit.refresh()
                        }
                    }
                    BackgroundManagerState.EnterBackground -> {
                        quantumKitWrapper?.quantumKit?.onEnterBackground()
                    }
                }
            }
        }
    }

    private fun stopKit() {
        job?.cancel()
        quantumKitWrapper?.quantumKit?.stop()
        quantumKitWrapper = null
        currentAccount = null
    }
}

class QuantumKitWrapper(
    val quantumKit: QuantumKit,
    val blockchainType: BlockchainType,
    val signer: Signer?
) {

    fun sendSingle(
        transactionData: TransactionData,
        gasPrice: GasPrice,
        gasLimit: Long,
        nonce: Long?
    ): Single<FullTransaction> {
        if (signer == null) return Single.error(Exception())

        return quantumKit.rawTransaction(transactionData, gasPrice, gasLimit, nonce)
            .flatMap { rawTransaction ->
                val signature = signer.signature(rawTransaction)
                quantumKit.send(rawTransaction, signature)
            }
    }
}
