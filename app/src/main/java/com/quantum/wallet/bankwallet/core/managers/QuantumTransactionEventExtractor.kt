package com.quantum.wallet.bankwallet.core.managers

import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.entities.TransactionValue
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.TransferEvent
import com.quantum.qrc20kit.decorations.OutgoingQip20Decoration
import com.quantum.qrc20kit.events.TokenInfo
import com.quantum.qrc20kit.events.TransferEventInstance
import com.quantum.quantumkit.decorations.IncomingDecoration
import com.quantum.quantumkit.decorations.OutgoingDecoration
import com.quantum.quantumkit.decorations.UnknownTransactionDecoration
import com.quantum.quantumkit.models.Address
import com.quantum.quantumkit.models.FullTransaction
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Extracts transfer events from Quantum Chain transactions for spam detection.
 */
class QuantumTransactionEventExtractor {

    /**
     * Extract outgoing transaction info from Quantum FullTransaction.
     */
    fun extractOutgoingInfo(
        fullTx: FullTransaction,
        userAddress: Address
    ): PoisoningScorer.OutgoingTxInfo? {
        val tx = fullTx.transaction
        return when (val decoration = fullTx.decoration) {
            is OutgoingDecoration -> {
                PoisoningScorer.OutgoingTxInfo(decoration.to.qip55, tx.timestamp, tx.blockNumber?.toInt())
            }
            is OutgoingQip20Decoration -> {
                PoisoningScorer.OutgoingTxInfo(decoration.to.qip55, tx.timestamp, tx.blockNumber?.toInt())
            }
            is UnknownTransactionDecoration -> {
                if (tx.from == userAddress) {
                    decoration.eventInstances
                        .mapNotNull { it as? TransferEventInstance }
                        .firstOrNull { it.from == userAddress }?.let { transfer ->
                            PoisoningScorer.OutgoingTxInfo(transfer.to.qip55, tx.timestamp, tx.blockNumber?.toInt())
                        }
                } else null
            }
            else -> null
        }
    }

    /**
     * Extract incoming events from Quantum FullTransaction.
     */
    fun extractIncomingEvents(
        fullTx: FullTransaction,
        userAddress: Address,
        baseToken: Token,
        blockchainType: BlockchainType
    ): List<TransferEvent> {
        return when (val decoration = fullTx.decoration) {
            is IncomingDecoration -> {
                val value = convertAmount(decoration.value, baseToken.decimals)
                listOf(TransferEvent(decoration.from.qip55, TransactionValue.CoinValue(baseToken, value)))
            }
            is UnknownTransactionDecoration -> {
                decoration.eventInstances
                    .mapNotNull { it as? TransferEventInstance }
                    .filter { it.to == userAddress || it.from == userAddress }
                    .map { transfer ->
                        val tokenValue = getQip20Value(transfer.contractAddress, transfer.value, blockchainType, transfer.tokenInfo)
                        if (transfer.from == userAddress) {
                            TransferEvent(transfer.to.qip55, tokenValue)
                        } else {
                            TransferEvent(transfer.from.qip55, tokenValue)
                        }
                    }
            }
            else -> emptyList()
        }
    }

    private fun getQip20Value(
        tokenAddress: Address,
        amount: BigInteger,
        blockchainType: BlockchainType,
        tokenInfo: TokenInfo?
    ): TransactionValue {
        val query = TokenQuery(blockchainType, TokenType.Eip20(tokenAddress.hex))
        val token = App.coinManager.getToken(query)

        return when {
            token != null -> {
                TransactionValue.CoinValue(token, convertAmount(amount, token.decimals))
            }
            tokenInfo != null -> {
                TransactionValue.TokenValue(
                    tokenName = tokenInfo.tokenName,
                    tokenCode = tokenInfo.tokenSymbol,
                    tokenDecimals = tokenInfo.tokenDecimal,
                    value = convertAmount(amount, tokenInfo.tokenDecimal)
                )
            }
            else -> {
                TransactionValue.RawValue(value = amount)
            }
        }
    }

    private fun convertAmount(amount: BigInteger, decimals: Int): BigDecimal {
        val result = amount.toBigDecimal().movePointLeft(decimals).stripTrailingZeros()
        return if (result.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO else result
    }
}
