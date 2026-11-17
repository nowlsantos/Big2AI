package com.nowlsantos.big2ai.engine

import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.model.enums.ComboType
import com.nowlsantos.big2ai.model.data.Player
import com.nowlsantos.big2ai.model.data.Move
import com.nowlsantos.big2ai.model.data.PlayMove
import com.nowlsantos.big2ai.model.data.PassMove
import com.nowlsantos.big2ai.model.data.PlayerHandCombination
import com.nowlsantos.big2ai.util.AppLogger

/**
 * PlayerProfile tracks and infers information about a single opponent.
 */
class PlayerProfile(
    private val playerId: String, // Added playerId for better logging/context
    private val handEvaluator: BigTwoHandEvaluator // Injected HandEvaluator dependency
) {
    private val tag = "PlayerProfile" // Define tag for logging within class methods

    // Tracks the cards this opponent has played
    private val playedCards = mutableSetOf<PlayingCard>()
    // A history of moves made by this opponent (useful for pattern recognition)
    val moveHistory = mutableListOf<Move>()
    // Estimated remaining cards in the opponent's hand. Managed externally by OpponentModel.
    private var estimatedHand = mutableSetOf<PlayingCard>()
    // Basic behavioral indicators
    private var isAggressive: Boolean = false
    // Tracks if this specific player passed last turn (for pass inference)
    private var passedLastTurn: Boolean = false
    // Stores the combo they passed on (for inference: if they passed on X, they likely don't have better than X)
    private var passedOnCombo: PlayerHandCombination? = null

    /**
     * Updates the player's profile based on game state observations for their turn.
     * @param player The specific Player object for this profile.
     * @param currentTrick The cards currently on the table (last played combo).
     * @param lastPlayerIdWhoPlayed The ID of the player who last played cards.
     */
    fun update(player: Player, currentTrick: List<PlayingCard>, lastPlayerIdWhoPlayed: String?) {
        AppLogger.d(tag, "PlayerProfile:: Updating profile for player ${player.name} (${player.id}).")

        // Update played cards from the current trick if this player was the last to play
        if (lastPlayerIdWhoPlayed == playerId && currentTrick.isNotEmpty()) {
            playedCards.addAll(currentTrick)
            val evaluatedCombo = handEvaluator.evaluateHand(currentTrick)
            if (evaluatedCombo != null && evaluatedCombo.type != ComboType.NONE) {
                moveHistory.add(PlayMove(currentTrick, evaluatedCombo.type, playerId))
            }
            passedLastTurn = false // Player played, so they didn't pass this turn
            passedOnCombo = null // Clear passed-on combo if they played
        }
        // If they passed this turn, record it and the combo they passed on
        else if (player.hasPassed && !passedLastTurn) { // Only log if they passed *this* turn and previously didn't
            AppLogger.d(tag, "PlayerProfile:: Player ${player.name} passed. Recording pass on combo: ${currentTrick.joinToString { it.toString() }}.")
            moveHistory.add(PassMove(playerId))
            passedOnCombo = handEvaluator.evaluateHand(currentTrick) // Store the combo they passed on
            passedLastTurn = true
        } else if (!player.hasPassed) {
            // If they didn't pass this turn, and weren't the last to play, reset pass status.
            passedLastTurn = false
            passedOnCombo = null
        }

        // Update behavioral traits (very simplistic for now)
        isAggressive = moveHistory.count { it is PlayMove && it.type.isHighValueCombo() } > moveHistory.size / 2
        AppLogger.d(tag, "PlayerProfile:: Profile for ${player.name} updated. Aggressive: $isAggressive, Passed Last Turn: $passedLastTurn.")
    }

    /**
     * Predicts the likelihood of this opponent countering a given move.
     * This now uses the estimatedHand and recorded passedOnCombo for more informed risk.
     * @param move The move against which to predict a counter.
     * @return A probability between 0.0 and 1.0.
     */
    fun predictCounterRisk(move: Move): Double {
        if (move is PassMove) return 0.0

        val potentialCounterMoves = handEvaluator.findCombinations(estimatedHand.toList())
        val playedCombo = handEvaluator.evaluateHand(move.cards) ?: return 0.0 // Ensure played combo is valid

        val canBeat = potentialCounterMoves.any { counterCombo ->
            handEvaluator.beats(counterCombo, playedCombo)
        }
        //AppLogger.d(tag, "PlayerProfile:: Predict counter risk for move: ${move.cards.joinToString { it.toShortString() }}. Can beat: $canBeat (based on estimated hand)")

        var risk = if (canBeat) 0.6 else 0.1 // Base risk if they can beat

        // Adjust based on estimated hand size
        risk += when (estimatedHand.size) {
            in 0..2 -> 0.3 // Very few cards, more likely to play if they can to win
            in 3..5 -> 0.1 // Mid-range
            else -> 0.0 // Many cards, might save
        }

        // Adjust based on if they recently passed on a similar/weaker combo
        if (passedLastTurn && passedOnCombo != null) {
            if (handEvaluator.beats(passedOnCombo!!, playedCombo) && !handEvaluator.beats(playedCombo, passedOnCombo!!)) {
                // If they passed on a combo that could beat the current move, but the current move is stronger.
                // This implies they didn't have something better than their passed combo, so their risk of beating
                // the *current* (stronger) move might be lower.
                risk -= 0.2 // Reduce risk if they passed on a weaker or comparable combo recently
                AppLogger.d(tag, "PlayerProfile:: Reduced risk due to recent pass on comparable/weaker combo.")
            }
        }

        // Factor in behavioral traits (if they are aggressive, higher risk)
        if (isAggressive) risk += 0.1

        val finalRisk = risk.coerceIn(0.0, 1.0)
        AppLogger.d(tag, "PlayerProfile:: Final counter risk: $finalRisk")
        return finalRisk
    }

    /**
     * Checks if this player is estimated to be close to winning.
     * @return True if the player is estimated to have 2 or fewer cards left.
     */
    fun isCloseToWinning(): Boolean {
        val closeToWin = estimatedHand.size <= 2
        AppLogger.d(tag, "PlayerProfile:: Player $playerId is close to winning: $closeToWin (Estimated hand size: ${estimatedHand.size})")
        return closeToWin
    }

    /**
     * Calculates the threat level posed by this opponent.
     * @return A Double representing the threat level (e.0 to 1.0).
     */
    fun threatLevel(): Double {
        val threat = when {
            estimatedHand.isEmpty() -> 0.0 // Already out
            estimatedHand.size <= 1 -> 1.0 // Very high threat
            estimatedHand.size <= 2 -> 0.8 // High threat
            estimatedHand.size <= 3 -> 0.5 // Medium threat
            else -> 0.1 // Low threat
        }
        AppLogger.d(tag, "PlayerProfile:: Player ${playerId} threat level: $threat")
        return threat
    }

    /**
     * Provides the current set of estimated cards for this opponent.
     * @return An immutable Set of PlayingCard objects.
     */
    fun getEstimatedHand(): Set<PlayingCard> = estimatedHand.toSet()

    /**
     * Sets the initial estimated hand for this opponent.
     * Used by OpponentModel during card distribution.
     * @param cards The set of cards to set as the estimated hand.
     */
    fun setEstimatedHand(cards: Set<PlayingCard>) {
        estimatedHand.clear()
        estimatedHand.addAll(cards)
        AppLogger.d(tag, "PlayerProfile:: Player ${playerId}: Estimated hand set. Size: ${estimatedHand.size}")
    }

    /**
     * Clears the estimated hand for this opponent.
     */
    fun clearEstimatedHand() {
        estimatedHand.clear()
        AppLogger.d(tag, "PlayerProfile:: Player ${playerId}: Estimated hand cleared.")
    }

    /**
     * Returns true if this player passed their last turn.
     */
    fun hasPassedLastTurn(): Boolean = passedLastTurn

    /**
     * Returns the combo this player passed on during their last turn.
     */
    fun getPassedOnCombo(): PlayerHandCombination? = passedOnCombo
}
