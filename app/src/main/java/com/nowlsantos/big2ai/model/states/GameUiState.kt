package com.nowlsantos.big2ai.model.states

import com.nowlsantos.big2ai.model.data.LastPlayedHandState
import com.nowlsantos.big2ai.model.enums.PlayerPosition
import com.nowlsantos.big2ai.model.data.PlayerUiState
import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.model.enums.GamePhase

/**
 * Represents the overall UI state of the game, designed specifically for Compose rendering.
 * It's a transformation of the core GameState, containing only relevant UI data.
 */
data class GameUiState(
    val currentPhase: GamePhase,
    val players: List<PlayerUiState>, // All players, including simplified info for opponents
    val currentTurnPlayerName: String?,
    val canPlay: Boolean,      // Control play button visibility/interactivity in PLAYING phase
    val canPass: Boolean,      // Control pass button visibility/interactivity in PLAYING phase
    val canResetGame: Boolean,  // Control reset button visibility
    val canStartGame: Boolean, // Control button visibility in SETUP phase
    val roundWinnerName: String?,
    val gameWinnerName: String?,
    val lastPlayedHand: LastPlayedHandState?, // Nullable if no hand has been played yet
    val roundNumber: Int,
) {
    // This is useful for previews or initial ViewModel states.
    companion object {
        fun initial(): GameUiState = GameUiState(
            currentPhase = GamePhase.SETUP,
            players = emptyList(),
            canStartGame = false,
            canPlay = false,
            canPass = false,
            canResetGame = false,
            currentTurnPlayerName = null,
            lastPlayedHand = null,
            roundWinnerName = null,
            gameWinnerName = null,
            roundNumber = 0,
        )
    }
}