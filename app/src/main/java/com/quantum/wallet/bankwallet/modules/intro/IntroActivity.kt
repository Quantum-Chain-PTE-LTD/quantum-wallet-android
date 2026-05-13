package com.quantum.wallet.bankwallet.modules.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseActivity
import com.quantum.wallet.bankwallet.modules.main.MainModule
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme

class IntroActivity : BaseActivity() {

    val viewModel by viewModels<IntroViewModel> { IntroModule.Factory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ComposeAppTheme {
                SplashScreen()
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            viewModel.onStartClicked()
            MainModule.start(this)
            finish()
        }, 2000)
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, IntroActivity::class.java)
            context.startActivity(intent)
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeAppTheme.colors.tyler),
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = Modifier.size(200.dp),
            painter = painterResource(R.drawable.ic_company_logo),
            contentDescription = null,
        )
    }
}
