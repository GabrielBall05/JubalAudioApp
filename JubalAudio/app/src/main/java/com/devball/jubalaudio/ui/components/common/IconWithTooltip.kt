package com.devball.jubalaudio.ui.components.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import kotlinx.coroutines.launch

sealed interface IconPulseEffect {
    data object Scaling: IconPulseEffect
    data object Transparency: IconPulseEffect
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconWithTooltip(
    tooltipText: String,
    pulseEffect: IconPulseEffect? = null,
    icon: @Composable (() -> Unit)
) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val infiniteTransition = rememberInfiniteTransition(label = "icon_pulse_effect")

    //If enabled, set up pulse effect
    val animationModifier = when (pulseEffect) {
        is IconPulseEffect.Scaling -> {
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale_animation"
            )
            Modifier.scale(scale)
        }
        is IconPulseEffect.Transparency -> {
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.7f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha_animation"
            )
            Modifier.alpha(alpha)
        }
        null -> Modifier
    }

    //Custom position provider to display tooltip to the right of the anchor (icon)
    val rightPositionProvider = remember(density) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val spacing = with(density) { 0.dp.roundToPx() }
                val x = anchorBounds.right + spacing
                val y = anchorBounds.top + (anchorBounds.height - popupContentSize.height) / 2
                return IntOffset(x, y)
            }
        }
    }

    TooltipBox(
        positionProvider = rightPositionProvider,
        state = tooltipState,
        tooltip = {
            PlainTooltip(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    text = tooltipText,
                    textAlign = TextAlign.Center
                )
            }
        }
    ) {
        IconButton(
            onClick = {
                coroutineScope.launch {
                    tooltipState.show()
                }
            }
        ) {
            Box(modifier = animationModifier) {
                icon()
            }
        }
    }
}