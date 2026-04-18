package com.quantum.wallet.bankwallet.core.adapters

import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.ICoinManager
import com.quantum.wallet.bankwallet.core.managers.EvmLabelManager
import com.quantum.wallet.bankwallet.core.managers.QuantumKitWrapper
import com.quantum.wallet.bankwallet.core.tokenIconPlaceholder
import com.quantum.wallet.bankwallet.entities.TransactionValue
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.ApproveTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.ContractCallTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.ContractCreationTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.EvmIncomingTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.EvmOutgoingTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.EvmTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.ExternalContractCallTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.TransferEvent
import com.quantum.wallet.bankwallet.modules.transactions.TransactionSource
import com.quantum.qrc20kit.decorations.ApproveQip20Decoration
import com.quantum.qrc20kit.decorations.OutgoingQip20Decoration
import com.quantum.qrc20kit.events.TokenInfo
import com.quantum.qrc20kit.events.TransferEventInstance
import com.quantum.quantumkit.core.QuantumKit
import com.quantum.quantumkit.decorations.ContractCreationDecoration
import com.quantum.quantumkit.decorations.IncomingDecoration
import com.quantum.quantumkit.decorations.OutgoingDecoration
import com.quantum.quantumkit.decorations.UnknownTransactionDecoration
import com.quantum.quantumkit.models.Address
import com.quantum.quantumkit.models.FullTransaction
import com.quantum.quantumkit.models.InternalTransaction
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import java.math.BigDecimal
import java.math.BigInteger
import io.horizontalsystems.ethereumkit.models.Transaction as EvmTransaction
import com.quantum.quantumkit.models.Transaction as QuantumTransaction

class QuantumTransactionConverter(
    private val coinManager: ICoinManager,
    private val quantumKitWrapper: QuantumKitWrapper,
    private val source: TransactionSource,
    private val baseToken: Token,
    private val evmLabelManager: EvmLabelManager
) {
    private val quantumKit: QuantumKit
        get() = quantumKitWrapper.quantumKit

    /**
     * Maps a QuantumKit Transaction to an EthereumKit Transaction so the
     * shared EvmTransactionRecord hierarchy can be reused without duplication.
     */
    private fun com.quantum.quantumkit.models.Transaction.toEvmTransaction(): EvmTransaction {
        return EvmTransaction(
            hash = hash,
            timestamp = timestamp,
            isFailed = isFailed,
            blockNumber = blockNumber,
            transactionIndex = transactionIndex,
            from = from?.let { io.horizontalsystems.ethereumkit.models.Address(it.hex) },
            to = to?.let { io.horizontalsystems.ethereumkit.models.Address(it.hex) },
            value = value,
            input = input,
            nonce = nonce,
            gasPrice = gasPrice,
            maxFeePerGas = maxFeePerGas,
            maxPriorityFeePerGas = maxPriorityFeePerGas,
            gasLimit = gasLimit,
            gasUsed = gasUsed,
            replacedWith = replacedWith
        )
    }

    suspend fun transactionRecord(fullTransaction: FullTransaction): EvmTransactionRecord {
        val transaction = fullTransaction.transaction
        val evmTx = transaction.toEvmTransaction()

        val transactionRecord = when (val decoration = fullTransaction.decoration) {
            is ContractCreationDecoration -> {
                ContractCreationTransactionRecord(evmTx, baseToken, source, false)
            }

            is IncomingDecoration -> {
                val fromAddress = decoration.from.qip55
                val transactionValue = baseCoinValue(decoration.value, false)
                val isSpam = App.spamManager.isSpam(
                    transaction.hash,
                    listOf(TransferEvent(fromAddress, transactionValue)),
                    source,
                    transaction.timestamp,
                    transaction.blockNumber?.toInt()
                )
                EvmIncomingTransactionRecord(evmTx, baseToken, source, fromAddress, transactionValue, isSpam, false)
            }

            is OutgoingDecoration -> {
                val toAddress = decoration.to.qip55
                EvmOutgoingTransactionRecord(
                    evmTx,
                    baseToken,
                    source,
                    toAddress,
                    baseCoinValue(decoration.value, true),
                    decoration.sentToSelf,
                    false
                )
            }

            is OutgoingQip20Decoration -> {
                val toAddress = decoration.to.qip55
                EvmOutgoingTransactionRecord(
                    evmTx,
                    baseToken,
                    source,
                    toAddress,
                    getQip20Value(decoration.contractAddress, decoration.value, true, decoration.tokenInfo),
                    decoration.sentToSelf,
                    false
                )
            }

            is ApproveQip20Decoration -> {
                ApproveTransactionRecord(
                    evmTx,
                    baseToken,
                    source,
                    decoration.spender.qip55,
                    getQip20Value(decoration.contractAddress, decoration.value, false),
                    false
                )
            }

            is UnknownTransactionDecoration -> {
                val address = quantumKit.receiveAddress

                val internalTransactions = decoration.internalTransactions.filter { it.to == address }

                val qip20Transfers = decoration.eventInstances.mapNotNull { it as? TransferEventInstance }
                val incomingQip20Transfers = qip20Transfers.filter { it.to == address && it.from != address }
                val outgoingQip20Transfers = qip20Transfers.filter { it.from == address }

                val contractAddress = transaction.to
                val value = transaction.value

                val incomingEvents = getInternalEvents(internalTransactions) +
                        getIncomingQip20Events(incomingQip20Transfers)

                val outgoingEvents = getOutgoingQip20Events(outgoingQip20Transfers)

                when {
                    transaction.from == address && contractAddress != null && value != null -> {
                        ContractCallTransactionRecord(
                            evmTx, baseToken, source,
                            contractAddress.qip55,
                            transaction.input?.let { evmLabelManager.methodLabel(it) },
                            incomingEvents,
                            getTransactionValueEvents(transaction) + outgoingEvents,
                            false
                        )
                    }

                    transaction.from != address && transaction.to != address -> {
                        val isSpam = App.spamManager.isSpam(
                            transaction.hash,
                            incomingEvents + outgoingEvents,
                            source,
                            transaction.timestamp,
                            transaction.blockNumber?.toInt()
                        )
                        ExternalContractCallTransactionRecord(
                            evmTx, baseToken, source,
                            incomingEvents,
                            outgoingEvents,
                            isSpam,
                            false
                        )
                    }

                    else -> null
                }
            }

            else -> null
        }

        return transactionRecord ?: EvmTransactionRecord(
            transaction = evmTx,
            baseToken = baseToken,
            source = source,
            protected = false,
            foreignTransaction = transaction.from != quantumKit.receiveAddress
        )
    }

    private fun convertAmount(amount: BigInteger, decimal: Int, negative: Boolean): BigDecimal {
        var significandAmount = amount.toBigDecimal().movePointLeft(decimal).stripTrailingZeros()

        if (significandAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO
        }

        if (negative) {
            significandAmount = significandAmount.negate()
        }

        return significandAmount
    }

    private fun getQip20Value(tokenAddress: Address, amount: BigInteger, negative: Boolean, tokenInfo: TokenInfo? = null): TransactionValue {
        val query = TokenQuery(quantumKitWrapper.blockchainType, TokenType.Eip20(tokenAddress.hex))
        val token = coinManager.getToken(query)

        return when {
            token != null -> {
                TransactionValue.CoinValue(token, convertAmount(amount, token.decimals, negative))
            }

            tokenInfo != null -> {
                TransactionValue.TokenValue(
                    tokenName = tokenInfo.tokenName,
                    tokenCode = tokenInfo.tokenSymbol,
                    tokenDecimals = tokenInfo.tokenDecimal,
                    value = convertAmount(amount, tokenInfo.tokenDecimal, negative),
                    coinIconPlaceholder = quantumKitWrapper.blockchainType.tokenIconPlaceholder
                )
            }

            else -> {
                TransactionValue.RawValue(value = amount)
            }
        }
    }

    private fun baseCoinValue(value: BigInteger, negative: Boolean): TransactionValue {
        val amount = convertAmount(value, baseToken.decimals, negative)
        return TransactionValue.CoinValue(baseToken, amount)
    }

    private fun getInternalEvents(internalTransactions: List<InternalTransaction>): List<TransferEvent> {
        return internalTransactions.map { transaction ->
            TransferEvent(transaction.from.qip55, baseCoinValue(transaction.value, false))
        }
    }

    private fun getTransactionValueEvents(transaction: QuantumTransaction): List<TransferEvent> {
        val value = transaction.value
        if (value == null || value <= BigInteger.ZERO) return listOf()

        return listOf(
            TransferEvent(transaction.to?.qip55, baseCoinValue(value, true))
        )
    }

    private fun getIncomingQip20Events(incomingTransfers: List<TransferEventInstance>): List<TransferEvent> {
        return incomingTransfers.map { transfer ->
            TransferEvent(
                transfer.from.qip55,
                getQip20Value(transfer.contractAddress, transfer.value, false, transfer.tokenInfo)
            )
        }
    }

    private fun getOutgoingQip20Events(outgoingTransfers: List<TransferEventInstance>): List<TransferEvent> {
        return outgoingTransfers.map { transfer ->
            TransferEvent(
                transfer.to.qip55,
                getQip20Value(transfer.contractAddress, transfer.value, true, transfer.tokenInfo)
            )
        }
    }
}
