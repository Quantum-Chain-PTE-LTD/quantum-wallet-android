package com.quantum.wallet.bankwallet.modules.addtoken

import com.quantum.wallet.bankwallet.core.customCoinUid
import com.quantum.wallet.bankwallet.modules.addtoken.AddTokenModule.IAddTokenBlockchainService
import com.quantum.qrc20kit.core.Qip20Provider
import com.quantum.quantumkit.core.AddressValidator
import com.quantum.quantumkit.models.Address
import com.quantum.quantumkit.models.RpcSource
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.rx2.await

class AddQuantumTokenBlockchainService(
    private val blockchain: Blockchain,
    private val qip20Provider: Qip20Provider
) : IAddTokenBlockchainService {

    override fun isValid(reference: String): Boolean {
        return try {
            AddressValidator.validate(reference)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun tokenQuery(reference: String): TokenQuery {
        return TokenQuery(blockchain.type, TokenType.Qrc20(reference.lowercase()))
    }

    override suspend fun token(reference: String): Token {
        val tokenInfo = qip20Provider.getTokenInfo(Address(reference)).await()
        val tokenQuery = tokenQuery(reference)
        return Token(
            coin = Coin(
                uid = tokenQuery.customCoinUid,
                name = tokenInfo.tokenName,
                code = tokenInfo.tokenSymbol
            ),
            blockchain = blockchain,
            type = tokenQuery.tokenType,
            decimals = tokenInfo.tokenDecimal
        )
    }

    companion object {
        fun getInstance(blockchain: Blockchain): AddQuantumTokenBlockchainService {
            val qip20Provider = Qip20Provider.instance(RpcSource.quantumRpcHttp())
            return AddQuantumTokenBlockchainService(blockchain, qip20Provider)
        }
    }
}
