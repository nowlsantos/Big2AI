package com.nowlsantos.big2ai.model.data

import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.model.enums.PlayerPosition

/**
 * Represents the simplified UI-specific state of a single player.
 * Contains only the data needed for rendering a player's status on the UI.
 */
data class PlayerUiState(
    val id: String,
    val name: String,
    val score: Int = 0,
    var selectedCards: Set<PlayingCard> = emptySet(),
    val numberOfCards: Int, // Number of cards in hand for all players (for opponents, hand will be empty)
    val hand: List<PlayingCard>, // Actual cards, only for the local player. Empty for others.
    val isCurrentPlayer: Boolean, // True if it's this player's turn
    val hasPassed: Boolean,       // True if this player has passed in the current trick
    val position: PlayerPosition, // Their position on the board (e.g., SOUTH, NORTH)
    val isHuman: Boolean,
    val roundsWon: Int = 0,
    val action: String? = null
) {
    companion object {
        fun initial(): PlayerUiState = PlayerUiState(
            id = "",
            name = "SouthPlayer",
            score = 0,
            selectedCards = emptySet(),
            numberOfCards = 0,
            hand = emptyList(),
            isCurrentPlayer = false,
            hasPassed = false,
            position = PlayerPosition.SOUTH,
            isHuman = true,
            roundsWon = 0,
            action = null
        )
    }
}