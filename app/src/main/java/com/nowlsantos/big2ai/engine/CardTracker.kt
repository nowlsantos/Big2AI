package com.nowlsantos.big2ai.engine

import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.model.states.GameState
import com.nowlsantos.big2ai.model.enums.Rank
import com.nowlsantos.big2ai.model.enums.Suit
import com.nowlsantos.big2ai.util.AppLogger

/**
 * Tracks cards that have been played in the game and maintains the pool of
 * currently un played cards (not in AI's hand, not on table, not in history).
 * This class focuses on factual card tracking, leaving probabilistic inference to OpponentModel.
 *
 * Optimized: Implements truly incremental updates.
 */
class CardTracker {
    private val tag = "CardTracker" // Define a tag for logging within this class

    // A constant set of all 52 cards in a standard deck. Used for resetting.
    private val all52CARDS: Set<PlayingCard> = run {
        val cards = mutableSetOf<PlayingCard>()
        Suit.entries.forEach { suit ->
            Rank.entries.forEach { rank ->
                cards.add(PlayingCard(rank, suit))
            }
        }
        cards
    }

    // Tracks all cards that are known to have been played in tricks so far.
    private val playedCardsInTricks = mutableSetOf<PlayingCard>()
    // Tracks all cards that are currently un-played (neither in playedTricks nor AI's hand).
    private val currentUnplayedCardsPool = mutableSetOf<PlayingCard>()
    // A mapping of player ID to their last known card count. Used by OpponentModel.
    private val playerLastKnownCardCounts = mutableMapOf<String, Int>()

    // Keep track of the last trick history size to process only new entries incrementally.
    private var lastTrickHistorySize = 0

    init {
        reset()
    }

    /**
     * Resets the card tracker to its initial state:
     * - No cards played.
     * - All 52 cards are considered unplayed.
     * - Player card counts cleared.
     * - Trick history size reset.
     */
    fun reset() {
        playedCardsInTricks.clear()
        currentUnplayedCardsPool.clear()
        currentUnplayedCardsPool.addAll(all52CARDS) // All cards are initially unplayed
        playerLastKnownCardCounts.clear()
        lastTrickHistorySize = 0 // Reset this as well
        AppLogger.d(tag, "CardTracker:: Reset. All cards in unplayed pool: ${currentUnplayedCardsPool.size}")
    }

    /**
     * Updates the card tracker based on the current game state.
     * This method identifies newly played cards and incrementally removes them from the unplayed pool.
     * It also updates known player card counts.
     *
     * @param gameState The current state of the game.
     * @param aiPlayerId The ID of the AI player whose hand is known (their cards are implicitly "tracked").
     */
    fun update(gameState: GameState, aiPlayerId: String) {
        AppLogger.d(tag, "CardTracker:: Updating incrementally with new game state for AI $aiPlayerId.")

        // 1. Update playedCardsInTricks and currentUnplayedCardsPool based on new trick history entries.
        // This processes only *new* tricks, avoiding reprocessing old ones.
        if (gameState.trickHistory.size > lastTrickHistorySize) {
            for (i in lastTrickHistorySize until gameState.trickHistory.size) {
                val trickEntry = gameState.trickHistory[i]
                trickEntry.playedCards.forEach { card ->
                    if (playedCardsInTricks.add(card)) { // Add returns true if element was new
                        currentUnplayedCardsPool.remove(card)
                        AppLogger.d(tag, "CardTracker:: New played card: $card. Unplayed pool size: ${currentUnplayedCardsPool.size}")
                    }
                }
            }
            lastTrickHistorySize = gameState.trickHistory.size
        }

        // 2. Remove AI's own hand cards from the unplayed pool (if they were dealt or changed).
        // This is important because the AI *knows* its own hand, so these cards are "assigned"
        // and shouldn't be considered part of the *unassigned* pool for opponent inference.
        // We only remove them if they are still in the unplayed pool.
        val aiPlayer = gameState.players.find { it.id == aiPlayerId }
        aiPlayer?.hand?.forEach { card ->
            if (currentUnplayedCardsPool.remove(card)) {
                AppLogger.d(tag, "CardTracker:: Removed AI card $card from un-played pool. New size: ${currentUnplayedCardsPool.size}")
            }
        }

        // 3. Update player card counts (essential for OpponentModel).
        // This must reflect the *current* number of cards each player holds.
        gameState.players.forEach { player ->
            playerLastKnownCardCounts[player.id] = player.numberOfCards
        }

        AppLogger.d(tag, "CardTracker:: Incremental update complete. " +
                "Total played cards: ${playedCardsInTricks.size}. " +
                "Remaining unassigned in pool: ${currentUnplayedCardsPool.size}.")
    }

    /**
     * Provides the current pool of cards that are not in the AI's hand and have not been played.
     * These cards are either in other opponents' hands or still in the deck (if deck were tracked).
     * This is the source for `OpponentModel` to infer opponent hands.
     * @return An immutable Set of PlayingCard objects.
     */
    fun getUnassignedCardsPool(): Set<PlayingCard> = currentUnplayedCardsPool.toSet()

    /**
     * Gets an immutable set of all cards that have been played in tricks so far.
     * @return A Set of PlayingCard objects.
     */
    fun getAllPlayedCards(): Set<PlayingCard> = playedCardsInTricks.toSet()

    /**
     * Gets the last known number of cards for a specific player (from GameState).
     * @param playerId The ID of the player.
     * @return The number of cards, or 0 if not tracked/player not found.
     */
    fun getPlayerCardCount(playerId: String): Int {
        return playerLastKnownCardCounts[playerId] ?: 0
    }

    fun deepCopy(): CardTracker {
        val newTracker = CardTracker()
        newTracker.playedCardsInTricks.addAll(this.playedCardsInTricks)
        newTracker.currentUnplayedCardsPool.addAll(this.currentUnplayedCardsPool)
        newTracker.playerLastKnownCardCounts.putAll(this.playerLastKnownCardCounts)
        newTracker.lastTrickHistorySize = this.lastTrickHistorySize
        return newTracker
    }
}