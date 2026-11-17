package com.nowlsantos.big2ai.presentation.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nowlsantos.big2ai.model.enums.GamePhase
import com.nowlsantos.big2ai.model.data.PlayerUiState
import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.model.enums.PlayerPosition
import com.nowlsantos.big2ai.util.CARD_HEIGHT
import com.nowlsantos.big2ai.util.CARD_WIDTH
import com.nowlsantos.big2ai.util.HORIZONTAL_OVERLAP_FRACTION

@Composable
fun HorizontalPlayerHand(
    player: PlayerUiState,
    isHuman: Boolean,
    selectedCards: List<PlayingCard> = emptyList(),
    phase: GamePhase,
    onCardClick: ((PlayingCard) -> Unit)? = null
) {
    val overlapOffset = CARD_WIDTH * (1f - HORIZONTAL_OVERLAP_FRACTION)
    val totalCards = player.numberOfCards
    val totalVisualWidth = if (totalCards > 0) {
        CARD_WIDTH + (overlapOffset * (totalCards - 1).toFloat())
    } else {
        0.dp
    }

    var newIndex = if (player.position == PlayerPosition.NORTH) -1 else 1

    Box(modifier = Modifier.size(totalVisualWidth, CARD_HEIGHT)) {
        player.hand.forEachIndexed { index, card ->
            if (isHuman) {
                val cardDisplayModifier = Modifier
                    .offset(x = overlapOffset * index)
                    .zIndex(index.toFloat())

                val isSelected = selectedCards.contains(card)
                CardView(
                    card = card,
                    modifier = cardDisplayModifier,
                    isSelected = isSelected,
                    faceUp = true,
                    onClick = onCardClick
                )
            } else {
                if (phase == GamePhase.ROUND_END || phase == GamePhase.GAME_OVER) {
                    newIndex = 1
                }
                val cardDisplayModifier = Modifier
                    .offset(x = overlapOffset * index)
                    .zIndex(index.toFloat() * newIndex)

                if (phase == GamePhase.ROUND_END || phase == GamePhase.GAME_OVER) {
                    CardView(
                        card = card,
                        modifier = cardDisplayModifier,
                        faceUp = true,
                    )
                } else {
                    BackFaceView(modifier = cardDisplayModifier,)
                }
            }
        }
    }
}
