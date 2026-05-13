package com.quantum.wallet.bankwallet.modules.market.search

import com.quantum.wallet.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.FullCoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MarketSearchService(private val marketKit: MarketKitWrapper) {
    private var query: String = ""
    private var results: List<FullCoin> = listOf()

    private val _stateFlow = MutableStateFlow(
        State(
            results = results,
            query = query
        )
    )
    val stateFlow: StateFlow<State>
        get() = _stateFlow.asStateFlow()

    private fun emitState() {
        _stateFlow.update {
            State(
                results = results,
                query = query
            )
        }
    }

    fun setQuery(query: String) {
        this.query = query

        refreshResults()
        emitState()
    }

    private fun refreshResults() {
        results = if (query.isBlank()) {
            listOf()
        } else {
            marketKit.fullCoins(query).filter { fullCoin ->
                // Exclude the Quantum Chain native coin and coins whose tokens
                // exist exclusively on the Quantum Chain blockchain.
                fullCoin.coin.uid != "quantum-chain" &&
                    (fullCoin.tokens.isEmpty() ||
                        fullCoin.tokens.any { it.blockchainType != BlockchainType.QuantumChain })
            }
        }
    }

    data class State(val results: List<FullCoin>, val query: String)
}
