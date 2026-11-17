package com.nowlsantos.big2ai.util

import com.nowlsantos.big2ai.model.states.GameState
import com.nowlsantos.big2ai.model.enums.Rank
import com.nowlsantos.big2ai.engine.BigTwoHandEvaluator // Import the hand evaluator
import com.nowlsantos.big2ai.model.enums.ComboType
import kotlin.math.pow

/**
 * Utility object for calculating scores at the end of a Big Two round,
 * following international standard scoring rules.
 */
object ScoreCalculator {

    private const val tag = "ScoreCalculator"

    /**
     * Calculates the penalty points for each player at the end of a round.
     * The winner receives 0 penalty points and has their `roundsWon` count incremented.
     * Losers receive points based on cards remaining and any special cards/bombs held.
     *
     * @param initialGameState The GameState at the moment the round ends,
     * which MUST have the `roundWinnerId` set.
     * @return A new GameState object with updated player scores and round win counts.
     */
    fun calculateRoundScores(initialGameState: GameState): GameState {
        AppLogger.i(tag, "Starting round score calculation for round ${initialGameState.roundNumber}.")

        val roundWinnerId = initialGameState.roundWinnerId
        if (roundWinnerId == null) {
            AppLogger.e(tag, "calculateRoundScores: roundWinnerId is null. Cannot calculate scores. Returning original state.")
            return initialGameState
        }

        val updatedPlayers = initialGameState.players.toMutableList()
        var totalLoserPointsDeducted = 0.0 // Use Double for calculation accuracy

        // Find the winner and increment their roundsWon
        val winnerPlayer = updatedPlayers.find { it.id == roundWinnerId }
        val winnerIndex = updatedPlayers.indexOf(winnerPlayer)
        if (winnerPlayer != null) {
            updatedPlayers[winnerIndex] = winnerPlayer.copy(roundsWon = winnerPlayer.roundsWon + 1)
            AppLogger.d(tag, "Round winner: ${winnerPlayer.name}. Rounds won: ${winnerPlayer.roundsWon + 1}.")
        } else {
            AppLogger.e(tag, "calculateRoundScores: Winner player not found. Returning original state.")
            return initialGameState
        }

        // Determine the cards played by the winner in their final move (the winning hand)
        val winningMoveCards = initialGameState.lastPlayedHand

        // --- Determine Winner's '2' Play Multiplier (for loser penalties) ---
        var winnerPlayedTwoMultiplierForLosers = 1.0
        if (winningMoveCards.any { it.rank == Rank.TWO }) {
            winnerPlayedTwoMultiplierForLosers = 2.0
            AppLogger.d(tag, "Winner played a '2' in their winning hand. Applying doubling effect to losers' base penalty.")
        }

        // Iterate through players to calculate scores
        for (i in updatedPlayers.indices) {
            var player = updatedPlayers[i]

            // Only calculate penalty for losers
            if (player.id != roundWinnerId) {
                val remainingCards = player.hand.size
                var penaltyPoints = 0.0 // Use Double for calculation accuracy

                when (remainingCards) {
                    in 1..9 -> penaltyPoints = remainingCards.toDouble()
                    in 10..12 -> penaltyPoints = remainingCards.toDouble() * 2
                    13 -> penaltyPoints = remainingCards.toDouble() * 3
                    else -> AppLogger.w(tag, "Unexpected remaining card count: $remainingCards for player ${player.name}. Setting penalty to 0.")
                }

                // Apply doubling if winner played a '2'
                penaltyPoints *= winnerPlayedTwoMultiplierForLosers

                // Check for '2's, 'Four of a Kind', and 'Straight Flush' in the loser's hand
                var bombsHeldCount = 0
                val handEvaluator = BigTwoHandEvaluator() // Create an instance

                // Count 2s
                val twosInHand = player.hand.count { it.rank == Rank.TWO }
                if (twosInHand > 0) {
                    // This is an additional flat penalty AND doubles (if that's the rule)
                    // Based on previous conversations, it seems this is an additional multiplier.
                    // The example "2s... can double the penalty points" implies a multiplier,
                    // but the previous code added a flat value (twosInHand * 2.0).
                    // Let's stick to the multiplier for consistency with "bombMultiplier".
                    // If '2's add a fixed penalty, use +=. If they double, use *=.
                    // Given the request, "unused 2s... can double the penalty points", a multiplier seems intended.
                    // Let's adjust this to multiply the current penalty.
                    penaltyPoints *= 2.0 // Double if a '2' is held
                    AppLogger.d(tag, "${player.name} held $twosInHand '2'(s). Penalty doubled due to unused 2s.")
                }


                // Check for Four of a Kind
                val fourOfAKindCombinations = handEvaluator.findCombinations(player.hand).filter { // FIXED: Changed findAllCombinations to findCombinations
                    it.type == ComboType.FOUR_OF_A_KIND
                }
                if (fourOfAKindCombinations.isNotEmpty()) {
                    bombsHeldCount += fourOfAKindCombinations.size // Each FoK is a bomb
                    AppLogger.d(tag, "${player.name} held ${fourOfAKindCombinations.size} Four of a Kind bomb(s).")
                }

                // Check for Straight Flush
                val straightFlushCombinations = handEvaluator.findCombinations(player.hand).filter { // FIXED: Changed findAllCombinations to findCombinations
                    it.type == ComboType.STRAIGHT_FLUSH
                }
                if (straightFlushCombinations.isNotEmpty()) {
                    bombsHeldCount += straightFlushCombinations.size // Each SF is a bomb
                    AppLogger.d(tag, "${player.name} held ${straightFlushCombinations.size} Straight Flush bomb(s).")
                }

                // Apply bomb multipliers
                if (bombsHeldCount > 0) {
                    val bombMultiplier = 2.0.pow(bombsHeldCount.toDouble()) // 1 bomb = 2x, 2 bombs = 4x
                    penaltyPoints *= bombMultiplier
                    AppLogger.d(tag, "${player.name} held $bombsHeldCount bomb type(s). " +
                            "Base penalty ${penaltyPoints / bombMultiplier} multiplied by ${bombMultiplier}.")
                }

                // Convert to Int for final score and update player's cumulative score
                val finalRoundPenalty = penaltyPoints.toInt() // Cast to Int for final score
                val newTotalScore = player.score - finalRoundPenalty

                player = player.copy(score = newTotalScore) // Update player's score
                updatedPlayers[i] = player // Update the player in the list

                totalLoserPointsDeducted += finalRoundPenalty // Add to sum for winner's score

                AppLogger.d(tag, "${player.name} (${remainingCards} cards left) incurred -$finalRoundPenalty points this round. " +
                        "Total score: ${player.score}. Total rounds won: ${player.roundsWon}.")
            }
        }

        // --- Calculate Winner's Score ---
        // The winner receives points equal to the sum of the penalty points DEDUCTED from the other players.
        // The previous 'totalRoundBonusMultiplier' based on winning combo (excluding '2') is preserved.
        var winnerFinalPlayBonusMultiplier = 1.0 // Initialize
        val winningComboEval = BigTwoHandEvaluator().evaluateHand(winningMoveCards) // Assumed to be non-null if winning
        if (winningComboEval != null && winnerPlayer.hand.isEmpty()) { // Ensure winner has no cards left
            // ONLY double if it's a Four-of-a-Kind OR Straight Flush (not a '2' here as per your latest request)
            if (winningComboEval.type == ComboType.FOUR_OF_A_KIND ||
                winningComboEval.type == ComboType.STRAIGHT_FLUSH) {
                winnerFinalPlayBonusMultiplier = 2.0
                AppLogger.d(tag, "Doubling triggered by winner's final play: ${winningComboEval.type}. Winner's total gain multiplier is now $winnerFinalPlayBonusMultiplier.")
            }
        }
        val finalWinnerScoreChange = (totalLoserPointsDeducted * winnerFinalPlayBonusMultiplier).toInt()

        // Apply winner's score (add to their score)
        val currentWinner = updatedPlayers.find { it.id == roundWinnerId }
        val updatedWinner = currentWinner?.copy(score = currentWinner.score + finalWinnerScoreChange)
        if (updatedWinner != null) {
            updatedPlayers[updatedPlayers.indexOf(currentWinner)] = updatedWinner
            AppLogger.d(tag, "${updatedWinner.name} won the round and gained +$finalWinnerScoreChange points. Total score: ${updatedWinner.score}.")
        } else {
            AppLogger.e(tag, "calculateRoundScores: Winner player not found after score calculation (final update). This should not happen.")
        }


        // Determine if game is over (This logic should be outside score calculation or handled by GameStateMachine)
        // I'm retaining the original structure for returning updatedPlayers and roundNumber
        // The MAX_GAME_SCORE_THRESHOLD is not defined in this snippet, assuming it's elsewhere.

        AppLogger.i(tag, "Round scores calculated. Final scores: " +
                "${updatedPlayers.associate { it.name to it.score }}. Rounds won: " +
                "${updatedPlayers.associate { it.name to it.roundsWon }}.")

        // Return a new GameState with the updated player list and advanced round number
        return initialGameState.copy(
            players = updatedPlayers,
        )
    }
}
