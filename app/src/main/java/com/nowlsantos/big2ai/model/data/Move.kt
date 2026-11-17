package com.nowlsantos.big2ai.model.data

import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.model.enums.Rank
import com.nowlsantos.big2ai.model.enums.ComboType

sealed class Move {
    abstract val cards: List<PlayingCard>
    abstract val type: ComboType
    abstract val ownerId: String // Who played this move (null for PassMove)

    /**
     * Returns the "leading card" for this move, used for comparison in Big Two rules.
     * For most combos, it's the highest card. For PassMove, it's null.
     */
    abstract fun leadCard(): PlayingCard?
}

/**
 * Represents a specific combination of cards played by a player.
 */
data class PlayMove(
    override val cards: List<PlayingCard>,
    override val type: ComboType,
    override val ownerId: String // Player ID who made this move
) : Move() {
    override fun leadCard(): PlayingCard? = when (type) {
        ComboType.SINGLE,
        ComboType.PAIR,
        ComboType.TRIPLE ->
            cards.maxByOrNull { it.strength }

        ComboType.STRAIGHT,
        ComboType.FLUSH,
        ComboType.FULL_HOUSE,
        ComboType.FOUR_OF_A_KIND,
        ComboType.STRAIGHT_FLUSH ->
            cards.sortedByDescending { it.strength }
                .firstOrNull { it.rank != Rank.TWO } // Avoid 2 as lead unless necessary
                ?: cards.maxByOrNull { it.strength }

        else -> null
    }

    val strength: Int
        get() = leadCard()?.strength ?: 0

}

/**
 * Represents a player choosing to pass their turn.
 */
data class PassMove(
    override val ownerId: String
) : Move() {
    override val cards: List<PlayingCard> = emptyList()
    override val type: ComboType = ComboType.NONE
    override fun leadCard(): PlayingCard? = null
}