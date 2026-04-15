package com.quantum.wallet.bankwallet.modules.multiswap

import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.ServiceState
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import kotlinx.coroutines.CoroutineScope
import java.math.BigDecimal

class SwapDefenseSystemService(
    private val supportsMevProtection: Boolean,
) : ServiceState<SwapDefenseSystemService.State>() {
    private var fiatPriceImpact: BigDecimal? = null
    private var fiatPriceImpactLevel: PriceImpactLevel? = null
    private var sendable = false
    private var mevProtectionEnabled = true

    private var systemMessage: DefenseSystemMessage? = null

    fun start(coroutineScope: CoroutineScope) {
        // MEV protection is always enabled when supported
    }

    fun setSwapProtectionEnabled(enabled: Boolean) {
        mevProtectionEnabled = enabled
        emitState()
    }

    override fun createState() = State(
        systemMessage = systemMessage,
        mevProtectionEnabled = supportsMevProtection && mevProtectionEnabled,
    )

    fun setPriceImpact(fiatPriceImpact: BigDecimal?, fiatPriceImpactLevel: PriceImpactLevel?) {
        this.fiatPriceImpact = fiatPriceImpact
        this.fiatPriceImpactLevel = fiatPriceImpactLevel

        refreshSystemMessage()

        emitState()
    }

    fun setSendable(sendable: Boolean) {
        this.sendable = sendable

        refreshSystemMessage()

        emitState()
    }

    private fun refreshSystemMessage() {
        systemMessage = null

        if (!sendable) return

        val fiatPriceImpact = fiatPriceImpact
        val fiatPriceImpactLevel = fiatPriceImpactLevel

        if (fiatPriceImpact != null && fiatPriceImpactLevel != null) {
            systemMessage = when (fiatPriceImpactLevel) {
                PriceImpactLevel.High -> {
                    DefenseSystemMessage(
                        level = DefenseAlertLevel.DANGER,
                        title = TranslatableString.ResString(R.string.SwapDefense_PriceImpact_High_Title),
                        body = TranslatableString.ResString(
                            R.string.SwapDefense_PriceImpact_High_Description,
                            fiatPriceImpact
                        ),
                    )
                }

                PriceImpactLevel.Forbidden -> {
                    DefenseSystemMessage(
                        level = DefenseAlertLevel.DANGER,
                        title = TranslatableString.ResString(R.string.SwapDefense_PriceImpact_Forbidden_Title),
                        body = TranslatableString.ResString(
                            R.string.SwapDefense_PriceImpact_Forbidden_Description,
                            fiatPriceImpact
                        ),
                    )
                }

                else -> null
            }
        }

        if (systemMessage == null && supportsMevProtection) {
            systemMessage = DefenseSystemMessage(
                level = DefenseAlertLevel.SAFE,
                title = TranslatableString.ResString(R.string.SwapDefense_Safe_Title),
                body = TranslatableString.ResString(R.string.SwapDefense_Safe_Description),
            )
        }
    }

    data class State(
        val systemMessage: DefenseSystemMessage?,
        val mevProtectionEnabled: Boolean,
    )
}

data class DefenseSystemMessage(
    val level: DefenseAlertLevel,
    val title: TranslatableString,
    val body: TranslatableString,
    val actionText: TranslatableString? = null,
)

enum class DefenseAlertLevel {
    WARNING,
    IDLE,
    DANGER,
    SAFE
}
