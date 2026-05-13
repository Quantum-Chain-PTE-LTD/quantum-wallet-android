package com.quantum.wallet.bankwallet.core.adapters

import com.quantum.wallet.bankwallet.core.AdapterState
import com.quantum.wallet.bankwallet.core.ICoinManager
import com.quantum.wallet.bankwallet.core.ITransactionsAdapter
import com.quantum.wallet.bankwallet.core.managers.EvmLabelManager
import com.quantum.wallet.bankwallet.core.managers.QuantumKitWrapper
import com.quantum.wallet.bankwallet.entities.LastBlockInfo
import com.quantum.wallet.bankwallet.entities.transactionrecords.TransactionRecord
import com.quantum.wallet.bankwallet.modules.transactions.FilterTransactionType
import com.quantum.wallet.bankwallet.modules.transactions.TransactionSource
import com.quantum.quantumkit.core.QuantumKit
import com.quantum.quantumkit.core.hexStringToByteArray
import com.quantum.quantumkit.core.hexStringToByteArrayOrNull
import com.quantum.quantumkit.models.FullTransaction
import com.quantum.quantumkit.models.TransactionTag
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.reactivex.Flowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.await

class QuantumTransactionsAdapter(
    val quantumKitWrapper: QuantumKitWrapper,
    val baseToken: Token,
    coinManager: ICoinManager,
    source: TransactionSource,
    evmLabelManager: EvmLabelManager
) : ITransactionsAdapter {

    private val quantumKit = quantumKitWrapper.quantumKit
    private val quantumTransactionSource = com.quantum.quantumkit.models.TransactionSource.quantum(listOf())
    private val transactionConverter = QuantumTransactionConverter(coinManager, quantumKitWrapper, source, baseToken, evmLabelManager)

    override val explorerTitle: String
        get() = quantumTransactionSource.name

    override fun getTransactionUrl(transactionHash: String): String =
        quantumTransactionSource.transactionUrl(transactionHash)

    override val lastBlockInfo: LastBlockInfo?
        get() = quantumKit.lastBlockHeight?.toInt()?.let { LastBlockInfo(it) }

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = quantumKit.lastBlockHeightFlowable.map { }

    override val transactionsState: AdapterState
        get() = convertToAdapterState(quantumKit.transactionsSyncState)

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = quantumKit.transactionsSyncStateFlowable.map {}

    override val additionalTokenQueries: List<TokenQuery>
        get() = quantumKit.getTagTokenContractAddresses().map { address ->
            TokenQuery(quantumKitWrapper.blockchainType, TokenType.Qrc20(address))
        }

    override suspend fun getTransactions(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ): List<TransactionRecord> {
        return quantumKit.getFullTransactionsAsync(
            getFilters(token, transactionType, address?.lowercase()),
            from?.transactionHash?.hexStringToByteArray(),
            limit
        )
            .await()
            .map { tx -> transactionConverter.transactionRecord(tx) }
    }

    override suspend fun getTransactionsAfter(fromTransactionId: String?): List<TransactionRecord> {
        return quantumKit.getFullTransactionsAfterSingle(fromTransactionId?.hexStringToByteArrayOrNull())
            .await()
            .map { tx -> transactionConverter.transactionRecord(tx) }
    }

    override suspend fun getQuantumFullTransactionsBefore(
        fromTransactionHash: ByteArray?,
        limit: Int
    ): List<FullTransaction> {
        return quantumKit.getFullTransactionsAsync(
            emptyList(),
            fromTransactionHash,
            limit
        ).await()
    }

    override fun getTransactionRecordsFlow(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flow<List<TransactionRecord>> {
        return quantumKit.getFullTransactionsFlowable(getFilters(token, transactionType, address))
            .asFlow()
            .map { it.map { tx -> transactionConverter.transactionRecord(tx) } }
    }

    private fun convertToAdapterState(syncState: QuantumKit.SyncState): AdapterState =
        when (syncState) {
            is QuantumKit.SyncState.Synced -> AdapterState.Synced
            is QuantumKit.SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
            is QuantumKit.SyncState.Syncing -> AdapterState.Syncing()
        }

    private fun coinTagName(token: Token) = when (val type = token.type) {
        TokenType.Native -> TransactionTag.QVM_COIN
        is TokenType.Qrc20 -> type.address.lowercase()
        is TokenType.Eip20 -> type.address.lowercase()
        else -> ""
    }

    private fun getFilters(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ) = buildList {
        token?.let {
            add(listOf(coinTagName(it)))
        }

        val filterType = when (transactionType) {
            FilterTransactionType.All -> null
            FilterTransactionType.Incoming -> when {
                token != null -> TransactionTag.tokenIncoming(coinTagName(token))
                else -> TransactionTag.INCOMING
            }

            FilterTransactionType.Outgoing -> when {
                token != null -> TransactionTag.tokenOutgoing(coinTagName(token))
                else -> TransactionTag.OUTGOING
            }

            FilterTransactionType.Swap -> TransactionTag.SWAP
            FilterTransactionType.Approve -> TransactionTag.QIP20_APPROVE
        }

        filterType?.let {
            add(listOf(it))
        }

        if (!address.isNullOrBlank()) {
            val lower = address.lowercase()
            add(listOf("from_$lower", "to_$lower"))
        }
    }.also { filters ->
        android.util.Log.i(
            "QuantumTxAdapter",
            "getFilters token=${token?.coin?.code} type=$transactionType filters=$filters"
        )
    }
}
