package com.nowlsantos.big2ai.model.states

import com.nowlsantos.big2ai.engine.BigTwoHandEvaluator
import com.nowlsantos.big2ai.model.data.Move
import com.nowlsantos.big2ai.model.data.PassMove
import com.nowlsantos.big2ai.model.data.PlayMove
import com.nowlsantos.big2ai.model.data.PlayerHandCombination
import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.model.enums.Rank
import com.nowlsantos.big2ai.model.enums.Suit

class GameStateTransitionProcessor(private val state: GameState) {
    fun apply(move: Move): GameState {
        return when (move) {
            is PlayMove -> applyPlayMove(move)
            is PassMove -> applyPassMove(move)
        }
    }

    private fun applyPlayMove(move: PlayMove): GameState {
        require(move.ownerId == state.currentPlayerId) {
            "Player ${move.ownerId} cannot play when it's ${state.currentPlayerId}'s turn"
        }

        // First move validation
        if (state.isFirstMoveOfGame) {
            val threeDiamonds = PlayingCard(Rank.THREE, Suit.DIAMONDS)
            if (!move.cards.contains(threeDiamonds)) {
                throw IllegalArgumentException("First move must include 3♦")
            }
        }

        val player = state.players.find { it.id == move.ownerId }
            ?: throw IllegalStateException("Player ${move.ownerId} not found")

        // Card ownership validation
        if (!player.hand.containsAll(move.cards)) {
            val missing = move.cards.filterNot { player.hand.contains(it) }
            throw IllegalArgumentException("Played cards are not in player's hand.")
        }

        // Evaluate current trick
        val lastPlayedCombo: PlayerHandCombination? = if (state.lastPlayedHand.isNotEmpty()) {
            BigTwoHandEvaluator().evaluateHand(state.lastPlayedHand)
        } else {
            null
        }

        // Evaluate new move
        val playedHandCombo = BigTwoHandEvaluator.Companion.evaluateHand(move.cards, lastPlayedCombo)
            ?: throw IllegalArgumentException("Invalid combination played.")

        // Validate move beats current trick
        if (!state.isFirstMoveOfGame && lastPlayedCombo != null) {
            require(BigTwoHandEvaluator().beats(playedHandCombo, lastPlayedCombo)) {
                "Played combination ${move.type} does not beat previous hand ${lastPlayedCombo.type}"
            }
        }

        // Update player hands AND clear selections
        val updatedPlayers = state.players.map { p ->
            if (p.id == move.ownerId) {
                val newHand = p.hand.toMutableList().apply {
                    removeAll(move.cards)
                }
                p.copy(
                    hand = newHand,
                    selectedCards = emptySet() // CRITICAL: Clear selections after play
                )
            } else {
                p
            }
        }

        // Check for winner
        val winnerId: String? = updatedPlayers.firstOrNull { it.hand.isEmpty() }?.id
        //val newRoundWinnerId: String? = if (player.hand.isEmpty()) player.id else player.id

        // Determine the game phase.
        // A player emptying their hand always ends the ROUND.
        // The GAME_OVER phase should be determined by higher-level game rules (e.g., score, number of rounds).
        //val determinedGamePhase: GamePhase = if (winnerId != null) GamePhase.ROUND_END else GamePhase.PLAYING
        //Log.d("Big2", "GameStateProcessor:: DeterminedGamePhase -> $determinedGamePhase")
        // Ensure gameWinnerId is NOT set here unless explicit overall game-winning conditions are met.
        // For most Big Two implementations, gameWinnerId is determined *after* a round ends and scores are tallied.
        //val finalGameWinnerId: String? = null // Explicitly set to null to prevent premature GAME_OVER

        // Update game state
        return state.copy(
            players = updatedPlayers,
            currentTrick = state.currentTrick + move.cards, // Accumulate trick if not game over, clear if winner
            lastPlayedHand = move.cards, // This player played the last hand for the current trick
            lastPlayerIdWhoPlayed = move.ownerId,
            trickHistory = state.trickHistory + TrickHistoryEntry(move.ownerId, move.cards, playedHandCombo.type),
            passCount = 0, // RESET: Clear pass count on a successful play
            playersWhoPassedInCurrentTrick = emptySet(), // RESET: Clear passes for a new trick
            roundWinnerId = winnerId,
            gameWinnerId = null, // This prevents the immediate GAME_OVER flicker
            currentPlayerId = state.nextPlayer(move.ownerId), // Next player in sequence (no skipping here)
            isFirstMoveOfGame = false
        )
    }

    private fun applyPassMove(move: PassMove): GameState {
        require(move.ownerId == state.currentPlayerId) {
            "Player ${move.ownerId} cannot pass when it's ${state.currentPlayerId}'s turn"
        }

        if (state.isFirstMoveOfGame) {
            throw IllegalArgumentException("Cannot pass on the first move of the game. The player with 3♦ must start.")
        }

        // Update passes for current trick
        val newPlayersWhoPassedInCurrentTrick = state.playersWhoPassedInCurrentTrick + move.ownerId
        val newPassCountForCurrentTrick = state.passCount + 1 // Assuming state.passCount is used for this purpose

        // Clear selections for passing player
        val updatedPlayers = state.players.map { p ->
            if (p.id == move.ownerId) p.copy(selectedCards = emptySet()) else p
        }

        // Determine if the trick ends:
        // 1. If all players *except* the player who played the last hand have passed.
        // 2. If the turn cycles back to the player who played the last hand (meaning everyone in between passed).
        val nextPlayerId = state.nextPlayer(move.ownerId)
        val allOtherPlayersPassed = newPlayersWhoPassedInCurrentTrick.size == state.players.size - 1
        val trickEndsByCyclingBackToLeader = state.lastPlayerIdWhoPlayed != null && nextPlayerId == state.lastPlayerIdWhoPlayed
        val trickEnds = trickEndsByCyclingBackToLeader || allOtherPlayersPassed

        return if (trickEnds) {
            //Log.d("Big2", "GameStateProcessor:: Trick ends. New leader: ${state.lastPlayerIdWhoPlayed ?: nextPlayerId}")
            state.copy(
                currentTrick = emptyList(), // Reset current trick
                lastPlayedHand = emptyList(), // No last played hand for new trick
                passCount = 0, // Reset pass count
                playersWhoPassedInCurrentTrick = emptySet(), // Reset passes for new trick
                isFirstMoveOfGame = false,
                currentPlayerId = state.lastPlayerIdWhoPlayed ?: nextPlayerId, // The player who played last leads the next trick, or next in sequence
                players = updatedPlayers // Clear selections
            )
        } else {
            state.copy(
                playersWhoPassedInCurrentTrick = newPlayersWhoPassedInCurrentTrick,
                passCount = newPassCountForCurrentTrick,
                currentPlayerId = nextPlayerId,
                players = updatedPlayers
            )
        }
    }
}