package com.nowlsantos.big2ai.presentation.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nowlsantos.big2ai.model.states.GameUiState
import com.nowlsantos.big2ai.model.enums.PlayerPosition
import com.nowlsantos.big2ai.util.ACTION_DELAY_MS

@Composable
fun PlayerNorth(
    uiState: GameUiState,
    playerAction: String,
    playerPassed: String,
) {
    Box(
        modifier = Modifier,
        contentAlignment = Alignment.Center
    ) {
        val northPlayer = uiState.players.find { it.position == PlayerPosition.NORTH }
        northPlayer?.let {
            HorizontalPlayerHand(
                player = it,
                isHuman = false,
                phase = uiState.currentPhase
            )

            if (uiState.currentTurnPlayerName == it.name) {
                if (playerAction.isNotEmpty()) {
                    DelayedContentDisplay(delayMillis = ACTION_DELAY_MS) {
                        PlayerAction(playerAction, PlayerPosition.NORTH)
                    }
                }
                if (playerPassed.isNotEmpty()) {
                    PlayerPassedTurn(playerPassed, PlayerPosition.NORTH)
                }
            }
        }
    }
}