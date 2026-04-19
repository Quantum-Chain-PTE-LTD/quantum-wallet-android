# Subscription & Premium Feature Removal Report

**Branch**: `chore/remove-subscriptions`  
**Commits**: `a0664ff71`, `a636a96a2`  
**Stats**: 106 files changed, 170 insertions, 4,966 deletions

---

## Part 1 — Deleted Files, Classes & Functions

### 1.1 Deleted Modules (entire directories)

| Module | Purpose |
|--------|---------|
| `subscriptions-core/` | Core subscription abstractions (`IPaidAction`, `UserSubscriptionManager`, `SubscriptionService`, `Subscription`, `HSPurchase`) |
| `subscriptions-dev/` | Dev-flavor subscription service implementation (`SubscriptionServiceDev`) |
| `subscriptions-fdroid/` | F-Droid flavor subscription service (`SubscriptionServiceFDroid`) |
| `subscriptions-google-play/` | Google Play billing integration (`SubscriptionServiceGooglePlay`) |

### 1.2 Deleted App Files (entire files)

| File | Purpose |
|------|---------|
| `modules/premium/DefenseSystemFeatureDialog.kt` | Dialog shown when user tried to use a premium-gated feature without subscription |
| `modules/settings/banners/SubscriptionBanner.kt` | Banner on Settings screen prompting users to subscribe |
| `modules/settings/subscription/SubscriptionFragment.kt` | Subscription management screen |
| `modules/settings/subscription/SubscriptionViewModel.kt` | ViewModel for subscription management |
| `modules/usersubscription/BuySubscriptionChoosePlanViewModel.kt` | ViewModel for plan selection dialog |
| `modules/usersubscription/BuySubscriptionHavHostFragment.kt` | Host fragment for purchase flow |
| `modules/usersubscription/BuySubscriptionModel.kt` | Data model mapping paid actions to display strings |
| `modules/usersubscription/BuySubscriptionViewModel.kt` | ViewModel for purchase flow |
| `modules/usersubscription/PremiumFeaturesDialog.kt` | Dialog listing all premium features |
| `modules/usersubscription/SelectPlanDialog.kt` | Subscription plan selection UI |
| `modules/usersubscription/ui/PremiumSubscribedDialog.kt` | "You're subscribed!" confirmation dialog |
| `modules/usersubscription/ui/UiElements.kt` | Shared premium UI helpers (gradient badges, `highlightText()`, etc.) |
| `modules/usersubscription/ui/UserReviewSlider.kt.kt` | User review/testimonial slider component |
| `core/managers/PaidActionSettingsManager.kt` | Manager tracking which paid actions the user had toggled on/off |

### 1.3 Deleted Functions & Composables (from files that still exist)

| File | Deleted Symbol | Type | Purpose |
|------|---------------|------|---------|
| `core/NavController.kt` | `paidAction()` | Extension fun | Central gate: checked `UserSubscriptionManager.isActionAllowed()`, ran action if allowed or showed `DefenseSystemFeatureDialog` if not |
| `ui/compose/components/Header.kt` | `PremiumHeader()` | Composable | Header with premium icon + title text, used above premium sections |
| `ui/compose/components/cell/CellUniversal.kt` | `SectionPremiumUniversalLawrence()` | Composable | Orange-gradient-bordered section container (`0xFFFFAA00` → `0xFFFE4A11`) |
| `ui/compose/components/cell/CellUniversal.kt` | `SectionPremiumUniversal()` | Composable (private) | Inner implementation of the gradient border section |
| `modules/coin/analytics/ui/Components.kt` | `PremiumBadge()` | Composable | Yellow gradient badge with "Premium" text shown on preview blocks |
| `modules/coin/analytics/ui/Components.kt` | `isPreview` parameter | Param | Parameter on `AnalyticsBlockHeader` that toggled PremiumBadge display |
| `modules/market/earn/EarnScreen.kt` | `PremiumContentMessage()` | Composable | Blurred content overlay with lock icon + "Unlock Premium" button |
| `core/stats/Types.kt` | `StatEvent.OpenPremium` | Data class | Analytics event for premium feature upsells |
| `core/stats/Types.kt` | `StatEvent.SubscribePremium` | Data class | Analytics event for subscription purchases |
| `core/stats/Types.kt` | `StatPremiumTrigger` | Enum class | 25 trigger values tracking which feature prompted an upsell |
| `core/stats/Types.kt` | `trialExpired` property | Property | Private computed property adding trial status to analytics |
| `core/App.kt` | `paidActionSettingsManager` | Field | `PaidActionSettingsManager` singleton |
| `core/App.kt` | `trialExpired` | Field | Boolean tracking trial expiration |
| `settings/security/passcode/SecuritySettingsViewModel.kt` | `DefenseSystemAction` | Data class | Wrapper pairing `IPaidAction` with enabled state |
| `settings/security/passcode/SecuritySettingsViewModel.kt` | `defenseSystemActions` | Field + refresh logic | List of toggleable defense actions with subscription awareness |
| `settings/security/passcode/SecuritySettingsViewModel.kt` | `setActionEnabled()` | Function | Delegated to `PaidActionSettingsManager` |
| `settings/main/MainSettingsViewModel.kt` | `showPremiumBanner` | Field | Controlled visibility of upsell banner on settings |
| `settings/main/MainSettingsViewModel.kt` | `hasSubscription` | Field | Tracked active subscription for UI display |
| `multiswap/SwapConfirmViewModel.kt` | `mevProtectionActionAllowed` | Field | Tracked whether swap protection was premium-unlocked |
| `multiswap/SwapDefenseSystemService.kt` | `refreshMevProtectionEnabled()` | Function | Computed MEV protection state from subscription + settings |
| `walletconnect/session/WCSessionViewModel.kt` | `hasSubscription` | Field | Tracked subscription for WC session UI |
| `walletconnect/session/WCSessionViewModel.kt` | `scamProtectionActionAllowed` | Field | Tracked whether scam protection was premium-unlocked |

### 1.4 Deleted String Resources

7 `Premium_*` strings removed from `values/strings.xml` + all localized copies (de, es, fr, ko, pt-rBR, ru, tr, zh):

| Key | Content |
|-----|---------|
| `Premium_TitleForDroid` | "Defense System" (F-Droid variant) |
| `Premium_Title` | "Premium" |
| `Premium_Upgrade` | Upgrade prompt text |
| `Premium_UpgradeFeature_RobberyProtection` | "Robbery Protection" |
| `Premium_UpgradeFeature_SecureSend` | "Secure Send" |
| `Premium_UpgradeFeature_RobberyProtection_Description` | Feature description |
| `Premium_DefenseSystem` | "Defense System" |

4 replacement `Settings_*` strings were added to keep feature labels working.

### 1.5 Deleted Navigation & Build Config

| Item | Detail |
|------|--------|
| `main_graph.xml` | Removed `defenseSystemFeatureDialog`, `buySubscriptionFragment`, `subscriptionFragment`, `premiumFeaturesDialog`, `selectPlanDialog`, `premiumSubscribedDialog` destinations |
| `app/build.gradle.kts` | Removed dependencies on `subscriptions-core`, `subscriptions-dev`, `subscriptions-fdroid`, `subscriptions-google-play` |
| `settings.gradle.kts` | Removed `include` for all 4 subscription modules |

---

## Part 2 — Features Ungated (Premium Check Removed, Action Preserved)

These functions previously required a subscription. Now they execute unconditionally.

### 2.1 Advanced Search (was gated by `AdvancedSearch`)

| File | Action | Was | Now |
|------|--------|-----|-----|
| `MarketFiltersFragment.kt` | Open Sectors selector | `paidAction(AdvancedSearch) { slideFromBottom(sectorsSelectorFragment) }` | Navigates directly |
| `MarketFiltersFragment.kt` | Open Price Change filter | `paidAction(AdvancedSearch) { showBottomSheet(PriceChange) }` | Shows directly |
| `MarketFiltersFragment.kt` | Open Price Period filter | `paidAction(AdvancedSearch) { showBottomSheet(PricePeriod) }` | Shows directly |
| `MarketFiltersFragment.kt` | Open Trading Signals filter | `paidAction(AdvancedSearch) { showBottomSheet(TradingSignals) }` | Shows directly |
| `MarketFiltersFragment.kt` | Open Price Close To filter | `paidAction(AdvancedSearch) { showBottomSheet(PriceCloseTo) }` | Shows directly |
| `MarketFiltersFragment.kt` | Toggle Outperformed BTC | `paidAction(AdvancedSearch) { updateOutperformedBtcOn() }` | Toggles directly |
| `MarketFiltersFragment.kt` | Toggle Outperformed ETH | `paidAction(AdvancedSearch) { updateOutperformedEthOn() }` | Toggles directly |
| `MarketFiltersFragment.kt` | Toggle Outperformed BNB | `paidAction(AdvancedSearch) { updateOutperformedBnbOn() }` | Toggles directly |
| `MarketFiltersFragment.kt` | Toggle Outperformed S&P 500 | `paidAction(AdvancedSearch) { updateOutperformedSnpOn() }` | Toggles directly |
| `MarketFiltersFragment.kt` | Toggle Outperformed Gold | `paidAction(AdvancedSearch) { updateOutperformedGoldOn() }` | Toggles directly |
| `MarketFiltersFragment.kt` | Toggle Good CEX Volume | `paidAction(AdvancedSearch) { updateSolidCexOn() }` | Toggles directly |
| `MarketFiltersFragment.kt` | Toggle Good DEX Volume | `paidAction(AdvancedSearch) { updateSolidDexOn() }` | Toggles directly |
| `MarketFiltersFragment.kt` | Toggle Good Distribution | `paidAction(AdvancedSearch) { updateGoodDistributionOn() }` | Toggles directly |
| `MarketFiltersFragment.kt` | Toggle Listed on Top Exchanges | `paidAction(AdvancedSearch) { updateListedOnTopExchangesOn() }` | Toggles directly |

### 2.2 Token Insights (was gated by `TokenInsights`)

| File | Action | Was | Now |
|------|--------|-----|-----|
| `CoinAnalyticsViewModel.kt` | Show analytics blocks | `showPreviewBlocks = !UserSubscriptionManager.isActionAllowed(TokenInsights)` → blurred previews | `showAsPreview = false` — all blocks fully visible |
| `CoinAnalyticsService.kt` | Refetch on subscription change | Collected `activeSubscriptionStateFlow` to re-fetch data | Removed — data loads once |
| `EarnScreen.kt` | Open vault detail | `paidAction(TokenInsights) { slideFromRight(vaultFragment) }` | Navigates directly |
| `EarnScreen.kt` | Change Filter By | `paidAction(TokenInsights) { onFilterBySelected() }` | Calls directly |
| `EarnScreen.kt` | Change APY Period | `paidAction(TokenInsights) { onApyPeriodSelected() }` | Calls directly |
| `EarnScreen.kt` | Change Sorting | `paidAction(TokenInsights) { onSortingSelected() }` | Calls directly |
| `EarnViewModel.kt` | Show all vaults | Split items into visible + blurred lists based on `hasPremium` | All items shown |
| `VaultBlockchainsSelectorFragment.kt` | Toggle blockchain filter | `paidAction(TokenInsights) { toggle selection }` | Toggles directly |
| `RoiSelectCoinsFragment.kt` | Open period selector | `paidAction(TokenInsights) { show dialog }` | Shows directly |
| `RoiSelectCoinsFragment.kt` | Toggle coin selection | `paidAction(TokenInsights) { onToggle() }` | Toggles directly |

### 2.3 Trade Signals (was gated by `TradeSignals`)

| File | Action | Was | Now |
|------|--------|-----|-----|
| `MarketFavoritesScreen.kt` | Open signals filter | `paidAction(TradeSignals) { slideFromBottomForResult() }` | Navigates directly |
| `MarketFavoritesViewModel.kt` | Refresh on subscription change | Collected `activeSubscriptionStateFlow` to refresh | Removed |
| `SignalsControlManager.kt` | Control signal visibility | `&& UserSubscriptionManager.isActionAllowed(TradeSignals)` | Condition removed — signals always available |

### 2.4 Swap Protection / MEV Protection (was gated by `SwapProtection`)

| File | Action | Was | Now |
|------|--------|-----|-----|
| `SwapConfirmFragment.kt` | Block MEV toggle without premium | Checked `mevProtectionActionAllowed`, showed `DefenseSystemFeatureDialog` if false | Toggle always allowed |
| `SwapConfirmViewModel.kt` | Track premium status for toggle | Passed `mevProtectionActionAllowed` in state | Field removed |
| `SwapDefenseSystemService.kt` | Compute MEV protection state | Combined `actionAllowed && actionEnabled` from subscription + settings | `mevProtectionEnabled` defaults to `true`, always shows "Safe" message |

### 2.5 Secure Send (was gated by `SecureSend`)

| File | Action | Was | Now |
|------|--------|-----|-----|
| `EnterAddressViewModel.kt` | Enable Secure Send logic | `hasPremium = UserSubscriptionManager.isActionAllowed(SecureSend)` | `hasPremium = true` |
| `EnterAddressScreen.kt` | Open config dialog | Checked `hasPremium` → opened config or upsell dialog | Opens config directly |
| `MainSettingsScreen.kt` | Open Address Checker | `paidAction(SecureSend) { slideFromRight(addressCheckFragment) }` | Navigates directly |
| `SecuritySettingsFragment.kt` | Defence System UI | Per-action subscription check with `DefenseSystemFeatureDialog` fallback | Direct navigation / toggle |

### 2.6 Scam Protection (was gated by `ScamProtection`)

| File | Action | Was | Now |
|------|--------|-----|-----|
| `EnterAddressViewModel.kt` | Run address checks | If no premium → all checks returned `NotAllowed` | All checks run normally |
| `WCSessionBottomSheet.kt` | Show DApp safety status | Showed lock icon if no premium; showed Secure/Risky if premium | Always shows Secure/Risky status |
| `WCSessionViewModel.kt` | Track scam protection state | `scamProtectionEnabled` from subscription + settings manager | `scamProtectionEnabled = true` |

### 2.7 Robbery Protection / Duress PIN (was gated by `RobberyProtection`)

| File | Action | Was | Now |
|------|--------|-----|-----|
| `SecuritySettingsFragment.kt` | Set up duress PIN | `paidAction(RobberyProtection) { navigate to pin setup }` | Navigates directly |

### 2.8 Priority Support (was gated by `PrioritySupport`)

| File | Action | Was | Now |
|------|--------|-----|-----|
| `MainSettingsScreen.kt` | Open VIP support link | `paidAction(PrioritySupport) { openLinkInAppBrowser() }` | Opens link directly |

---

## Part 3 — Relocated Utilities

| Symbol | From | To | Reason |
|--------|------|----|--------|
| `highlightText()` | `usersubscription/ui/UiElements.kt` | `multiswap/swapterms/SwapTermsFragment.kt` | Only remaining caller; moved as private function |
| `SectionPremiumUniversalLawrence` usage | Premium gradient border | `SectionUniversalLawrence` | Standard non-gradient section, already existed |

---

## Part 4 — Summary

| Metric | Count |
|--------|-------|
| Modules deleted | 4 |
| Files deleted | 14 app files + 40 module files |
| Call sites ungated (`paidAction()` removed) | 22+ |
| Subscription state observers removed | 8 `activeSubscriptionStateFlow.collect` blocks |
| Premium analytics events removed | 2 event types + 25 trigger values |
| String resources deleted | 7 keys × 9 locales = 63 entries |
| Premium UI composables deleted | 5 (`PremiumHeader`, `SectionPremiumUniversalLawrence`, `SectionPremiumUniversal`, `PremiumBadge`, `PremiumContentMessage`) |
| Navigation destinations removed | 6 |
| Net line change | -4,796 lines |
