package com.nowlsantos.big2ai.presentation.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nowlsantos.big2ai.model.states.GameUiState
import com.nowlsantos.big2ai.model.enums.PlayerPosition
import com.nowlsantos.big2ai.model.data.PlayerUiState
import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.util.ACTION_DELAY_MS

@Composable
fun PlayerSouth(
    uiState: GameUiState,
    player: PlayerUiState,
    selectedCards: List<PlayingCard>?,
    playerAction: String,
    playerPassed: String,
    onCardClick: (PlayingCard) -> Unit
) {
    Box(
        modifier = Modifier,
        contentAlignment = Alignment.Center
    ) {
        selectedCards?.let {
            HorizontalPlayerHand(
                player = player,
                isHuman = true,
                selectedCards = it,
                phase = uiState.currentPhase,
                onCardClick = onCardClick
            )
        }

        if (uiState.currentTurnPlayerName == player.name) {
            if (playerAction.isNotEmpty()) {
                DelayedContentDisplay(delayMillis = ACTION_DELAY_MS) {
                    PlayerAction(playerAction, PlayerPosition.SOUTH)
                }
            }
            if (playerPassed.isNotEmpty()) {
                PlayerPassedTurn(playerPassed, PlayerPosition.SOUTH)
            }
        }
    }
}