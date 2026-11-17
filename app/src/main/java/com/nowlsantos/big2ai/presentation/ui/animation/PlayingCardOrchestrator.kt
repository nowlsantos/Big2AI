package com.nowlsantos.big2ai.presentation.ui.animation

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import com.nowlsantos.big2ai.model.enums.PlayerPosition
import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.presentation.ui.components.CardView
import com.nowlsantos.big2ai.util.CARD_HEIGHT
import com.nowlsantos.big2ai.util.CARD_WIDTH
import kotlinx.coroutines.launch
import androidx.compose.animation.core.VectorConverter // NEW: Import VectorConverter
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.unit.dp
import com.nowlsantos.big2ai.model.data.LocalCoordinate
import kotlinx.coroutines.joinAll // NEW: Import joinAll for Job collections

@Composable
fun PlayingCardOrchestrator(
    animatingCards: List<PlayingCard>,
    layoutCoordinate: LocalCoordinate,
    animatingPlayerPosition: PlayerPosition?,
    onAnimationFinished: () -> Unit
) {
    val tag = "PlayingCardOrchestrator" // Define a tag for logging within this class
    val density = LocalDensity.current
    val cardAnimatables = remember(animatingCards) { mutableStateMapOf<PlayingCard, Animatable<Offset, AnimationVector2D>>() }

    LaunchedEffect(animatingCards, animatingPlayerPosition, layoutCoordinate) {
        if (animatingCards.isNotEmpty() && animatingPlayerPosition != null) {
            val initialPlayerAreaOffset = when (animatingPlayerPosition) {
                PlayerPosition.NORTH -> layoutCoordinate.northOffset
                PlayerPosition.SOUTH -> layoutCoordinate.southOffset
                PlayerPosition.EAST -> layoutCoordinate.eastOffset
                PlayerPosition.WEST -> layoutCoordinate.westOffset
            }

            val playerAreaSize = when (animatingPlayerPosition) {
                PlayerPosition.NORTH, PlayerPosition.SOUTH -> layoutCoordinate.horizontalSize
                PlayerPosition.EAST, PlayerPosition.WEST -> layoutCoordinate.verticalSize
            }

            // Calculate initial position for the card (center of the player's hand area)
            val startX = initialPlayerAreaOffset.x + (playerAreaSize.width / 2f) - (with(density) { CARD_WIDTH.toPx() / 2f })
            val startY = initialPlayerAreaOffset.y + (playerAreaSize.height / 2f) - (with(density) { CARD_HEIGHT.toPx() / 2f })
            val initialCardStartOffset = Offset(startX, startY)

            // Calculate target positions with spacing for multiple cards
            val spacingBetweenCardsPx = with(density) { 2.dp.toPx() } // Define spacing between cards
            val totalCards = animatingCards.size
            val totalWidthOfCardsPx = (totalCards * with(density) { CARD_WIDTH.toPx() }) + ((totalCards - 1) * spacingBetweenCardsPx)

            // Determine the starting X for the first card to center the whole group
            val startOfFirstCardTargetX = layoutCoordinate.centerOffset.x - (totalWidthOfCardsPx / 2f)

            val animations = animatingCards.mapIndexed { index, card ->
                val animatable = cardAnimatables.getOrPut(card) { Animatable(Offset.Zero, Offset.VectorConverter) }

                // Calculate the individual target X position for this card
                val individualTargetX = startOfFirstCardTargetX + (index * (with(density) { CARD_WIDTH.toPx() } + spacingBetweenCardsPx))
                val individualTargetY = layoutCoordinate.centerOffset.y - with(density) { CARD_HEIGHT.toPx() / 2f } // Y remains centered

                val individualTargetOffset = Offset(individualTargetX, individualTargetY)

                launch {
                    animatable.snapTo(initialCardStartOffset) // Ensure card starts from its initial position
                    animatable.animateTo(
                        targetValue = individualTargetOffset, // Animate to the spaced target
                        animationSpec = tween(
                            durationMillis = 500, // Duration of individual card animation
                            delayMillis = index * 80 // Stagger cards for a fan effect
                        )
                    )
                }
            }
            animations.joinAll() // Wait for all individual card animations to complete
            Log.d(tag, "PlayCardOrchestrator: All cards animated to center.")
            onAnimationFinished() // Signal ViewModel that animation is done
        } else {
            cardAnimatables.clear() // Clear animatables if animation state is off
        }
    }

    // Render animating cards
    Box(modifier = Modifier.fillMaxSize().zIndex(100f)) { // Higher zIndex to ensure cards are on top
        animatingCards.forEachIndexed { index, card ->
            val animatedOffset = cardAnimatables[card]?.value // Get current animated value
            if (animatedOffset != null) {
                val finalOffsetX = with(density) { animatedOffset.x.toDp() }
                val finalOffsetY = with(density) { animatedOffset.y.toDp() }

                CardView(
                    card = card.copy(isFaceUp = true),
                    isSelected = false,
                    faceUp = true,
                    onClick = null,
                    modifier = Modifier
                        .offset(x = finalOffsetX, y = finalOffsetY)
                        .zIndex(1.0f + index) // Ensure cards maintain order and are on top
                )
            }
        }
    }
}
