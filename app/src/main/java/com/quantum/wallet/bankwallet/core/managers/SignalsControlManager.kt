package com.quantum.wallet.bankwallet.core.managers

import com.quantum.wallet.bankwallet.core.ILocalStorage

class SignalsControlManager(
    private val localStorage: ILocalStorage,
) {
    val showSignalsStateChangedFlow = localStorage.marketSignalsStateChangedFlow

    var showSignals: Boolean
        get() = localStorage.marketFavoritesShowSignals
        set(value) {
            localStorage.marketFavoritesShowSignals = value
        }
}