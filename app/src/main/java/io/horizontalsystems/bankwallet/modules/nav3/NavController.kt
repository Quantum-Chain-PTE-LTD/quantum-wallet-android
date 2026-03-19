package io.horizontalsystems.bankwallet.modules.nav3

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavOptions

class NavController {

    fun popBackStack() {

    }

    fun popBackStack(i: Int, b: Boolean) {

    }

    fun navigate(resId: Int, args: Bundle?, navOptions: NavOptions) {}

    val previousBackStackEntry: NavBackStackEntry? = TODO()
    val currentBackStackEntry: NavBackStackEntry? = TODO()

    fun getBackStackEntry(@IdRes destinationId: Int): NavBackStackEntry {
        TODO()
    }
}