package com.devball.jubalaudio.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeDismissable(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    background: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val velocityTracker = remember { VelocityTracker() }

    Box(modifier = modifier) {
        background() //Render optional background behind the swipeable content

        //Actual foreground content
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { velocityTracker.resetTracking() },
                        onDragEnd = {
                            val velocity = velocityTracker.calculateVelocity().x
                            val targetWidth = size.width.toFloat()

                            //Dismiss if swiped 30% OR if flicked fast to the right
                            if (offsetX.value > targetWidth * 0.3f || velocity > 1000f) {
                                scope.launch {
                                    offsetX.animateTo(
                                        targetWidth,
                                        tween(150)
                                    )
                                    onDismiss()
                                }
                            } else {
                                scope.launch {
                                    offsetX.animateTo(
                                        0f,
                                        spring(stiffness = Spring.StiffnessLow)
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(0f, tween(200))
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val newOffset = (offsetX.value + dragAmount).coerceAtLeast(0f)
                            scope.launch { offsetX.snapTo(newOffset) }
                        }
                    )
                }
        ) {
            content()
        }
    }
}