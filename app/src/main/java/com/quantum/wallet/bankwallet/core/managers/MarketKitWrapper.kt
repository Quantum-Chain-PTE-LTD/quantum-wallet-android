package com.quantum.wallet.bankwallet.core.managers

import android.content.Context
import com.quantum.wallet.bankwallet.core.InvalidAuthTokenException
import com.quantum.wallet.bankwallet.core.NoAuthTokenException
import com.quantum.wallet.bankwallet.core.customCoinPrefix
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.SyncInfo
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.HsPeriodType
import io.horizontalsystems.marketkit.models.HsPointTimePeriod
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.horizontalsystems.marketkit.models.MarketInfo
import io.horizontalsystems.marketkit.models.MarketOverview
import io.horizontalsystems.marketkit.models.NftTopCollection
import io.horizontalsystems.marketkit.models.Stock
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TopMovers
import io.horizontalsystems.marketkit.models.TopPlatform
import io.horizontalsystems.marketkit.models.Vault
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.HttpException
import retrofit2.Response
import java.math.BigDecimal

// Quantum Wallet hides Quantum Chain coins/tokens from market data so that the in-app
// market views never reference the home blockchain. The user's own QC wallet flows
// (balance, send, receive) do not depend on MarketKitWrapper and are unaffected.
private val Blockchain.isQuantumChain: Boolean
    get() = type == BlockchainType.QuantumChain

private val Token.isQuantumChain: Boolean
    get() = blockchain.isQuantumChain

private val TopPlatform.isQuantumChain: Boolean
    get() = blockchain.isQuantumChain

private fun FullCoin.withoutQuantumChain(): FullCoin? {
    val filtered = tokens.filterNot { it.isQuantumChain }
    return when {
        filtered.isEmpty() -> null
        filtered.size == tokens.size -> this
        else -> copy(tokens = filtered)
    }
}

private fun MarketInfo.withoutQuantumChain(): MarketInfo? {
    val filteredFullCoin = fullCoin.withoutQuantumChain() ?: return null
    return if (filteredFullCoin === fullCoin) this else copy(fullCoin = filteredFullCoin)
}

// Synthetic Blockchain entry for Quantum Chain. MarketKit's database does not contain
// the home blockchain, so wallet storage / lookups (e.g. WalletStorage reconstructing
// custom QRC20 wallets, contacts, settings) fall back to this hardcoded value.
private val quantumChainBlockchain: Blockchain = Blockchain(
    type = BlockchainType.QuantumChain,
    name = "Quantum Chain",
    eip3091url = null
)

class MarketKitWrapper(
    context: Context,
    hsApiBaseUrl: String,
    hsApiKey: String,
    newsApiKey: String,
    qcApiBaseUrl: String,
    qcApiKey: String,
) {

    private val marketKit: MarketKit by lazy {
        MarketKit.getInstance(
            context = context,
            hsApiBaseUrl = hsApiBaseUrl,
            hsApiKey = hsApiKey,
            newsApiKey = newsApiKey,
            qcApiBaseUrl = qcApiBaseUrl,
            qcApiKey = qcApiKey,
        )
    }

    private fun <T> requestWithAuthToken(f: (String) -> Single<T>): Single<T> =
        Single.error(NoAuthTokenException())

    // Coins

    val fullCoinsUpdatedObservable: Observable<Unit>
        get() = marketKit.fullCoinsUpdatedObservable

    fun topFullCoins(limit: Int = 20): List<FullCoin> =
        marketKit.topFullCoins(limit)

    fun fullCoins(filter: String, limit: Int = 20): List<FullCoin> =
        marketKit.fullCoins(filter, limit)

    fun fullCoins(coinUids: List<String>): List<FullCoin> =
        marketKit.fullCoins(coinUids)

    fun fullCoinsByCoinCode(coinCodes: List<String>): List<FullCoin> =
        marketKit.fullCoinsByCoinCodes(coinCodes)

    fun allCoins() = marketKit.allCoins()

    fun token(query: TokenQuery): Token? = marketKit.token(query)

    fun tokens(queries: List<TokenQuery>): List<Token> = marketKit.tokens(queries)

    fun tokens(reference: String): List<Token> = marketKit.tokens(reference)

    fun tokens(blockchainType: BlockchainType, filter: String, limit: Int = 20): List<Token> {
        if (blockchainType == BlockchainType.QuantumChain) return emptyList()
        return marketKit.tokens(blockchainType, filter, limit)
    }

    fun allBlockchains(): List<Blockchain> =
        marketKit.allBlockchains().filterNot { it.isQuantumChain }

    fun blockchains(uids: List<String>): List<Blockchain> {
        val result = marketKit.blockchains(uids).toMutableList()
        if (BlockchainType.QuantumChain.uid in uids && result.none { it.type == BlockchainType.QuantumChain }) {
            result.add(quantumChainBlockchain)
        }
        return result
    }

    fun blockchain(uid: String): Blockchain? {
        return marketKit.blockchain(uid)
            ?: if (uid == BlockchainType.QuantumChain.uid) quantumChainBlockchain else null
    }

    fun marketInfosSingle(top: Int, currencyCode: String, defi: Boolean): Single<List<MarketInfo>> =
        marketKit.marketInfosSingle(top, currencyCode, defi).map { list -> list.mapNotNull { it.withoutQuantumChain() } }

    fun categoriesSingle() = marketKit.categoriesSingle()

    fun advancedMarketInfosSingle(top: Int = 250, currencyCode: String): Single<List<MarketInfo>> =
        marketKit.advancedMarketInfosSingle(top, currencyCode).map { list -> list.mapNotNull { it.withoutQuantumChain() } }

    fun marketInfosSingle(coinUids: List<String>, currencyCode: String): Single<List<MarketInfo>> =
        marketKit.marketInfosSingle(coinUids.removeCustomCoins(), currencyCode)

    fun marketInfosSingle(categoryUid: String, currencyCode: String): Single<List<MarketInfo>> =
        marketKit.marketInfosSingle(categoryUid, currencyCode).map { list -> list.mapNotNull { it.withoutQuantumChain() } }

    fun marketInfoOverviewSingle(
        coinUid: String,
        currencyCode: String,
        language: String,
        roiUids: List<String>,
        roiPeriods: List<HsTimePeriod>
    ) = marketKit.marketInfoOverviewSingle(coinUid, currencyCode, language, roiUids, roiPeriods)

    fun analyticsSingle(coinUid: String, currencyCode: String) =
        requestWithAuthToken { marketKit.analyticsSingle(it, coinUid, currencyCode) }

    fun analyticsPreviewSingle(coinUid: String, addresses: List<String>) = marketKit.analyticsPreviewSingle(coinUid, addresses)

    fun marketInfoTvlSingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod) =
        marketKit.marketInfoTvlSingle(coinUid, currencyCode, timePeriod)

    fun marketInfoGlobalTvlSingle(chain: String, currencyCode: String, timePeriod: HsTimePeriod) =
        marketKit.marketInfoGlobalTvlSingle(chain, currencyCode, timePeriod)

    fun defiMarketInfosSingle(currencyCode: String) =
        marketKit.defiMarketInfosSingle(currencyCode).map { list ->
            list.mapNotNull { info ->
                val fullCoin = info.fullCoin
                if (fullCoin == null) {
                    info
                } else {
                    val filtered = fullCoin.withoutQuantumChain() ?: return@mapNotNull null
                    if (filtered === fullCoin) info else info.copy(fullCoin = filtered)
                }
            }
        }

    // Categories

    fun coinCategoriesSingle(currencyCode: String) = marketKit.coinCategoriesSingle(currencyCode)

    fun coinCategoryMarketPointsSingle(categoryUid: String, interval: HsTimePeriod, currencyCode: String) =
        marketKit.coinCategoryMarketPointsSingle(categoryUid, interval, currencyCode)

    fun sync() = marketKit.sync()

    // Coin Prices

    private val String.isCustomCoin: Boolean
        get() = startsWith(TokenQuery.customCoinPrefix)

    private fun List<String>.removeCustomCoins(): List<String> = filterNot { it.isCustomCoin }

    fun refreshCoinPrices(currencyCode: String) = marketKit.refreshCoinPrices(currencyCode)

    fun coinPrice(coinUid: String, currencyCode: String): CoinPrice? =
        if (coinUid.isCustomCoin) null else marketKit.coinPrice(coinUid, currencyCode)

    fun coinPriceMap(coinUids: List<String>, currencyCode: String): Map<String, CoinPrice> {
        val coinUidsNoCustom = coinUids.removeCustomCoins()
        return when {
            coinUidsNoCustom.isEmpty() -> mapOf()
            else -> marketKit.coinPriceMap(coinUidsNoCustom, currencyCode)
        }
    }

    fun coinPriceObservable(tag: String, coinUid: String, currencyCode: String): Observable<CoinPrice> =
        if (coinUid.isCustomCoin) Observable.never() else marketKit.coinPriceObservable(tag, coinUid, currencyCode)

    fun coinPriceMapObservable(tag: String, coinUids: List<String>, currencyCode: String): Observable<Map<String, CoinPrice>> {
        val coinUidsNoCustom = coinUids.removeCustomCoins()
        return when {
            coinUidsNoCustom.isEmpty() -> Observable.never()
            else -> marketKit.coinPriceMapObservable(tag, coinUidsNoCustom, currencyCode)
        }
    }

    // Coin Historical Price

    fun coinHistoricalPriceSingle(coinUid: String, currencyCode: String, timestamp: Long): Single<BigDecimal> =
        if (coinUid.isCustomCoin) Single.never() else marketKit.coinHistoricalPriceSingle(coinUid, currencyCode, timestamp)

    fun coinHistoricalPrice(coinUid: String, currencyCode: String, timestamp: Long) =
        if (coinUid.isCustomCoin) null else marketKit.coinHistoricalPrice(coinUid, currencyCode, timestamp)

    // Posts

    fun postsSingle() = marketKit.postsSingle()

    // Market Tickers

    fun marketTickersSingle(coinUid: String, currencyCode: String) = marketKit.marketTickersSingle(coinUid, currencyCode)

    // Details

    fun tokenHoldersSingle(coinUid: String, blockchainUid: String) =
        requestWithAuthToken { marketKit.tokenHoldersSingle(it, coinUid, blockchainUid) }

    fun treasuriesSingle(coinUid: String, currencyCode: String) = marketKit.treasuriesSingle(coinUid, currencyCode)

    fun investmentsSingle(coinUid: String) = marketKit.investmentsSingle(coinUid)

    fun coinReportsSingle(coinUid: String) = marketKit.coinReportsSingle(coinUid)

    // Pro Details

    fun cexVolumesSingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod) =
        marketKit.cexVolumesSingle(coinUid, currencyCode, timePeriod)

    fun dexLiquiditySingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod) =
        requestWithAuthToken { marketKit.dexLiquiditySingle(it, coinUid, currencyCode, timePeriod) }

    fun dexVolumesSingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod) =
        requestWithAuthToken { marketKit.dexVolumesSingle(it, coinUid, currencyCode, timePeriod) }

    fun transactionDataSingle(coinUid: String, timePeriod: HsTimePeriod, platform: String?) =
        requestWithAuthToken { marketKit.transactionDataSingle(it, coinUid, timePeriod, platform) }

    fun activeAddressesSingle(coinUid: String, timePeriod: HsTimePeriod) =
        requestWithAuthToken { marketKit.activeAddressesSingle(it, coinUid, timePeriod) }

    fun cexVolumeRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.cexVolumeRanksSingle(it, currencyCode) }

    fun dexVolumeRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.dexVolumeRanksSingle(it, currencyCode) }

    fun dexLiquidityRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.dexLiquidityRanksSingle(it, currencyCode) }

    fun activeAddressRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.activeAddressRanksSingle(it, currencyCode) }

    fun transactionCountsRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.transactionCountsRanksSingle(it, currencyCode) }

    fun revenueRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.revenueRanksSingle(it, currencyCode) }

    fun feeRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.feeRanksSingle(it, currencyCode) }

    fun holdersRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.holderRanksSingle(it, currencyCode) }

    // Overview

    fun marketOverviewSingle(currencyCode: String): Single<MarketOverview> =
        marketKit.marketOverviewSingle(currencyCode).map { overview ->
            overview.copy(
                topPlatforms = overview.topPlatforms.filterNot { it.isQuantumChain }
            )
        }

    fun marketGlobalSingle(currencyCode: String) = marketKit.marketGlobalSingle(currencyCode)

    fun topPairsSingle(currencyCode: String, page: Int, limit: Int) = marketKit.topPairsSingle(currencyCode, page, limit)

    fun topMoversSingle(currencyCode: String): Single<TopMovers> =
        marketKit.topMoversSingle(currencyCode).map { movers ->
            movers.copy(
                gainers100 = movers.gainers100.mapNotNull { it.withoutQuantumChain() },
                gainers200 = movers.gainers200.mapNotNull { it.withoutQuantumChain() },
                gainers300 = movers.gainers300.mapNotNull { it.withoutQuantumChain() },
                losers100 = movers.losers100.mapNotNull { it.withoutQuantumChain() },
                losers200 = movers.losers200.mapNotNull { it.withoutQuantumChain() },
                losers300 = movers.losers300.mapNotNull { it.withoutQuantumChain() }
            )
        }

    fun topCoinsMarketInfosSingle(top: Int, currencyCode: String): Single<List<MarketInfo>> =
        marketKit.topCoinsMarketInfosSingle(top, currencyCode).map { list -> list.mapNotNull { it.withoutQuantumChain() } }

    // Chart Info

    fun chartStartTimeSingle(coinUid: String) = marketKit.chartStartTimeSingle(coinUid)

    fun chartPointsSingle(coinUid: String, currencyCode: String, periodType: HsPeriodType) =
        marketKit.chartPointsSingle(coinUid, currencyCode, periodType)

    fun chartPointsSingle(coinUid: String, currencyCode: String, period: HsPointTimePeriod, pointCount: Int) =
        marketKit.chartPointsSingle(coinUid, currencyCode, period, pointCount)

    // Global Market Info

    fun globalMarketPointsSingle(currencyCode: String, timePeriod: HsTimePeriod) =
        marketKit.globalMarketPointsSingle(currencyCode, timePeriod)

    fun topPlatformsSingle(currencyCode: String): Single<List<TopPlatform>> =
        marketKit.topPlatformsSingle(currencyCode).map { list -> list.filterNot { it.isQuantumChain } }

    fun topPlatformMarketCapStartTimeSingle(platform: String) =
        marketKit.topPlatformMarketCapStartTimeSingle(platform)

    fun topPlatformMarketCapPointsSingle(
        chain: String,
        currencyCode: String,
        periodType: HsPeriodType
    ) = marketKit.topPlatformMarketCapPointsSingle(chain, currencyCode, periodType)

    fun topPlatformCoinListSingle(chain: String, currencyCode: String): Single<List<MarketInfo>> {
        if (chain == BlockchainType.QuantumChain.uid) return Single.just(emptyList())
        return marketKit.topPlatformMarketInfosSingle(chain, currencyCode)
            .map { list -> list.mapNotNull { it.withoutQuantumChain() } }
    }

    fun getCoinSignalsSingle(coinUids: List<String>) = marketKit.coinsSignalsSingle(coinUids)

    // NFT

    suspend fun nftCollections(): List<NftTopCollection> =
        marketKit.nftTopCollections()

    fun subscriptionsSingle(addresses: List<String>) =
        marketKit.subscriptionsSingle(addresses)

    fun authGetSignMessage(address: String) =
        marketKit.authGetSignMessage(address)

    fun authenticate(signature: String, address: String) =
        marketKit.authenticate(signature, address)

    // Misc

    fun syncInfo(): SyncInfo {
        return marketKit.syncInfo()
    }

    fun requestPersonalSupport(username: String): Single<Response<Void>> =
        requestWithAuthToken { marketKit.requestPersonalSupport(it, username) }

    fun requestVipSupport(subscriptionId: String): Single<Map<String, String>> =
        requestWithAuthToken { marketKit.requestVipSupport(it, subscriptionId) }

    fun getStocks(currencyCode: String): Single<List<Stock>> = marketKit.getStocks(currencyCode)

    // Stats

    fun sendStats(stats: String, appVersion: String, appId: String?): Single<Unit> {
        return marketKit.sendStats(stats, appVersion, appId)
    }

    // Etf

    fun etfs(category: String, currencyCode: String) = marketKit.etfSingle(category, currencyCode)

    fun etfPoints(category: String, currencyCode: String, period: String) = marketKit.etfPointSingle(category, currencyCode, period)

    // Vaults

    fun vaults(currencyCode: String): Single<List<Vault>> {
        return requestWithAuthToken { marketKit.vaultsSingle(currencyCode) }
    }

    fun vault(address: String, currencyCode: String, periodType: HsTimePeriod): Single<Vault> {
        return requestWithAuthToken { marketKit.vaultSingle(address, currencyCode, periodType) }
    }

}
