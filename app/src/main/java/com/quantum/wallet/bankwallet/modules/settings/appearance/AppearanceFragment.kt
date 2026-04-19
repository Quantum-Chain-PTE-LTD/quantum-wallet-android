package com.quantum.wallet.bankwallet.modules.settings.appearance

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.slideFromRight
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.uiv3.components.menu.MenuGroup
import com.quantum.wallet.bankwallet.uiv3.components.menu.MenuItemX
import com.quantum.wallet.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import com.quantum.wallet.bankwallet.ui.compose.components.HeaderText
import com.quantum.wallet.bankwallet.ui.compose.components.HsSwitch
import com.quantum.wallet.bankwallet.ui.compose.components.RowUniversal
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.body_leah
import com.quantum.wallet.bankwallet.ui.compose.components.subhead1_grey
import com.quantum.wallet.bankwallet.ui.compose.components.subhead1_leah
import com.quantum.wallet.bankwallet.ui.compose.components.subhead2_grey
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold

class AppearanceFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        AppearanceScreen(navController)
    }

}

@Composable
fun AppearanceScreen(navController: NavController) {
    val viewModel = viewModel<AppearanceViewModel>(factory = AppearanceModule.Factory())
    val uiState = viewModel.uiState

    var openLaunchPageSelector by rememberSaveable { mutableStateOf(false) }
    var openBalanceValueSelector by rememberSaveable { mutableStateOf(false) }
    var openPriceChangeIntervalSelector by rememberSaveable { mutableStateOf(false) }

    HSScaffold(
        title = stringResource(R.string.Settings_AppSettings),
        onBack = navController::popBackStack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            VSpacer(height = 12.dp)
            CellUniversalLawrenceSection(
                buildList {
                    add {
                        MenuItem(
                            R.string.Settings_Language,
                            value = uiState.currentLanguage,
                            onClick = {
                                navController.slideFromRight(R.id.languageSettingsFragment)

                                stat(
                                    page = StatPage.Settings,
                                    event = StatEvent.Open(StatPage.Language)
                                )
                            }
                        )
                    }
                    add {
                        MenuItem(
                            R.string.Settings_BaseCurrency,
                            value = uiState.baseCurrencyCode,
                            onClick = {
                                navController.slideFromRight(R.id.baseCurrencySettingsFragment)

                                stat(
                                    page = StatPage.Settings,
                                    event = StatEvent.Open(StatPage.BaseCurrency)
                                )
                            }
                        )
                    }
                }
            )

            VSpacer(24.dp)

            HeaderText(text = stringResource(id = R.string.Appearance_MarketsTab))
            CellUniversalLawrenceSection(
                listOf(
                    {
                        SettingUniversalCell(
                            title = R.string.Appearance_HideMarketsTab,
                            subtitle = R.string.Appearance_HideMarketsTab_Tip,
                        ) {
                            HsSwitch(
                                checked = uiState.marketsTabHidden,
                                onCheckedChange = {
                                    viewModel.onSetMarketTabsHidden(it)
                                }
                            )
                        }
                    },
                    {
                        SettingUniversalCell(
                            title = R.string.Appearance_PriceChangeInterval,
                            subtitle = R.string.Appearance_PriceChangeInterval_Tip,
                            onClick = { openPriceChangeIntervalSelector = true }
                        ) {
                            subhead1_leah(
                                text = uiState.priceChangeInterval.title.getString(),
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Image(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(id = R.drawable.ic_down_arrow_20),
                                contentDescription = null,
                            )
                        }
                    }
                )
            )

            AnimatedVisibility(visible = !uiState.marketsTabHidden) {
                Column {
                    VSpacer(32.dp)
                    CellUniversalLawrenceSection(
                        listOf {
                            SettingUniversalCell(
                                title = R.string.Settings_LaunchScreen,
                                subtitle = R.string.Settings_LaunchScreen_Tip,
                                onClick = { openLaunchPageSelector = true }
                            ) {
                                subhead1_leah(
                                    text = uiState.selectedLaunchScreen.title.getString(),
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                Image(
                                    modifier = Modifier.size(20.dp),
                                    painter = painterResource(id = R.drawable.ic_down_arrow_20),
                                    contentDescription = null,
                                )
                            }
                        }
                    )
                }
            }

            VSpacer(24.dp)
            HeaderText(text = stringResource(id = R.string.Appearance_BalanceTab))
            CellUniversalLawrenceSection(
                listOf(
                    {
                        SettingUniversalCell(
                            title = R.string.Appearance_HideBalanceTabButtons,
                            subtitle = R.string.Appearance_HideBalanceTabButtons_Tip,
                        ) {
                            HsSwitch(
                                checked = uiState.balanceTabButtonsHidden,
                                onCheckedChange = {
                                    viewModel.onSetBalanceTabButtonsHidden(it)
                                }
                            )
                        }
                    },
                    {
                        SettingUniversalCell(
                            title = R.string.Appearance_AmountRounding,
                            subtitle = R.string.Appearance_AmountRounding_Tip,
                        ) {
                            HsSwitch(
                                checked = uiState.amountRoundingEnabled,
                                onCheckedChange = {
                                    viewModel.onAmountRoundingToggle(it)
                                }
                            )
                        }
                    },
                    {
                        SettingUniversalCell(
                            title = R.string.Appearance_BalanceValue,
                            subtitle = R.string.Appearance_BalanceValue_Tip,
                            onClick = { openBalanceValueSelector = true }
                        ) {
                            subhead1_leah(
                                text = uiState.selectedBalanceViewType.title.getString(),
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Image(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(id = R.drawable.ic_down_arrow_20),
                                contentDescription = null,
                            )
                        }
                    }
                )
            )

            HeaderText(text = stringResource(id = R.string.Appearance_SendScreen))
            CellUniversalLawrenceSection(
                listOf(
                    {
                        SettingUniversalCell(
                            title = R.string.Appearance_RecentlySent,
                            subtitle = R.string.Appearance_RecentlySent_Tip,
                        ) {
                            HsSwitch(
                                checked = uiState.recentlySentEnabled,
                                onCheckedChange = {
                                    viewModel.enableRecentlySent(it)
                                }
                            )
                        }
                    }
                )
            )

            VSpacer(32.dp)
        }
        //Dialogs
        if (openLaunchPageSelector) {
            MenuGroup(
                title = stringResource(R.string.Settings_LaunchScreen),
                items = uiState.launchScreenOptions.options.map {
                    MenuItemX(it.title.getString(), it == uiState.launchScreenOptions.selected, it)
                },
                onDismissRequest = { openLaunchPageSelector = false },
                onSelectItem = { viewModel.onEnterLaunchPage(it) }
            )
        }
        if (openBalanceValueSelector) {
            MenuGroup(
                title = stringResource(R.string.Appearance_BalanceValue),
                items = uiState.balanceViewTypeOptions.options.map {
                    MenuItemX(it.title.getString(), it == uiState.balanceViewTypeOptions.selected, it)
                },
                onDismissRequest = { openBalanceValueSelector = false },
                onSelectItem = { viewModel.onEnterBalanceViewType(it) }
            )
        }
        if (openPriceChangeIntervalSelector) {
            MenuGroup(
                title = stringResource(R.string.Appearance_PriceChangeInterval),
                items = uiState.priceChangeIntervalOptions.options.map {
                    MenuItemX(it.title.getString(), it == uiState.priceChangeIntervalOptions.selected, it)
                },
                onDismissRequest = { openPriceChangeIntervalSelector = false },
                onSelectItem = { viewModel.onSetPriceChangeInterval(it) }
            )
        }

    }
}

@Composable
fun SettingUniversalCell(
    title: Int,
    subtitle: Int? = null,
    onClick: (() -> Unit)? = null,
    value: @Composable() (RowScope.() -> Unit),
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            body_leah(text = stringResource(title))
            subtitle?.let {
                VSpacer(height = 1.dp)
                subhead2_grey(text = stringResource(it))
            }
        }
        Row(
            content = value
        )
    }
}

@Composable
fun MenuItemWithDialog(
    @StringRes title: Int,
    value: String,
    onClick: () -> Unit
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = onClick
    ) {
        body_leah(
            text = stringResource(title),
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        subhead1_grey(
            text = value,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.ic_down_arrow_20),
            contentDescription = null,
        )
    }
}

@Composable
private fun MenuItem(
    @StringRes title: Int,
    value: String? = null,
    onClick: () -> Unit
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = onClick
    ) {
        body_leah(
            text = stringResource(title),
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        value?.let {
            subhead1_grey(
                text = value,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
        )
    }
}