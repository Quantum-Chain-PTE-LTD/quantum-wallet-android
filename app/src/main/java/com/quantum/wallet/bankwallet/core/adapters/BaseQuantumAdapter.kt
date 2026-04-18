package com.quantum.wallet.bankwallet.core.adapters

import com.quantum.wallet.bankwallet.core.IAdapter
import com.quantum.wallet.bankwallet.core.IBalanceAdapter
import com.quantum.wallet.bankwallet.core.ICoinManager
import com.quantum.wallet.bankwallet.core.IReceiveAdapter
import com.quantum.wallet.bankwallet.core.ISendQuantumAdapter
import com.quantum.wallet.bankwallet.core.managers.QuantumKitWrapper
import java.math.BigDecimal
import java.math.BigInteger

abstract class BaseQuantumAdapter(
    final override val quantumKitWrapper: QuantumKitWrapper,
    val decimal: Int,
    val coinManager: ICoinManager
) : IAdapter, ISendQuantumAdapter, IBalanceAdapter, IReceiveAdapter {

    val quantumKit = quantumKitWrapper.quantumKit

    override val debugInfo: String
        get() = ""

    val statusInfo: Map<String, Any>
        get() = quantumKit.statusInfo()

    // IReceiveAdapter

    override val receiveAddress: String
        get() = quantumKit.receiveAddress.qip55

    override val isMainNet: Boolean
        get() = true

    protected fun scaleDown(amount: BigDecimal, decimals: Int = decimal): BigDecimal {
        return amount.movePointLeft(decimals).stripTrailingZeros()
    }

    protected fun scaleUp(amount: BigDecimal, decimals: Int = decimal): BigInteger {
        return amount.movePointRight(decimals).toBigInteger()
    }

    protected fun balanceInBigDecimal(balance: BigInteger?, decimal: Int): BigDecimal {
        balance?.toBigDecimal()?.let {
            return scaleDown(it, decimal)
        } ?: return BigDecimal.ZERO
    }

    companion object {
        const val confirmationsThreshold: Int = 12
    }
}
