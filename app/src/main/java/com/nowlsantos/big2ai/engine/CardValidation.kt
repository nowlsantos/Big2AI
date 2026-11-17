package com.nowlsantos.big2ai.engine

import com.nowlsantos.big2ai.model.states.GameState
import com.nowlsantos.big2ai.model.data.Player
import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.model.enums.Rank
import com.nowlsantos.big2ai.model.enums.Suit

/**
 * Provides static utility functions for validating card plays in the Big Two game.
 */
object CardValidation {

    private val threeDiamonds = PlayingCard(Rank.THREE, Suit.DIAMONDS)

    /**
     * Validates a human player's attempted card play against the current game state rules.
     *
     * @param gameState The current state of the game.
     * @param player The player attempting the move.
     * @param cards The list of cards the player is attempting to play.
     * @param handEvaluator An instance of BigTwoHandEvaluator for combination evaluation.
     * @return An error message String if the move is invalid, or `null` if the move is valid.
     */
    fun validatePlayMove(
        gameState: GameState,
        player: Player,
        cards: List<PlayingCard>,
        handEvaluator: BigTwoHandEvaluator
    ): String? {

        if (cards.isEmpty()) {
            return "Select card(s) to play."
        }

        val currentTrickComboOnTable = gameState.lastPlayedHand.takeIf { it.isNotEmpty() }?.let { handEvaluator.evaluateHand(it) }
        val attemptedPlayCombo = handEvaluator.evaluateHand(cards)

        // Rule 1: Selected cards must form a valid Big Two combination
        if (attemptedPlayCombo == null) {
            return "Selected cards do not form a valid Big Two combination (e.g., single, pair, triple, 5-card combo)."
        }

        // Rule 2: Player must possess all selected cards
        if (!player.hand.containsAll(cards)) {
            return "You don't have all selected cards in your hand."
        }

        // Rule 3: First move of the game must contain 3 of Diamonds
        if (gameState.isFirstMoveOfGame) {
            if (!cards.contains(threeDiamonds)) {
                return "First move must contain 3 of Diamonds!"
            }
        } else if (currentTrickComboOnTable != null) { // Rule 4 & 5: If there's a trick on the table
            val isCurrentFiveCard = currentTrickComboOnTable.type.isHighValueCombo()
            val isAttemptedFiveCard = attemptedPlayCombo.type.isHighValueCombo()
            val isAttemptedBomb = attemptedPlayCombo.type.isBomb()

            if (isCurrentFiveCard) {
                // If current trick is a 5-card combo, attempted MUST also be a 5-card combo
                if (!isAttemptedFiveCard) {
                    return "You must play a 5-card combination to beat the current 5-card trick."
                }
            } else {
                // If current trick is Single, Pair, or Triple, attempted MUST be same type (unless it's a bomb)
                if (attemptedPlayCombo.type != currentTrickComboOnTable.type && !isAttemptedBomb) {
                    return "You must play a combination of the same type (e.g., single, pair, triple) to beat the trick, unless it's a bomb."
                }
            }

            // Rule 5: The attempted combination must be strong enough to beat the current trick
            // This check applies after type compatibility (or bomb exemption) is established.
            if (!handEvaluator.beats(attemptedPlayCombo, currentTrickComboOnTable)) {
                return "Your combination is not strong enough to beat the current trick."
            }
        }
        // If currentTrickComboOnTable is null and not first move, any valid combo is allowed (leading a new trick)

        return null // Move is valid
    }
}
