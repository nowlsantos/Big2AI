package com.nowlsantos.big2ai.presentation.ui.animation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.zIndex
import com.nowlsantos.big2ai.model.enums.PlayerPosition
import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.model.enums.Suit
import com.nowlsantos.big2ai.model.enums.Rank
import com.nowlsantos.big2ai.util.CARD_HEIGHT
import com.nowlsantos.big2ai.util.CARD_WIDTH
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import com.nowlsantos.big2ai.model.data.LocalCoordinate
import com.nowlsantos.big2ai.model.data.PlayerUiState
import com.nowlsantos.big2ai.util.AppLogger
import com.nowlsantos.big2ai.util.TOTAL_CARDS
import kotlin.time.Duration.Companion.milliseconds

// Internal data class to hold animating card's state
private data class AnimatingCardInstance(
    val card: PlayingCard,
    val playerPosition: PlayerPosition, // Keep player position for rotation logic
    val playerIndexInHand: Int, // Keep player index in hand for target offset calculation
    val targetOffset: Offset,
    val faceUp: Boolean = false,
    val rotationDegrees: Float = 0f, // <-- IMPORTANT: NEW PROPERTY ADDED HERE
    val startTime: Long = System.currentTimeMillis(),
    val animationCompleted: MutableState<Boolean> = mutableStateOf(false)
)

@Composable
fun CardDealingOrchestrator(
    modifier: Modifier = Modifier, // Add modifier to allow external styling
    players: List<PlayerUiState>,
    screenWidth: Float,
    screenHeight: Float,
    localCoordinate: LocalCoordinate,
    key: Any? = null, // Add a generic key parameter for external control
    onAnimationFinished: () -> Unit,
) {
    val tag = "CardDealingOrchestrator" // Define a tag for logging within this class
    val density = LocalDensity.current
    val activeAnimatingCards = remember { mutableStateListOf<AnimatingCardInstance>() }

    // Calculate card dimensions in pixels
    val cardWidthPx = with(density) { CARD_WIDTH.toPx() }
    val cardHeightPx = with(density) { CARD_HEIGHT.toPx() }

    // Initial offset for all cards (center of the screen)
    val initialCardOffset = remember(screenWidth, screenHeight, cardWidthPx, cardHeightPx) {
        val xpos = screenWidth * 0.5f - cardWidthPx * 0.5f//  + 15.0f
        val ypos = screenHeight * 0.5f - cardHeightPx * 0.5f // + 30.0f
        Offset(xpos, ypos)
    }
    LaunchedEffect(key) {
        // Clear any previous animations (e.g., on recomposition or new deal)
        activeAnimatingCards.clear()

        // Keep track of how many cards each player has received during the animation
        val dealtCardsPerPlayer = mutableMapOf<PlayerPosition, Int>().apply {
            players.forEach { player -> put(player.position, 0) }
        }

        // Iterate through all 52 cards to simulate dealing
        var cardIndex = 0
        while (cardIndex < TOTAL_CARDS) {
            val playerIndexInLoop = cardIndex % players.size // Determine which player gets the card in this iteration
            val player = players[playerIndexInLoop]
            val cardIndexInPlayerHand = dealtCardsPerPlayer[player.position] ?: 0

            // Dummy card for animation purposes (actual card data is handled by GameState)
            val cardBeingDealt = PlayingCard(Rank.THREE, Suit.CLUBS)
            var rotationDegrees = 0f

            // Determine target offset for the card based on player position and its index in hand
            val targetOffsetForCard = when (player.position) {
                PlayerPosition.SOUTH -> {
                    rotationDegrees = 0f
                    calculateSouthPlayerCardPosition(cardIndexInPlayerHand, density, screenHeight, localCoordinate.horizontalSize)
                }

                PlayerPosition.WEST -> {
                    rotationDegrees = 90f
                    calculateWestPlayerCardPosition(cardIndexInPlayerHand, density, localCoordinate.verticalSize)
                }

                PlayerPosition.NORTH -> {
                    rotationDegrees = 0f
                    calculateNorthPlayerCardPosition(cardIndexInPlayerHand, density, localCoordinate,)
                }

                PlayerPosition.EAST -> {
                    rotationDegrees = -90f
                    calculateEastPlayerCardPosition(cardIndexInPlayerHand, density, screenWidth, localCoordinate.verticalSize)
                }
            }

            activeAnimatingCards.add(
                AnimatingCardInstance(
                    card = cardBeingDealt,
                    playerPosition = player.position,
                    playerIndexInHand = cardIndexInPlayerHand,
                    targetOffset = targetOffsetForCard,
                    //faceUp = player.isHuman, // Human player cards face up during deal
                    rotationDegrees = rotationDegrees
                )
            )

            dealtCardsPerPlayer[player.position] = cardIndexInPlayerHand + 1
            delay(120L.milliseconds) // Delay between dealing each card
            cardIndex++
        }

        // Wait for all individual card animations to report completion
        while (activeAnimatingCards.any { !it.animationCompleted.value }) {
            delay(60.milliseconds) // Poll frequently until all are done
        }

        // Ensure onAnimationFinished is called reliably after all cards are dealt and animated
        AppLogger.d(tag, "All individual card animations finished. Invoking onAnimationFinished.")
        activeAnimatingCards.clear() // Clear animating cards from display
        onAnimationFinished.invoke() // Signal ViewModel that animations are done
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Render all active animating cards
        activeAnimatingCards.forEachIndexed { index, cardInstance ->
            AnimatingCard(
                card = cardInstance.card,
                initialOffset = initialCardOffset,
                targetOffset = cardInstance.targetOffset,
                faceUp = cardInstance.faceUp,
                rotationDegrees = cardInstance.rotationDegrees, // PASS ROTATION FROM INSTANCE
                modifier = Modifier.zIndex(index.toFloat()), // Ensure correct Z-ordering
                onAnimationEnd = { cardInstance.animationCompleted.value = true }
            )
        }
    }
}

// Utility functions to calculate card positions (adjusted to take screen dimensions as parameters)
private fun calculateWestPlayerCardPosition(
    cardIndexInHand: Int,
    density: Density,
    verticalSize: IntSize,
): Offset {
    val overlapOffset = (CARD_HEIGHT * (0.25f)).toPx(density)
    val numberOfOverlaps = (12).coerceAtLeast(0).dp.toPx(density)
    val totalOverlapDistanceAbs = overlapOffset * numberOfOverlaps
    val totalHeight = (CARD_HEIGHT.toPx(density) + totalOverlapDistanceAbs)

    val startY = (verticalSize.height * 0.5f - totalHeight * 0.5f) * 0.5f
    val offsetX = (verticalSize.width * 0.5f) - CARD_WIDTH.toPx(density) + 18
    val offsetY = startY + (cardIndexInHand * overlapOffset) + 18
    return Offset(offsetX, offsetY)
}

private fun calculateEastPlayerCardPosition(
    cardIndexInHand: Int,
    density: Density,
    screenWidth: Float,
    verticalSize: IntSize,
): Offset {
    val overlapOffset = (CARD_HEIGHT * (0.25f)).toPx(density)
    val numberOfOverlaps = (12).coerceAtLeast(0).dp.toPx(density)
    val totalOverlapDistanceAbs = overlapOffset * numberOfOverlaps
    val totalHeight = (CARD_HEIGHT.toPx(density) + totalOverlapDistanceAbs)

    val startY = (verticalSize.height * 0.5f + totalHeight * 0.5f) * 0.5f
    val offsetX = (screenWidth - (verticalSize.width * 0.5f)) + 24
    val offsetY = startY - (cardIndexInHand * overlapOffset) + overlapOffset + 14
    return Offset(offsetX, offsetY)
}

private fun calculateNorthPlayerCardPosition(
    cardIndexInHand: Int,
    density: Density,
    localCoordinate: LocalCoordinate,
): Offset {
    val overlapOffset = (CARD_HEIGHT * (0.29f)).toPx(density)
    val numberOfOverlaps = (12).coerceAtLeast(0).dp.toPx(density)
    val totalOverlapDistanceAbs = overlapOffset * numberOfOverlaps
    val totalWidth = ((CARD_HEIGHT.toPx(density)) + totalOverlapDistanceAbs)
    val horizontalSize = localCoordinate.horizontalSize
    val verticalSize = localCoordinate.verticalSize

    val startX = (horizontalSize.width * 0.5f + totalWidth * 0.5f) - (verticalSize.width * 0.71f)
    val offsetX = startX - (cardIndexInHand * overlapOffset) + (CARD_WIDTH.toPx(density)) * 0.5f
    val offsetY = 24f
    return Offset(offsetX, offsetY)
}

private fun calculateSouthPlayerCardPosition(
    cardIndexInHand: Int,
    density: Density,
    screenHeight: Float,
    horizontalSize: IntSize,
): Offset {
    val overlapOffset = (CARD_HEIGHT * (0.29f)).toPx(density)
    val numberOfOverlaps = (12).coerceAtLeast(0).dp.toPx(density)
    val totalOverlapDistanceAbs = overlapOffset * numberOfOverlaps
    val totalWidth = ((CARD_HEIGHT.toPx(density)) + totalOverlapDistanceAbs)
    val startX = (horizontalSize.width * 0.5f - totalWidth * 0.5f) + CARD_WIDTH.toPx(density) * 1.58f
    val offsetX = startX + (cardIndexInHand * overlapOffset) + (CARD_WIDTH.toPx(density)) * 0.5f
    val offsetY = screenHeight - (horizontalSize.height * 0.25f)
    return Offset(offsetX, offsetY)
}

// Extension function to convert Dp to Px
private fun Dp.toPx(density: Density): Float {
    return with(density) { this@toPx.toPx() }
}