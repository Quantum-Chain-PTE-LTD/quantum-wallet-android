package com.quantum.wallet.bankwallet.core.adapters

import com.quantum.wallet.bankwallet.core.AdapterState
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.BalanceData
import com.quantum.wallet.bankwallet.core.ICoinManager
import com.quantum.wallet.bankwallet.core.managers.QuantumKitWrapper
import com.quantum.quantumkit.core.QuantumKit
import com.quantum.quantumkit.models.Address
import com.quantum.quantumkit.models.TransactionData
import io.reactivex.Flowable
import java.math.BigDecimal

class QuantumAdapter(quantumKitWrapper: QuantumKitWrapper, coinManager: ICoinManager) :
    BaseQuantumAdapter(quantumKitWrapper, decimal, coinManager) {

    // IAdapter

    override fun start() {
        // started via QuantumKitManager
    }

    override fun stop() {
        // stopped via QuantumKitManager
    }

    override fun refresh() {
        // refreshed via QuantumKitManager
    }

    // IBalanceAdapter

    override val balanceState: AdapterState
        get() = convertToAdapterState(quantumKit.syncState)

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = quantumKit.syncStateFlowable.map {}

    override val balanceData: BalanceData
        get() = BalanceData(balanceInBigDecimal(quantumKit.accountState?.balance, decimal))

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = quantumKit.accountStateFlowable.map { }

    private fun convertToAdapterState(syncState: QuantumKit.SyncState): AdapterState =
        when (syncState) {
            is QuantumKit.SyncState.Synced -> AdapterState.Synced
            is QuantumKit.SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
            is QuantumKit.SyncState.Syncing -> AdapterState.Syncing()
        }

    // ISendQuantumAdapter

    override fun getTransactionData(amount: BigDecimal, address: Address): TransactionData {
        val amountBigInt = amount.movePointRight(decimal).toBigInteger()
        return TransactionData(address, amountBigInt, byteArrayOf())
    }

    companion object {
        const val decimal = 18

        fun clear(walletId: String) {
            QuantumKit.clear(App.instance, com.quantum.quantumkit.models.Chain.Quantum, walletId)
        }
    }
}
