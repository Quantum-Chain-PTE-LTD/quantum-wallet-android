package com.quantum.wallet.bankwallet.modules.send.quantum

import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.providers.Translator
import com.quantum.wallet.bankwallet.entities.Address
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.quantum.quantumkit.models.Address as QuantumAddress

class SendQuantumAddressService {
    private var address: Address? = null
    private var addressError: Throwable? = null
    private var quantumAddress: QuantumAddress? = null

    private val _stateFlow = MutableStateFlow(
        State(
            address = address,
            quantumAddress = quantumAddress,
            addressError = addressError,
            canBeSend = quantumAddress != null,
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    fun setAddress(address: Address?) {
        this.address = address

        validateAddress()

        emitState()
    }

    private fun validateAddress() {
        addressError = null
        quantumAddress = null
        val address = this.address ?: return

        try {
            quantumAddress = QuantumAddress(address.hex)
        } catch (e: Exception) {
            addressError = Throwable(Translator.getString(R.string.SwapSettings_Error_InvalidAddress))
        }
    }

    private fun emitState() {
        _stateFlow.update {
            State(
                address = address,
                quantumAddress = quantumAddress,
                addressError = addressError,
                canBeSend = quantumAddress != null
            )
        }
    }

    data class State(
        val address: Address?,
        val quantumAddress: QuantumAddress?,
        val addressError: Throwable?,
        val canBeSend: Boolean
    )
}
