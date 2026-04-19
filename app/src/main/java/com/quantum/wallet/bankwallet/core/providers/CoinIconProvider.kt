package com.quantum.wallet.bankwallet.core.providers

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URL

class CoinIconProvider(private val context: Context) {

    companion object {
        private const val TAG = "CoinIconProvider"
        private const val BASE_URL = "https://quantum-static-prod-s3-apse1.s3.ap-southeast-1.amazonaws.com/qwallet"
        private const val MANIFEST_URL = "$BASE_URL/manifest.json"
        private const val PREFS_NAME = "coin_icon_provider"
        private const val KEY_MANIFEST = "cached_manifest"
        private const val KEY_LAST_FETCH = "last_fetch_time"
        private const val CACHE_DURATION_MS = 6 * 60 * 60 * 1000L // 6 hours
    }

    private var manifest: IconManifest? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        loadCachedManifest()
        refreshManifestIfNeeded()
    }

    fun coinIconUrl(coinUid: String): String? {
        val m = manifest ?: return null
        val entry = m.coins.find { it.uid == coinUid } ?: return null
        return if (entry.url != null) {
            entry.url
        } else {
            "$BASE_URL/coin-icons/${m.defaultSize}/$coinUid.png"
        }
    }

    fun blockchainIconUrl(blockchainUid: String): String? {
        val m = manifest ?: return null
        val entry = m.blockchains.find { it.uid == blockchainUid } ?: return null
        return if (entry.url != null) {
            entry.url
        } else {
            "$BASE_URL/blockchain-icons/${m.defaultSize}/$blockchainUid.png"
        }
    }

    fun hasCoinIcon(coinUid: String): Boolean {
        return manifest?.coins?.any { it.uid == coinUid } == true
    }

    fun hasBlockchainIcon(blockchainUid: String): Boolean {
        return manifest?.blockchains?.any { it.uid == blockchainUid } == true
    }

    private fun loadCachedManifest() {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_MANIFEST, null)
            if (json != null) {
                manifest = Gson().fromJson(json, IconManifest::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load cached manifest", e)
        }
    }

    private fun refreshManifestIfNeeded() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastFetch = prefs.getLong(KEY_LAST_FETCH, 0L)
        val now = System.currentTimeMillis()
        if (now - lastFetch < CACHE_DURATION_MS && manifest != null) return

        coroutineScope.launch {
            try {
                val json = URL(MANIFEST_URL).readText()
                val newManifest = Gson().fromJson(json, IconManifest::class.java)
                manifest = newManifest
                prefs.edit()
                    .putString(KEY_MANIFEST, json)
                    .putLong(KEY_LAST_FETCH, now)
                    .apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch icon manifest", e)
            }
        }
    }

    data class IconManifest(
        @SerializedName("version") val version: Int = 1,
        @SerializedName("default_size") val defaultSize: String = "96px",
        @SerializedName("base_url") val baseUrl: String = BASE_URL,
        @SerializedName("coins") val coins: List<IconEntry> = emptyList(),
        @SerializedName("blockchains") val blockchains: List<IconEntry> = emptyList()
    )

    data class IconEntry(
        @SerializedName("uid") val uid: String,
        @SerializedName("name") val name: String? = null,
        @SerializedName("url") val url: String? = null
    )
}
