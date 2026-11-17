package com.nowlsantos.big2ai.model.states

import android.util.Log
import com.nowlsantos.big2ai.engine.BigTwoHandEvaluator
import com.nowlsantos.big2ai.model.data.Move
import com.nowlsantos.big2ai.model.data.PassMove
import com.nowlsantos.big2ai.model.data.PlayMove
import com.nowlsantos.big2ai.model.data.Player
import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.model.enums.Rank
import com.nowlsantos.big2ai.model.enums.Suit
import com.nowlsantos.big2ai.model.data.Deck
import com.nowlsantos.big2ai.model.enums.ComboType
import com.nowlsantos.big2ai.model.enums.GamePhase

// Represents an entry in the trick history, detailing who played what combo.
data class TrickHistoryEntry(
    val ownerId: String,
    val playedCards: List<PlayingCard>,
    val comboType: ComboType
)

data class GameState(
    val gamePhase: GamePhase,
    val deck: Deck,
    val players: List<Player>,
    val currentPlayerId: String,
    val roundNumber: Int,
    val lastPlayedHand: List<PlayingCard>,
    val lastPlayerIdWhoPlayed: String?,
    val roundWinnerId: String?,
    val gameWinnerId: String?,
    val isDealingAnimationPlaying: Boolean = false,
    val currentTrick: List<PlayingCard> = emptyList(),
    val passCount: Int = 0,
    val trickHistory: List<TrickHistoryEntry> = emptyList(),
    val isFirstMoveOfGame: Boolean = true,
    val playersWhoPassedInCurrentTrick: Set<String> = emptySet(),
) {
    companion object {
        val sharedHandEvaluator = BigTwoHandEvaluator()

        fun initial() = GameState(
            gamePhase = GamePhase.SETUP,
            deck = Deck.create(),
            players = emptyList(),
            currentPlayerId = "",
            roundNumber = 0,
            lastPlayedHand = emptyList(),
            lastPlayerIdWhoPlayed = null,
            roundWinnerId = null,
            gameWinnerId = null,
            isDealingAnimationPlaying = false,
            currentTrick = emptyList(),
            passCount = 0,
            trickHistory = emptyList(),
            isFirstMoveOfGame = true,
            playersWhoPassedInCurrentTrick = emptySet() // ADDED
        )
    }

    val currentPlayer: Player?
        get() = players.find { it.id == currentPlayerId }

    fun generateValidMoves(): List<Move> {
        val player = currentPlayer ?: return emptyList()
        val moves = mutableListOf<Move>()
        val threeDiamonds = PlayingCard(Rank.THREE, Suit.DIAMONDS)
        val currentCombo = if (lastPlayedHand.isNotEmpty()) sharedHandEvaluator.evaluateHand(lastPlayedHand) else null
        val possibleCombos = sharedHandEvaluator.findCombinations(player.hand)

        for (combo in possibleCombos) {
            val validCombo = BigTwoHandEvaluator.evaluateHand(
                combo.cards,
                currentCombo
            )

            if (validCombo != null) {
                if (isFirstMoveOfGame) {
                    if (validCombo.cards.contains(threeDiamonds)) {
                        moves.add(PlayMove(validCombo.cards, validCombo.type, player.id))
                    }
                } else {
                    moves.add(PlayMove(validCombo.cards, validCombo.type, player.id))
                }
            }
        }

        val canPass = !isFirstMoveOfGame && currentCombo != null
        if (canPass) {
            // Only add PassMove if there are no valid play moves OR if the player is allowed to pass
            // We add PassMove as an option if they can't play, or if there's a trick to pass on.
            // The UI will determine if the "Pass" button is enabled.
            if (moves.none { it is PlayMove } || !playersWhoPassedInCurrentTrick.contains(player.id)) {
                moves.add(PassMove(player.id))
            }
        }

        return moves
    }

    fun playMove(move: Move): GameState {
        return GameStateTransitionProcessor(this.clone()).apply(move)
    }

    fun clone(): GameState {
        return copy(
            players = players.map {
                it.copy(
                    hand = it.hand.toMutableList(), // Ensure mutable copy
                    selectedCards = it.selectedCards.toMutableSet()
                )
            },
            deck = deck.clone(),
            currentTrick = currentTrick.toMutableList(),
            trickHistory = trickHistory.map {
                it.copy(playedCards = it.playedCards.toMutableList())
            },
            playersWhoPassedInCurrentTrick = playersWhoPassedInCurrentTrick.toMutableSet() // Clone the new set
        )
    }
}

// Extension function to determine the next player in sequence, skipping those who have passed
fun GameState.nextPlayer(currentId: String): String {
    val currentIndex = players.indexOfFirst { it.id == currentId }
    if (currentIndex == -1) {
        //Log.e("Big2", "nextPlayer: Current player ID $currentId not found. Returning first player.")
        return players.firstOrNull()?.id ?: "" // Fallback
    }
    val nextIndex = (currentIndex + 1) % players.size
    return players[nextIndex].id
}