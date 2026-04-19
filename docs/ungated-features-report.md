# Ungated Features Report

Features that were premium-locked and are now free for all users.  
The **Gatekeeper** column shows the exact check that blocked access.  
The **Kept Function** column shows what now runs unconditionally.

---

## 1. `paidAction()` Call Sites

The central gatekeeper was `NavController.paidAction(action: IPaidAction, block: () -> Unit)`.  
It checked `UserSubscriptionManager.isActionAllowed(action)` — if **false**, it showed `DefenseSystemFeatureDialog` and never ran the block. We removed the wrapper and kept the block contents.

### Advanced Search — `paidAction(AdvancedSearch)`

| # | File | Kept Function | What It Does |
|---|------|--------------|--------------|
| 1 | `MarketFiltersFragment.kt` | `slideFromBottom(R.id.sectorsSelectorFragment)` | Open Sectors filter |
| 2 | `MarketFiltersFragment.kt` | `showBottomSheet(PriceChange)` | Open Price Change filter |
| 3 | `MarketFiltersFragment.kt` | `showBottomSheet(PricePeriod)` | Open Price Period filter |
| 4 | `MarketFiltersFragment.kt` | `showBottomSheet(TradingSignals)` | Open Trading Signals filter |
| 5 | `MarketFiltersFragment.kt` | `showBottomSheet(PriceCloseTo)` | Open Price Close To filter |
| 6 | `MarketFiltersFragment.kt` | `viewModel.updateOutperformedBtcOn(it)` | Toggle Outperformed BTC |
| 7 | `MarketFiltersFragment.kt` | `viewModel.updateOutperformedEthOn(it)` | Toggle Outperformed ETH |
| 8 | `MarketFiltersFragment.kt` | `viewModel.updateOutperformedBnbOn(it)` | Toggle Outperformed BNB |
| 9 | `MarketFiltersFragment.kt` | `viewModel.updateOutperformedSnpOn(it)` | Toggle Outperformed S&P 500 |
| 10 | `MarketFiltersFragment.kt` | `viewModel.updateOutperformedGoldOn(it)` | Toggle Outperformed Gold |
| 11 | `MarketFiltersFragment.kt` | `viewModel.updateSolidCexOn(it)` | Toggle Good CEX Volume |
| 12 | `MarketFiltersFragment.kt` | `viewModel.updateSolidDexOn(it)` | Toggle Good DEX Volume |
| 13 | `MarketFiltersFragment.kt` | `viewModel.updateGoodDistributionOn(it)` | Toggle Good Distribution |
| 14 | `MarketFiltersFragment.kt` | `viewModel.updateListedOnTopExchangesOn(it)` | Toggle Listed on Top Exchanges |

### Token Insights — `paidAction(TokenInsights)`

| # | File | Kept Function | What It Does |
|---|------|--------------|--------------|
| 15 | `EarnScreen.kt` | `slideFromRight(R.id.vaultFragment, input)` | Open vault detail |
| 16 | `EarnScreen.kt` | `viewModel.onFilterBySelected(selected)` | Change vault Filter By |
| 17 | `EarnScreen.kt` | `viewModel.onApyPeriodSelected(selected)` | Change APY Period |
| 18 | `EarnScreen.kt` | `viewModel.onSortingSelected(selected)` | Change vault Sorting |
| 19 | `VaultBlockchainsSelectorFragment.kt` | `selectedBlockchains.add/remove(item)` | Toggle blockchain filter |
| 20 | `RoiSelectCoinsFragment.kt` | `dialog = PeriodSelectorDialog(text, period, i)` | Open period selector |
| 21 | `RoiSelectCoinsFragment.kt` | `viewModel.onToggle(item, !checked)` | Toggle coin selection |

### Trade Signals — `paidAction(TradeSignals)`

| # | File | Kept Function | What It Does |
|---|------|--------------|--------------|
| 22 | `MarketFavoritesScreen.kt` | `slideFromBottomForResult<MarketSignalsFragment.Result>()` | Open signals filter |

### Swap Protection — `paidAction(SwapProtection)` equivalent

| # | File | Kept Function | What It Does |
|---|------|--------------|--------------|
| 23 | `SwapConfirmFragment.kt` | MEV protection toggle (returns `true`) | Toggle swap protection on/off |

### Secure Send — `paidAction(SecureSend)`

| # | File | Kept Function | What It Does |
|---|------|--------------|--------------|
| 24 | `MainSettingsScreen.kt` | `slideFromRight(R.id.addressCheckFragment)` | Open Address Checker |
| 25 | `EnterAddressScreen.kt` | `slideFromBottom(R.id.secureSendConfigDialog)` | Open Secure Send config |

### Robbery Protection — `paidAction(RobberyProtection)`

| # | File | Kept Function | What It Does |
|---|------|--------------|--------------|
| 26 | `SecuritySettingsFragment.kt` | Navigate to duress PIN setup (`editDuressPinFragment` or `setDuressPinIntroFragment`) | Set up / edit duress PIN |

### Priority Support — `paidAction(PrioritySupport)`

| # | File | Kept Function | What It Does |
|---|------|--------------|--------------|
| 27 | `MainSettingsScreen.kt` | `LinkHelper.openLinkInAppBrowser(context, viewModel.vipSupportLink)` | Open VIP support chat |

---

## 2. `UserSubscriptionManager.isActionAllowed()` Checks

These weren't using `paidAction()` but checked the subscription directly in ViewModel/Service code.

| # | File | Gatekeeper | Kept Function | Effect |
|---|------|-----------|--------------|--------|
| 28 | `CoinAnalyticsViewModel.kt` | `!UserSubscriptionManager.isActionAllowed(TokenInsights)` → `showPreviewBlocks = true` → blurred analytics | `viewItem(data)` with `showAsPreview = false` | All 10 analytics blocks (DEX volume, liquidity, holders, fees, etc.) shown in full instead of blurred previews |
| 29 | `EnterAddressViewModel.kt` | `UserSubscriptionManager.isActionAllowed(SecureSend)` → `hasPremium` | `hasPremium = true` | Secure Send address validation always active |
| 30 | `EnterAddressViewModel.kt` | `UserSubscriptionManager.isActionAllowed(ScamProtection)` → if false, all checks return `NotAllowed` | Checks run normally | Address scam checks (sanctions, OFAC, phishing) run for all users |
| 31 | `EarnViewModel.kt` | `UserSubscriptionManager.isActionAllowed(TokenInsights)` → `hasPremium` → split list into visible + blurred | All vaults shown | Full vault list visible (was capped at `VISIBLE_ITEMS_NO_PREMIUM` + blurred extras) |
| 32 | `SignalsControlManager.kt` | `&& UserSubscriptionManager.isActionAllowed(TradeSignals)` | Condition removed | Trading signals always enabled |
| 33 | `SwapDefenseSystemService.kt` | `UserSubscriptionManager.isActionAllowed(SwapProtection)` → `actionAllowed` | `mevProtectionEnabled = true` | MEV protection defaults to on; always shows "Safe" defense message |
| 34 | `WCSessionViewModel.kt` | `UserSubscriptionManager.isActionAllowed(ScamProtection)` → `scamProtectionActionAllowed` | `scamProtectionEnabled = true` | WalletConnect DApp scam check always active |
| 35 | `WCSessionBottomSheet.kt` | `!scamProtectionActionAllowed` → showed lock icon | Shows `Secure` / `Risky` status | DApp whitelist status always visible (no lock overlay) |
| 36 | `SecuritySettingsFragment.kt` | `UserSubscriptionManager.isActionAllowed(action)` per defense action → `DefenseSystemFeatureDialog` | Direct toggle / navigation | Each defense feature toggleable without subscription check |

---

## 3. `activeSubscriptionStateFlow.collect` Observers

These subscription state observers reactively updated UI when subscription changed. All removed — features are now unconditionally active.

| # | File | What It Did |
|---|------|------------|
| 37 | `CoinAnalyticsViewModel.kt` | Re-evaluated `showPreviewBlocks` on subscription change |
| 38 | `CoinAnalyticsService.kt` | Re-fetched analytics data on subscription change |
| 39 | `EnterAddressViewModel.kt` | Re-ran address validation on subscription change |
| 40 | `EarnViewModel.kt` | Re-split vault list (visible vs blurred) on subscription change |
| 41 | `MarketFavoritesViewModel.kt` | Refreshed favorites on subscription change |
| 42 | `MainSettingsViewModel.kt` | Updated `showPremiumBanner` and `hasSubscription` on subscription change |
| 43 | `SwapDefenseSystemService.kt` | Re-evaluated MEV protection enabled state |
| 44 | `WCSessionViewModel.kt` | Updated `hasSubscription` and `scamProtectionActionAllowed` |
| 45 | `SecuritySettingsViewModel.kt` | Refreshed `defenseSystemActions` list |
| 46 | `App.kt` | Called `UserSubscriptionManager.onResume()`/`pause()` on foreground/background |

---

## Summary

| Gatekeeper Mechanism | Count | Now |
|---------------------|-------|-----|
| `paidAction()` wrapper (NavController extension) | 27 call sites | Block runs directly, no check |
| `UserSubscriptionManager.isActionAllowed()` | 9 checks | Hardcoded `true` or condition removed |
| `activeSubscriptionStateFlow.collect` | 10 observers | Removed — no reactive subscription tracking |
| **Total ungated sites** | **46** | |

All 8 paid action types (`AdvancedSearch`, `TokenInsights`, `TradeSignals`, `SwapProtection`, `SecureSend`, `ScamProtection`, `RobberyProtection`, `PrioritySupport`) are now free.
