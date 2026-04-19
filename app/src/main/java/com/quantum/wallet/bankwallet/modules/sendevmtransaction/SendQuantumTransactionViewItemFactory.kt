package com.quantum.wallet.bankwallet.modules.sendevmtransaction

import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.badge
import com.quantum.wallet.bankwallet.core.ethereum.EvmCoinServiceFactory
import com.quantum.wallet.bankwallet.core.providers.Translator
import com.quantum.wallet.bankwallet.modules.contacts.ContactsRepository
import com.quantum.wallet.bankwallet.modules.contacts.model.Contact
import com.quantum.wallet.bankwallet.modules.send.SendModule
import com.quantum.wallet.bankwallet.modules.send.quantum.SendQuantumData
import com.quantum.wallet.bankwallet.modules.send.quantum.SendQuantumData.AdditionalInfo
import com.quantum.wallet.core.toHexString
import com.quantum.quantumkit.decorations.OutgoingDecoration
import com.quantum.quantumkit.decorations.TransactionDecoration
import com.quantum.quantumkit.models.TransactionData
import com.quantum.qrc20kit.decorations.OutgoingQip20Decoration
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import java.math.BigInteger

class SendQuantumTransactionViewItemFactory(
    private val coinServiceFactory: EvmCoinServiceFactory,
    private val contactsRepo: ContactsRepository,
    private val blockchainType: BlockchainType
) {
    fun getItems(
        transactionData: TransactionData?,
        additionalInfo: AdditionalInfo?,
        decoration: TransactionDecoration?
    ): List<SectionViewItem> {
        var sections = decoration?.let {
            getViewItems(it)
        } ?: listOf()

        if (sections.isEmpty()) {
            if (transactionData != null) {
                sections = getUnknownMethodItems(transactionData)
            }
        }

        return sections
    }

    private fun getViewItems(
        decoration: TransactionDecoration
    ): List<SectionViewItem>? =
        when (decoration) {
            is OutgoingDecoration -> getSendBaseCoinItems(
                decoration.to.qip55,
                decoration.value
            )

            is OutgoingQip20Decoration -> getQip20TransferViewItems(
                decoration.to.qip55,
                decoration.value,
                decoration.contractAddress.hex
            )

            else -> null
        }

    private fun getQip20TransferViewItems(
        to: String,
        value: BigInteger,
        contractAddress: String
    ): List<SectionViewItem>? {
        val coinService = coinServiceFactory.getCoinService(contractAddress) ?: return null

        val viewItems: MutableList<ViewItem> = mutableListOf(
            getAmountWithTitle(
                coinService.amountData(value),
                ValueType.Outgoing,
                coinService.token,
                coinService.token.coin.code,
                coinService.token.badge
            )
        )
        val contact = getContact(to)
        viewItems.add(
            ViewItem.Address(
                Translator.getString(R.string.Send_Confirmation_To),
                to,
                contact?.name
            )
        )

        return listOf(SectionViewItem(viewItems))
    }

    private fun getContact(addressValue: String): Contact? {
        return contactsRepo.getContactsFiltered(blockchainType, addressQuery = addressValue).firstOrNull()
    }

    private fun getUnknownMethodItems(
        transactionData: TransactionData,
    ): List<SectionViewItem> {
        val viewItems = buildList {
            add(
                getAmount(
                    coinServiceFactory.baseCoinService.amountData(transactionData.value),
                    ValueType.Outgoing,
                    coinServiceFactory.baseCoinService.token
                )
            )
            val toValue = transactionData.to.qip55
            val contact = getContact(toValue)
            add(
                ViewItem.Address(
                    Translator.getString(R.string.Send_Confirmation_To),
                    toValue,
                    contact?.name
                )
            )

            add(ViewItem.Input("Input", transactionData.input.toHexString()))
        }

        return listOf(SectionViewItem(viewItems))
    }

    private fun getSendBaseCoinItems(to: String, value: BigInteger): List<SectionViewItem> {
        val baseCoinService = coinServiceFactory.baseCoinService

        val viewItems = buildList {
            add(
                getAmountWithTitle(
                    baseCoinService.amountData(value),
                    ValueType.Outgoing,
                    baseCoinService.token,
                    baseCoinService.token.coin.code,
                    baseCoinService.token.badge,
                )
            )
            val contact = getContact(to)
            add(
                ViewItem.Address(
                    Translator.getString(R.string.Send_Confirmation_To),
                    to,
                    contact?.name
                )
            )
        }

        return listOf(
            SectionViewItem(
                viewItems
            )
        )
    }

    private fun getAmount(amountData: SendModule.AmountData, valueType: ValueType, token: Token) =
        ViewItem.Amount(
            amountData.secondary?.getFormatted(),
            amountData.primary.getFormatted(),
            valueType,
            token
        )

    private fun getAmountWithTitle(
        amountData: SendModule.AmountData,
        valueType: ValueType,
        token: Token,
        title: String,
        badge: String?
    ): ViewItem.AmountWithTitle {
        return ViewItem.AmountWithTitle(
            amountData.secondary?.getFormatted(),
            amountData.primary.getFormatted(),
            valueType,
            token,
            title,
            badge
        )
    }
}
