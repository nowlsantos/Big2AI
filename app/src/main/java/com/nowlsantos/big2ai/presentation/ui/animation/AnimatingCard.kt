package com.nowlsantos.big2ai.presentation.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.rotate
import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.presentation.ui.components.BackFaceView

@Composable
fun AnimatingCard(
    card: PlayingCard,
    initialOffset: Offset, // Starting position for animation
    targetOffset: Offset,  // Ending position for animation
    modifier: Modifier = Modifier,
    faceUp: Boolean, // Whether the card should be face up or down
    rotationDegrees: Float = 0f, // NEW PARAMETER: Default to 0 degrees for no rotation
    animationSpec: AnimationSpec<Offset> = tween(durationMillis = 500), // Default animation spec
    onAnimationEnd: (() -> Unit)? = null, // Callback when animation finishes
) {
    // Create and remember an Animatable instance for the offset
    // This allows us to animate the card's position
    val animatedOffset = remember { Animatable(initialOffset, Offset.VectorConverter) }

    LaunchedEffect(targetOffset, initialOffset) {
        // When the targetOffset or initialOffset changes (e.g., a new card is to be animated),
        // we first snap the animation's current value to the initialOffset.
        // This ensures the animation always starts from the specified initial point.
        animatedOffset.snapTo(initialOffset)

        // Then, we animate from the current (snapped) position to the targetOffset
        // using the provided animationSpec.
        animatedOffset.animateTo(
            targetValue = targetOffset,
            animationSpec = animationSpec
        )

        // After the animation completes, invoke the onAnimationEnd callback.
        onAnimationEnd?.invoke()
    }

    // The actual card rendering, positioned using the current value of the animatedOffset.
    // Apply the rotationDegrees BEFORE the offset.
    Box(
        modifier = modifier
            .offset(x = animatedOffset.value.x.dp, y = animatedOffset.value.y.dp)
            .rotate(rotationDegrees) // APPLY THE ROTATION HERE
    ) {
        BackFaceView()
    }
}