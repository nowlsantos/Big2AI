package com.nowlsantos.big2ai.model.data

import com.nowlsantos.big2ai.model.enums.Rank
import com.nowlsantos.big2ai.model.enums.Suit

/**
 * Represents a playing card with a rank and a suit.
 * Implements Comparable to define the card order in Big 2:
 * - Rank is the primary sorting key (2s highest, 3s lowest).
 * - Suit is the secondary sorting key if ranks are equal.
 */
data class PlayingCard(
    val rank: Rank,
    val suit: Suit,
    val isFaceUp: Boolean = false
) : Comparable<PlayingCard> {
    /**
     * Custom comparison logic for Big Two:
     * 1. Compares by Rank (3 is lowest, 2 is highest).
     * 2. If Ranks are equal, compares by Suit (Clubs < Diamonds < Hearts < Spades).
     */
    override fun compareTo(other: PlayingCard): Int {
        val rankComparison = this.rank.value.compareTo(other.rank.value)
        if (rankComparison != 0) {
            return rankComparison
        }
        // If ranks are equal, compare by suit (ordinal value naturally orders them as defined in enum)
        return this.suit.ordinal.compareTo(other.suit.ordinal)
    }

    // Override equals and hashCode to only consider rank and suit for logical equality
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlayingCard

        if (rank != other.rank) return false
        if (suit != other.suit) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rank.hashCode()
        result = 31 * result + suit.hashCode()
        return result
    }

    override fun toString(): String {
        return "${this.rank} of ${this.suit}"
    }

    /**
     * Calculates the "strength" of the card. This is a numerical value
     * used by the AI for scoring and comparisons. It ensures higher ranks are always stronger,
     * and for same ranks, higher suits are stronger.
     * This property makes card comparisons consistent throughout the game logic.
     */
    val strength: Int
        get() {
            // Using rank.value (0 for 3, ..., 12 for 2) and suit.ordinal (0 for Clubs, ..., 3 for Spades)
            // Multiply rank.value by 4 (number of suits) to ensure rank is the primary sorting key.
            // Add suit.ordinal to break ties for cards of the same rank.
            return rank.value * 4 + suit.ordinal
        }
}