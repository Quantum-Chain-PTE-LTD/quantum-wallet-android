package com.quantum.wallet.bankwallet.modules.send.quantum

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.ISendQuantumAdapter
import com.quantum.wallet.bankwallet.core.isNative
import com.quantum.wallet.bankwallet.entities.Address
import com.quantum.wallet.bankwallet.entities.Wallet
import com.quantum.wallet.bankwallet.modules.amount.AmountValidator
import com.quantum.wallet.bankwallet.modules.amount.SendAmountService
import com.quantum.wallet.bankwallet.modules.xrate.XRateService
import com.quantum.quantumkit.models.TransactionData
import kotlinx.parcelize.Parcelize
import java.math.BigInteger
import java.math.RoundingMode


data class SendQuantumData(
    val transactionData: TransactionData,
    val additionalInfo: AdditionalInfo? = null
) {
    sealed class AdditionalInfo : Parcelable {
        @Parcelize
        class Send(val info: SendInfo) : AdditionalInfo()

        val sendInfo: SendInfo?
            get() = (this as? Send)?.info
    }

    @Parcelize
    data class SendInfo(
        val nftShortMeta: NftShortMeta? = null
    ) : Parcelable

    @Parcelize
    data class NftShortMeta(
        val nftName: String,
        val previewImageUrl: String?
    ) : Parcelable
}

object SendQuantumModule {

    @Parcelize
    data class TransactionDataParcelable(
        val toAddress: String,
        val value: BigInteger,
        val input: ByteArray
    ) : Parcelable {
        constructor(transactionData: TransactionData) : this(
            transactionData.to.hex,
            transactionData.value,
            transactionData.input
        )
    }


    class Factory(
        private val wallet: Wallet,
        private val address: Address,
        private val hideAddress: Boolean,
        private val adapter: ISendQuantumAdapter
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendQuantumViewModel::class.java -> {
                    val amountValidator = AmountValidator()
                    val coinMaxAllowedDecimals = wallet.token.decimals

                    val amountService = SendAmountService(
                        amountValidator,
                        wallet.token.coin.code,
                        adapter.balanceData.available.setScale(coinMaxAllowedDecimals, RoundingMode.DOWN),
                        wallet.token.type.isNative
                    )
                    val addressService = SendQuantumAddressService()
                    val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)

                    SendQuantumViewModel(
                        wallet,
                        wallet.token,
                        adapter,
                        xRateService,
                        amountService,
                        addressService,
                        coinMaxAllowedDecimals,
                        !hideAddress,
                        App.connectivityManager,
                        address
                    ) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }
}
