package com.nowlsantos.big2ai.engine

import com.nowlsantos.big2ai.model.enums.ComboType
import com.nowlsantos.big2ai.model.states.GameState
import com.nowlsantos.big2ai.model.data.Move
import com.nowlsantos.big2ai.model.data.PassMove
import com.nowlsantos.big2ai.model.data.PlayerHandCombination
import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.model.enums.Rank
import kotlin.math.min
import kotlin.random.Random
import com.nowlsantos.big2ai.util.AppLogger

class OpponentModel(
    private val cardTracker: CardTracker,
    private val handEvaluator: BigTwoHandEvaluator
) {
    private val tag = "OpponentModel"
    private val playerProfiles = mutableMapOf<String, PlayerProfile>()

    fun reset() {
        playerProfiles.clear()
        AppLogger.d(tag, "Reset. All player profiles cleared.")
    }

    fun update(gameState: GameState, aiPlayerId: String) {
        AppLogger.d(tag, "Updating all player profiles with AI $aiPlayerId.")

        gameState.players.forEach { player ->
            if (!playerProfiles.containsKey(player.id)) {
                playerProfiles[player.id] = PlayerProfile(player.id, handEvaluator)
            }
            playerProfiles[player.id]?.update(player, gameState.currentTrick, gameState.lastPlayerIdWhoPlayed)
        }

        distributeEstimatedCards(gameState, aiPlayerId)
    }

    private fun distributeEstimatedCards(gameState: GameState, aiPlayerId: String) {
        val unassignedCardsPool = cardTracker.getUnassignedCardsPool().toMutableList()
        AppLogger.d(tag, "Distributing estimated cards. Unassigned pool size: ${unassignedCardsPool.size}")

        val opponentsForEstimation = gameState.players.filter { it.id != aiPlayerId && !it.isHuman }
        playerProfiles.values.forEach { it.clearEstimatedHand() }

        unassignedCardsPool.shuffle(Random)
        var currentCardIndex = 0
        opponentsForEstimation.forEach { opponent ->
            val profile = playerProfiles[opponent.id]
            val cardsToDeal = min(cardTracker.getPlayerCardCount(opponent.id), unassignedCardsPool.size - currentCardIndex)
            val thisOpponentEstimatedHand = mutableSetOf<PlayingCard>()

            repeat(cardsToDeal) {
                if (currentCardIndex < unassignedCardsPool.size) {
                    thisOpponentEstimatedHand.add(unassignedCardsPool[currentCardIndex])
                    currentCardIndex++
                }
            }
            profile?.setEstimatedHand(thisOpponentEstimatedHand)
        }
    }

    // ADDED MISSING METHOD
    fun estimateHandStrength(playerId: String): Double {
        val profile = playerProfiles[playerId]
        if (profile == null) {
            AppLogger.d(tag, "estimateHandStrength: Profile not found for $playerId. Returning 0.0.")
            return 0.0
        }

        val estimatedHand = profile.getEstimatedHand().toList()
        if (estimatedHand.isEmpty()) return 0.0

        // Calculate hand strength based on card values
        return estimatedHand.sumOf { card ->
            when (card.rank) {
                Rank.TWO -> 25.0
                Rank.ACE -> 20.0
                Rank.KING -> 18.0
                Rank.QUEEN -> 16.0
                Rank.JACK -> 14.0
                else -> card.rank.value.toDouble()
            }
        }
    }

    fun predictCounterMoveRisk(move: Move, gameState: GameState): Double {
        if (move is PassMove) return 0.0

        val opponentsToConsider = playerProfiles.filter { (id, _) ->
            val playerInGameState = gameState.players.find { p -> p.id == id }
            id != move.ownerId && playerInGameState?.isHuman == false
        }.values

        if (opponentsToConsider.isEmpty()) return 0.0

        val totalRisk = opponentsToConsider.sumOf { profile ->
            profile.predictCounterRisk(move)
        }
        return totalRisk / opponentsToConsider.size
    }

    fun bombProbability(opponentId: String): Double {
        val profile = playerProfiles[opponentId] ?: return 0.0
        val estimatedHand = profile.getEstimatedHand().toList()
        if (estimatedHand.size < 4) return 0.0

        val possibleCombos = handEvaluator.findCombinations(estimatedHand)
        return if (possibleCombos.any { it.type.isBomb() }) 1.0 else 0.0
    }

    fun isPlayerCloseToWinning(playerId: String): Boolean {
        return playerProfiles[playerId]?.isCloseToWinning() == true
    }

    fun threatLevel(playerId: String): Double {
        return playerProfiles[playerId]?.threatLevel() ?: 0.0
    }

    fun probabilityOfHigherCombo(opponentId: String, comboType: ComboType, leadCard: PlayingCard?): Double {
        if (leadCard == null || comboType == ComboType.NONE) return 0.0

        val profile = playerProfiles[opponentId] ?: return 0.0
        val estimatedHand = profile.getEstimatedHand().toList()
        if (estimatedHand.isEmpty()) return 0.0

        val possibleCombosInOpponentHand = handEvaluator.findCombinations(estimatedHand)
        /* Comparison logic would go here */
        return 0.0 // Placeholder
    }

    fun estimatePassProbability(gameState: GameState): Double {
        val opponents = gameState.players.filter {
            it.id != gameState.currentPlayerId && !it.isHuman
        }
        if (opponents.isEmpty()) return 0.0

        var totalPassProbability = 0.0
        opponents.forEach { opponent ->
            val profile = playerProfiles[opponent.id]
            var opponentPassProb = 0.0

            if (profile != null && profile.hasPassedLastTurn()) {
                opponentPassProb += 0.3
            }

            val estimatedHand = profile?.getEstimatedHand() ?: emptySet()
            if (estimatedHand.isNotEmpty()) {
                val maxCardStrength = 153.0
                val handStrength = estimatedHand.sumOf { it.strength }
                val normalizedStrength = handStrength / (estimatedHand.size * maxCardStrength)
                opponentPassProb += (1 - normalizedStrength) * 0.5
            } else {
                opponentPassProb += 0.5
            }

            totalPassProbability += opponentPassProb.coerceIn(0.0, 1.0)
        }

        return (totalPassProbability / opponents.size).coerceIn(0.0, 1.0)
    }

    // ADDED HELPER FOR CARD VALUE
    private fun cardValue(card: PlayingCard): Double {
        return when (card.rank) {
            Rank.TWO -> 25.0
            Rank.ACE -> 20.0
            Rank.KING -> 18.0
            Rank.QUEEN -> 16.0
            Rank.JACK -> 14.0
            else -> card.rank.value.toDouble()
        }
    }
}



//package com.nowlsantos.big2ai.engine
//
////import android.util.Log
//import com.nowlsantos.big2ai.model.enums.ComboType
//import com.nowlsantos.big2ai.model.states.GameState
//import com.nowlsantos.big2ai.model.data.Move
//import com.nowlsantos.big2ai.model.data.PassMove
//import com.nowlsantos.big2ai.model.data.PlayerHandCombination
//import com.nowlsantos.big2ai.model.data.PlayingCard
//import kotlin.math.min
//import kotlin.random.Random
//import com.nowlsantos.big2ai.util.AppLogger
//
///**
// * OpponentModel tracks and infers information about all opponents in the game.
// * It manages individual PlayerProfile instances and uses CardTracker's data
// * to probabilistically estimate opponent hands and predict behavior.
// */
//class OpponentModel(
//    private val cardTracker: CardTracker, // Injected CardTracker dependency
//    private val handEvaluator: BigTwoHandEvaluator // Injected HandEvaluator dependency
//) {
//    private val tag = "OpponentModel" // Define tag for logging within class methods
//
//    // Stores PlayerProfile instances, keyed by player ID.
//    private val playerProfiles = mutableMapOf<String, PlayerProfile>()
//
//    /**
//     * Resets the opponent model and all player profiles.
//     */
//    fun reset() {
//        playerProfiles.clear()
//        AppLogger.d(tag, "OpponentModel:: Reset. All player profiles cleared.")
//    }
//
//    /**
//     * Updates all opponent profiles based on the latest game state.
//     * This method should be called at the beginning of each AI's turn.
//     * @param gameState The current state of the game.
//     * @param aiPlayerId The ID of the AI player.
//     */
//    fun update(gameState: GameState, aiPlayerId: String) {
//        AppLogger.d(tag, "OpponentModel:: Updating all player profiles with AI $aiPlayerId.")
//
//        // Ensure profiles exist for all players and update them
//        gameState.players.forEach { player ->
//            if (!playerProfiles.containsKey(player.id)) {
//                playerProfiles[player.id] = PlayerProfile(player.id, handEvaluator) // Pass player ID and handEvaluator to profile
//                AppLogger.d(tag, "OpponentModel:: Created new PlayerProfile for player: ${player.name} (${player.id})")
//            }
//            // Update each player's profile with their state and the context of the last trick
//            playerProfiles[player.id]?.update(player, gameState.currentTrick, gameState.lastPlayerIdWhoPlayed)
//        }
//
//        // Distribute unassigned cards among opponent estimated hands
//        distributeEstimatedCards(gameState, aiPlayerId)
//        AppLogger.d(tag, "OpponentModel:: All player profiles updated and cards distributed.")
//    }
//
//    /**
//     * Distributes the currently unassigned cards (from CardTracker)
//     * probabilistically among opponents' estimated hands.
//     *
//     * This improved distribution considers:
//     * 1. The known number of cards each opponent has.
//     * 2. Simple inference from recent passes.
//     *
//     * @param gameState The current game state.
//     * @param aiPlayerId The ID of the AI player (whose hand is known).
//     */
//    private fun distributeEstimatedCards(gameState: GameState, aiPlayerId: String) {
//        // Get the pool of cards not in AI's hand and not in played tricks.
//        val unassignedCardsPool = cardTracker.getUnassignedCardsPool().toMutableList()
//        AppLogger.d(tag, "OpponentModel:: Distributing estimated cards. Unassigned pool size: ${unassignedCardsPool.size}")
//
//        // Filter out the AI player itself and human players from opponents for estimation.
//        val opponentsForEstimation = gameState.players.filter { it.id != aiPlayerId && !it.isHuman }
//
//        // Clear previous estimations before redistributing
//        playerProfiles.values.forEach { it.clearEstimatedHand() }
//
//        // Perform some basic inference based on passes for all opponents in this round
//        opponentsForEstimation.forEach { opponent ->
//            val profile = playerProfiles[opponent.id]
//            if (profile != null && profile.hasPassedLastTurn()) {
//                val passedOnCombo = profile.getPassedOnCombo()
//                if (passedOnCombo != null) {
//                    // Simple inference: If they passed on a strong combo (e.g., A or 2 lead),
//                    // it's unlikely they had an even stronger card of that type.
//                    // This is still very basic and doesn't remove cards, but it helps inform.
//                    // For now, no cards are removed from the pool based on passing, but this could be a future enhancement.
//                }
//            }
//        }
//
//        // Randomly distribute the unassigned cards while trying to respect card counts.
//        // Shuffle the unassigned cards for randomness.
//        unassignedCardsPool.shuffle(Random)
//
//        // Calculate total cards to distribute among opponents
//        val totalOpponentCardsExpected = opponentsForEstimation.sumOf { cardTracker.getPlayerCardCount(it.id) }
//        AppLogger.d(tag, "OpponentModel:: Total opponent cards expected: $totalOpponentCardsExpected. Unassigned pool size: ${unassignedCardsPool.size}")
//
//        var currentCardIndex = 0
//        opponentsForEstimation.forEach { opponent ->
//            val profile = playerProfiles[opponent.id]
//            // Get the actual number of cards this opponent is known to have (from CardTracker)
//            val cardsToDeal = min(cardTracker.getPlayerCardCount(opponent.id), unassignedCardsPool.size - currentCardIndex)
//            val thisOpponentEstimatedHand = mutableSetOf<PlayingCard>() // Initialize a new mutable set for this opponent
//
//            // Distribute cards to this opponent's estimated hand
//            repeat(cardsToDeal) {
//                if (currentCardIndex < unassignedCardsPool.size) {
//                    thisOpponentEstimatedHand.add(unassignedCardsPool[currentCardIndex])
//                    currentCardIndex++
//                } else {
//                    AppLogger.w(tag, "OpponentModel:: Ran out of unassigned cards while dealing to ${opponent.name}. Card count might be inaccurate.")
//                }
//            }
//            profile?.setEstimatedHand(thisOpponentEstimatedHand)
//            AppLogger.d(tag, "OpponentModel:: Estimated hand for ${opponent.name}: ${thisOpponentEstimatedHand.size} cards.")
//        }
//        AppLogger.d(tag, "OpponentModel:: Estimated cards distributed to opponents.")
//    }
//
//    /**
//     * Predicts the likelihood of an opponent countering a given move.
//     * This leverages the `PlayerProfile`'s estimated hand and behavioral traits.
//     * @param move The move to assess against.
//     * @return A weighted average of counter risks from all active opponents.
//     */
//    fun predictCounterMoveRisk(move: Move, gameState: GameState): Double {
//        if (move is PassMove) return 0.0
//
//        val opponentsToConsider = playerProfiles.filter { (id, _) ->
//            val playerInGameState = gameState.players.find { p -> p.id == id }
//            id != move.ownerId && playerInGameState?.isHuman == false // Filter out AI's own player and human players
//        }.values
//        AppLogger.d(tag, "OpponentModel:: Predicting counter move risk for move: ${move.javaClass.simpleName}. Opponents to consider: ${opponentsToConsider.size}")
//
//        if (opponentsToConsider.isEmpty()) return 0.0
//
//        val totalRisk = opponentsToConsider.sumOf { profile ->
//            profile.predictCounterRisk(move)
//        }
//        val averageRisk = totalRisk / opponentsToConsider.size
//        AppLogger.d(tag, "OpponentModel:: Average counter risk: $averageRisk")
//        return averageRisk
//    }
//
//    /**
//     * Estimates the probability of a specific opponent having a "bomb" combination
//     * within their estimated hand.
//     * @param opponentId The ID of the opponent to check.
//     * @return A probability between 0.0 and 1.0.
//     */
//    fun bombProbability(opponentId: String): Double {
//        val profile = playerProfiles[opponentId]
//        if (profile == null) {
//            AppLogger.d(tag, "OpponentModel:: bombProbability: Profile not found for $opponentId. Returning 0.0.")
//            return 0.0
//        }
//
//        val estimatedHand = profile.getEstimatedHand().toList()
//        if (estimatedHand.size < 4) { // Minimum cards for a 4-of-a-kind bomb
//            AppLogger.d(tag, "OpponentModel:: bombProbability: Opponent $opponentId hand too small (${estimatedHand.size}) for bomb. Returning 0.0.")
//            return 0.0
//        }
//
//        val possibleCombos = handEvaluator.findCombinations(estimatedHand)
//        val hasBomb = possibleCombos.any { it.type.isBomb() }
//
//        // This is still a simplified probability based on the *single* estimated hand.
//        // For a more advanced MCTS, this would be a distribution over possible hands.
//        return if (hasBomb) 1.0 else 0.0
//    }
//
//    /**
//     * Checks if a specific player is estimated to be close to winning (few cards left).
//     * @param playerId The ID of the player to check.
//     * @return True if the player is close to winning based on their profile estimation.
//     */
//    fun isPlayerCloseToWinning(playerId: String): Boolean {
//        val isClose = playerProfiles[playerId]?.isCloseToWinning() == true
//        AppLogger.d(tag, "OpponentModel:: Player $playerId close to winning: $isClose")
//        return isClose
//    }
//
//    /**
//     * Calculates the threat level posed by a specific player.
//     * @param playerId The ID of the player to assess.
//     * @return A numerical threat level.
//     */
//    fun threatLevel(playerId: String): Double {
//        val threat = playerProfiles[playerId]?.threatLevel() ?: 0.0
//        AppLogger.d(tag, "OpponentModel:: Player $playerId threat level: $threat")
//        return threat
//    }
//
//    /**
//     * Estimates the probability that an opponent has a higher combo of a specific type.
//     * This uses the opponent's estimated hand.
//     * @param opponentId The ID of the opponent to check.
//     * @param comboType The type of combination to check against.
//     * @param leadCard The leading card of the combo played (used for strength comparison).
//     * @return A probability between 0.0 and 1.0.
//     */
//    fun probabilityOfHigherCombo(opponentId: String, comboType: ComboType, leadCard: PlayingCard?): Double {
//        if (leadCard == null || comboType == ComboType.NONE) {
//            AppLogger.d(tag, "OpponentModel:: probabilityOfHigherCombo: Invalid input (leadCard null or comboType NONE). Returning 0.0.")
//            return 0.0
//        }
//
//        val profile = playerProfiles[opponentId]
//        if (profile == null) {
//            AppLogger.d(tag, "OpponentModel:: probabilityOfHigherCombo: Profile not found for $opponentId. Returning 0.0.")
//            return 0.0
//        }
//
//        val estimatedHand = profile.getEstimatedHand().toList()
//        if (estimatedHand.isEmpty()) {
//            AppLogger.d(tag, "OpponentModel:: probabilityOfHigherCombo: Opponent $opponentId has empty estimated hand. Returning 0.0.")
//            return 0.0
//        }
//
//        val possibleCombosInOpponentHand = handEvaluator.findCombinations(estimatedHand)
//        /*val hasHigherCombo = possibleCombosInOpponentHand.any { opponentCombo ->
//            // Ensure the comparison is against the correct combo type and lead card.
//            // The `beats` function handles bomb rules and same-type comparisons.
//            handEvaluator.beats(opponentCombo, PlayerHandCombination(listOf(leadCard), comboType, leadCard))
//        }*/
//        val hasHigherCombo = possibleCombosInOpponentHand.any { opponentCombo ->
//            val comparisonCombo = when (comboType) {
//                ComboType.SINGLE -> PlayerHandCombination.createSingle(leadCard)
//                ComboType.PAIR -> {
//                    // Create a proper pair combination for comparison
//                    // Note: In real usage, you should pass actual pair cards, not duplicates
//                    PlayerHandCombination(
//                        cards = listOf(leadCard, leadCard.copy()), // Two distinct cards
//                        type = ComboType.PAIR,
//                        leadCard = leadCard,
//                        strength = leadCard.strength
//                    )
//                }
//                ComboType.TRIPLE -> {
//                    // Create a proper triple combination for comparison
//                    PlayerHandCombination(
//                        cards = listOf(leadCard, leadCard.copy(), leadCard.copy()),
//                        type = ComboType.TRIPLE,
//                        leadCard = leadCard,
//                        strength = leadCard.strength
//                    )
//                }
//                else -> PlayerHandCombination(
//                    cards = listOf(leadCard),
//                    type = comboType,
//                    leadCard = leadCard,
//                    strength = leadCard.strength
//                )
//            }
//            handEvaluator.beats(opponentCombo, comparisonCombo)
//        }
//
//        // This is a binary probability based on the single estimated hand.
//        return if (hasHigherCombo) 1.0 else 0.0
//    }
//
//    fun estimatePassProbability(gameState: GameState): Double {
//        val opponents = gameState.players.filter {
//            it.id != gameState.currentPlayerId && !it.isHuman
//        }
//        if (opponents.isEmpty()) return 0.0
//
//        var totalPassProbability = 0.0
//        opponents.forEach { opponent ->
//            val profile = playerProfiles[opponent.id]
//            var opponentPassProb = 0.0
//
//            // 1. Recent pass behavior
//            if (profile != null && profile.hasPassedLastTurn()) {
//                opponentPassProb += 0.3
//            }
//
//            // 2. Hand strength heuristic
//            val estimatedHand = profile?.getEstimatedHand() ?: emptySet()
//            if (estimatedHand.isNotEmpty()) {
//                val maxCardStrength = 153.0 // (15*10 + 3) for TWO-SPADES
//                val handStrength = estimatedHand.sumOf { it.strength }
//                val normalizedStrength = handStrength / (estimatedHand.size * maxCardStrength)
//                opponentPassProb += (1 - normalizedStrength) * 0.5
//            } else {
//                opponentPassProb += 0.5 // Unknown hand, assume 50% pass chance
//            }
//
//            // Cap individual probability at 1.0
//            totalPassProbability += opponentPassProb.coerceIn(0.0, 1.0)
//        }
//
//        // Return average probability
//        return (totalPassProbability / opponents.size).coerceIn(0.0, 1.0)
//    }
//}
