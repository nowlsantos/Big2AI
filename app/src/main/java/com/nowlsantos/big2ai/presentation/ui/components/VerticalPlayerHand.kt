package com.nowlsantos.big2ai.presentation.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nowlsantos.big2ai.model.enums.GamePhase
import com.nowlsantos.big2ai.model.enums.PlayerPosition
import com.nowlsantos.big2ai.model.data.PlayerUiState
import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.util.*

@Composable
fun VerticalPlayerHand(
    modifier: Modifier = Modifier,
    phase: GamePhase,
    player: PlayerUiState,
    animatingCards: List<PlayingCard> = emptyList(),
) {
    val cards = player.hand.filterNot { it in animatingCards }
    if (cards.isEmpty()) return

    val overlapOffset = CARD_HEIGHT * (1f - VERTICAL_OVERLAP_FRACTION)
    val totalVisualHeight = if (cards.isNotEmpty()) {
        CARD_HEIGHT + (overlapOffset * (cards.size - 1).coerceAtLeast(0))
    } else {
        0.dp
    }

    val rotation = when(player.position) {
        PlayerPosition.WEST -> 90f
        PlayerPosition.EAST -> -90f
        else -> 0f
    }

    val newIndex = if (player.position == PlayerPosition.EAST) -1 else 1

    Box(modifier = modifier.size(CARD_WIDTH, totalVisualHeight)) {
        cards.forEachIndexed { index, card ->
            val cardModifier = Modifier
                .offset(y = overlapOffset * index)
                .rotate(rotation)
                .zIndex(index.toFloat() * newIndex)

            if (phase == GamePhase.ROUND_END || phase == GamePhase.GAME_OVER) {
                CardView(
                    card = card,
                    modifier = cardModifier,
                    faceUp = true,
                )
            } else {
                BackFaceView(modifier = cardModifier,)
            }
        }
    }
}