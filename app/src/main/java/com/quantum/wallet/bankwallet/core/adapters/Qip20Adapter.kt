package com.quantum.wallet.bankwallet.core.adapters

import android.content.Context
import com.quantum.wallet.bankwallet.core.AdapterState
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.BalanceData
import com.quantum.wallet.bankwallet.core.ICoinManager
import com.quantum.wallet.bankwallet.core.managers.EvmLabelManager
import com.quantum.wallet.bankwallet.core.managers.QuantumKitWrapper
import com.quantum.wallet.bankwallet.entities.Wallet
import com.quantum.wallet.bankwallet.entities.transactionrecords.TransactionRecord
import com.quantum.qrc20kit.core.Qrc20Kit
import com.quantum.quantumkit.core.QuantumKit.SyncState
import com.quantum.quantumkit.models.Address
import com.quantum.quantumkit.models.Chain
import com.quantum.quantumkit.models.DefaultBlockParameter
import com.quantum.quantumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.math.BigInteger

class Qip20Adapter(
    context: Context,
    quantumKitWrapper: QuantumKitWrapper,
    contractAddress: String,
    baseToken: Token,
    coinManager: ICoinManager,
    wallet: Wallet,
    evmLabelManager: EvmLabelManager
) : BaseQuantumAdapter(quantumKitWrapper, wallet.decimal, coinManager) {

    private val transactionConverter = QuantumTransactionConverter(coinManager, quantumKitWrapper, wallet.transactionSource, baseToken, evmLabelManager)

    private val contractAddress: Address = Address(contractAddress)
    val qip20Kit: Qrc20Kit = Qrc20Kit.getInstance(context, this.quantumKit, this.contractAddress)

    val pendingTransactions: List<TransactionRecord>
        get() = qip20Kit.getPendingTransactions().map { runBlocking { transactionConverter.transactionRecord(it) } }

    // IAdapter

    override fun start() {
        // started via QuantumKitManager
    }

    override fun stop() {
        // stopped via QuantumKitManager
    }

    override fun refresh() {
        qip20Kit.refresh()
    }

    // IBalanceAdapter

    override val balanceState: AdapterState
        get() = convertToAdapterState(qip20Kit.syncState)

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = qip20Kit.syncStateFlowable.map { }

    override val balanceData: BalanceData
        get() = BalanceData(balanceInBigDecimal(qip20Kit.balance, decimal))

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = qip20Kit.balanceFlowable.map { Unit }

    // ISendQuantumAdapter

    override fun getTransactionData(amount: BigDecimal, address: Address): TransactionData {
        val amountBigInt = amount.movePointRight(decimal).toBigInteger()
        return qip20Kit.buildTransferTransactionData(address, amountBigInt)
    }

    private fun convertToAdapterState(syncState: SyncState): AdapterState = when (syncState) {
        is SyncState.Synced -> AdapterState.Synced
        is SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
        is SyncState.Syncing -> AdapterState.Syncing()
    }

    fun allowance(spenderAddress: Address, defaultBlockParameter: DefaultBlockParameter): Single<BigDecimal> {
        return qip20Kit.getAllowanceAsync(spenderAddress, defaultBlockParameter)
            .map {
                scaleDown(it.toBigDecimal())
            }
    }

    fun buildRevokeTransactionData(spenderAddress: Address): TransactionData {
        return qip20Kit.buildApproveTransactionData(spenderAddress, BigInteger.ZERO)
    }

    fun buildApproveTransactionData(spenderAddress: Address, amount: BigDecimal): TransactionData {
        val amountBigInt = amount.movePointRight(decimal).toBigInteger()
        return qip20Kit.buildApproveTransactionData(spenderAddress, amountBigInt)
    }

    fun buildApproveUnlimitedTransactionData(spenderAddress: Address): TransactionData {
        val max = BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE)
        return qip20Kit.buildApproveTransactionData(spenderAddress, max)
    }

    companion object {
        fun clear(walletId: String) {
            Qrc20Kit.clear(App.instance, Chain.Quantum, walletId)
        }
    }
}
