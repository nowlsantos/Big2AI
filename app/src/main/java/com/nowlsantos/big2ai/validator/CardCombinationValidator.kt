// File: CardCombinationValidator.kt
package com.nowlsantos.big2ai.validator

import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.model.enums.Rank.*
import com.nowlsantos.big2ai.model.enums.Suit.*
import com.nowlsantos.big2ai.validator.CardCombination.*
import com.nowlsantos.big2ai.validator.CardCombination.FiveCardCombination.Type

// NEW: Enum class for specific validation error types
enum class ValidationError {
    INVALID_CARD_COUNT,
    NOT_A_VALID_COMBINATION,
    CARDS_NOT_IN_HAND,
    NOT_CURRENT_PLAYER_TURN, // Though this check might live outside validator for game state
    INVALID_FIRST_MOVE,
    COMBINATION_TYPE_MISMATCH,
    COMBINATION_TOO_WEAK,
    CANNOT_PASS_ON_EMPTY_TRICK,
    PLAYER_HAS_PASSED, // Player has already passed in this trick
    UNKNOWN_ERROR // For fallback
}

// NEW: Data class to hold validation results, now including an error type
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val errorType: ValidationError? = null
)

object CardCombinationValidator {
    /**
     * Attempts to validate a list of cards as a Big2 combination.
     * Returns the appropriate CardCombination object if valid, otherwise null.
     */
    fun validateCombination(cards: List<PlayingCard>): CardCombination? {
        val sortedCards = cards.sorted() // Always work with sorted cards for consistent logic

        return when (sortedCards.size) {
            1 -> validateSingle(sortedCards)
            2 -> validatePair(sortedCards)
            3 -> validateTriple(sortedCards) // Check for 3-card hands
            5 -> validateFiveCardCombination(sortedCards)
            else -> null // Invalid number of cards for any Big2 combination
        }
    }

    /**
     * Comprehensive validation for a player's move.
     * @param playedCards The cards the current player intends to play.
     * @param currentPlayerHand The full hand of the current player.
     * @param lastPlayedCards The last combination played on the table. Empty list if it's a new trick.
     * @param isFirstMoveOfGame True if it's the very first move of the entire game.
     */
    fun validateMove(
        playedCards: List<PlayingCard>,
        currentPlayerHand: List<PlayingCard>,
        lastPlayedCards: List<PlayingCard>,
        isFirstMoveOfGame: Boolean,
        hasPlayerPassedThisTrick: Boolean // Added to distinguish from simply being able to pass
    ): ValidationResult {
        // 0. Check if player has already passed this trick
        if (hasPlayerPassedThisTrick) {
            return ValidationResult(false, "You have already passed this trick.", ValidationError.PLAYER_HAS_PASSED)
        }

        // 1. Validate the combination type and structure of the played cards
        val playedCombination = validateCombination(playedCards)
        if (playedCombination == null) {
            return ValidationResult(false, "Invalid card combination.", ValidationError.NOT_A_VALID_COMBINATION)
        }

        // 2. Ensure the player actually has these cards in their hand
        if (!currentPlayerHand.containsAll(playedCards)) {
            return ValidationResult(false, "Player does not have all specified cards in hand.", ValidationError.CARDS_NOT_IN_HAND)
        }

        // 3. Handle the very first move of the game (must play 3 of Diamonds)
        if (isFirstMoveOfGame) {
            val threeOfDiamonds = PlayingCard(THREE, DIAMONDS)
            if (playedCards.isNotEmpty() && !playedCards.contains(threeOfDiamonds)) {
                return ValidationResult(false, "The first move of the game must include the 3 of Diamonds.", ValidationError.INVALID_FIRST_MOVE)
            }
            // If it's the first move and contains 3D, it's valid so far.
            // Further checks (e.g. combination type) are implicitly handled by validateCombination
            // If the 3D is part of a valid combination, it's fine.
            return ValidationResult(true) // Valid first move including 3D
        }

        // 4. If it's not the first move of the game, compare with last played hand
        if (lastPlayedCards.isEmpty()) {
            // New trick, any valid combination is allowed
            return ValidationResult(true)
        } else {
            // Continue current trick, must beat the last played hand
            val lastPlayedCombination = validateCombination(lastPlayedCards)
            if (lastPlayedCombination == null) {
                // This state should ideally not be reached if game state is well-maintained
                return ValidationResult(false, "Error: Last played hand is invalid.", ValidationError.UNKNOWN_ERROR)
            }

            // A. Check if the card counts match (must play same number of cards)
            if (playedCombination.cards.size != lastPlayedCombination.cards.size) {
                return ValidationResult(false, "You must play the same number of cards as the last hand.", ValidationError.INVALID_CARD_COUNT)
            }

            // B. Check if the combination types match (e.g., cannot play a pair on a triple)
            if (playedCombination.javaClass != lastPlayedCombination.javaClass) {
                return ValidationResult(false, "You must play the same type of combination.", ValidationError.COMBINATION_TYPE_MISMATCH)
            }

            // C. Compare the played combination with the last one
            if (playedCombination <= lastPlayedCombination) {
                return ValidationResult(false, "Your combination is not strong enough.", ValidationError.COMBINATION_TOO_WEAK)
            }
        }

        // If all checks pass
        return ValidationResult(true)
    }

    private fun validateSingle(cards: List<PlayingCard>): Single? {
        return if (cards.size == 1) Single(cards) else null
    }

    private fun validatePair(cards: List<PlayingCard>): Pair? {
        return if (cards.size == 2 && cards[0].rank == cards[1].rank) Pair(cards) else null
    }

    private fun validateTriple(cards: List<PlayingCard>): Triple? {
        return if (cards.size == 3 && cards[0].rank == cards[1].rank && cards[1].rank == cards[2].rank) Triple(cards) else null
    }

    private fun validateFiveCardCombination(cards: List<PlayingCard>): FiveCardCombination? {
        if (cards.size != 5) return null

        // Checks should be ordered from strongest to weakest Big 2 5-card combination
        if (isStraightFlush(cards)) {
            return FiveCardCombination(cards, Type.STRAIGHT_FLUSH)
        }
        if (isFourOfAKind(cards)) {
            return FiveCardCombination(cards, Type.FOUR_OF_A_KIND)
        }
        if (isFullHouse(cards)) {
            return FiveCardCombination(cards, Type.FULL_HOUSE)
        }
        if (isFlush(cards)) {
            return FiveCardCombination(cards, Type.FLUSH)
        }
        if (isStraight(cards)) {
            return FiveCardCombination(cards, Type.STRAIGHT)
        }

        return null
    }

    private fun isFlush(cards: List<PlayingCard>): Boolean {
        if (cards.size != 5) return false
        val firstSuit = cards[0].suit
        return cards.all { it.suit == firstSuit }
    }

    private fun isStraight(cards: List<PlayingCard>): Boolean {
        if (cards.size != 5) return false

        // Scenario 1: Standard straight (consecutive ranks, 3-4-5-6-7, 10-J-Q-K-A, etc.)
        // Note: Ace is high in Big 2 (A-K-Q-J-10) and low (A-2-3-4-5) - this is crucial
        val sortedRanks = cards.map { it.rank.value }.sorted() // Use raw rank values for sorting

        var isConsecutive = true
        for (i in 0 until sortedRanks.size - 1) {
            val currentRankValue = sortedRanks[i]
            val nextRankValue = sortedRanks[i + 1]

            // Check for wrap-around for Ace high straight (A-K-Q-J-10) is handled by rank values
            // and regular consecutive check.
            if (nextRankValue != currentRankValue + 1) {
                isConsecutive = false
                break
            }
        }
        if (isConsecutive) return true

        // Scenario 2: Ace-low straight (A-2-3-4-5) - special Big2 rule
        // Ranks are mapped as A=1, 2=2, 3=3, 4=4, 5=5 for this specific check
        val ranksForLowAceStraight = cards.map {
            when (it.rank) {
                ACE -> 1
                TWO -> 2
                THREE -> 3
                FOUR -> 4
                FIVE -> 5
                else -> it.rank.value // Other ranks won't form this specific straight
            }
        }.sorted()

        // Check if the mapped ranks form the specific A-2-3-4-5 sequence
        if (ranksForLowAceStraight == listOf(1, 2, 3, 4, 5)) {
            return true
        }

        return false
    }

    private fun isFullHouse(cards: List<PlayingCard>): Boolean {
        if (cards.size != 5) return false
        val groups = cards.groupBy { it.rank }
        return groups.size == 2 &&
                groups.any { it.value.size == 3 } &&
                groups.any { it.value.size == 2 }
    }

    private fun isFourOfAKind(cards: List<PlayingCard>): Boolean {
        if (cards.size != 5) return false
        val groups = cards.groupBy { it.rank }
        // Must have two groups: one with 4 cards, one with 1 card (the kicker)
        return groups.size == 2 &&
                groups.any { it.value.size == 4 } &&
                groups.any { it.value.size == 1 }
    }

    private fun isStraightFlush(cards: List<PlayingCard>): Boolean {
        if (cards.size != 5) return false
        // A straight flush is both a straight and a flush
        return isStraight(cards) && isFlush(cards)
    }
}