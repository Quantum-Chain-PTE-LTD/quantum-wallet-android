package com.quantum.wallet.bankwallet.uiv3.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private const val MIN_DRAG_AMOUNT = 3

@SuppressLint("UnusedTransitionTargetStateParameter")
@Composable
fun DraggableCardSimple(
    key: Any?,
    isRevealed: Boolean,
    cardOffset: Float,
    onReveal: () -> Unit,
    onConceal: () -> Unit,
    content: @Composable () -> Unit,
) {
    val transitionState = remember {
        MutableTransitionState(isRevealed).apply {
            targetState = !isRevealed
        }
    }
    val transition = rememberTransition(transitionState, "cardTransition")

    val offsetInPx = with(LocalDensity.current) { cardOffset.dp.toPx() }

    val offsetTransition by transition.animateFloat(
        label = "cardOffsetTransition",
        transitionSpec = { tween(durationMillis = 200) },
        targetValueByState = { if (isRevealed) -offsetInPx else 0f },
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(offsetTransition.roundToInt(), 0) }
            .pointerInput(key) {
                detectHorizontalDragGestures { _, dragAmount ->
                    when {
                        dragAmount <= -MIN_DRAG_AMOUNT -> onReveal()
                        dragAmount > MIN_DRAG_AMOUNT -> onConceal()
                    }
                }
            },
    ) {
        content()
    }
}

@Composable
fun ActionsRow(
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.End
    ) {
        content()
    }
}

fun getShape(itemsCount: Int, index: Int): Shape = when {
    itemsCount == 1 -> RoundedCornerShape(16.dp)
    itemsCount - 1 == index -> RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp)
    0 == index -> RoundedCornerShape(16.dp, 16.dp, 0.dp, 0.dp)
    else -> RoundedCornerShape(0.dp)
}

fun showDivider(itemsCount: Int, index: Int): Boolean = when {
    itemsCount == 1 || index == 0 -> false
    else -> true
}
