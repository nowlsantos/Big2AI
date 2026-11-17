package com.nowlsantos.big2ai.model.data

import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.model.enums.ComboType

/**
 * Represents a found combination from a player's hand, including its type and the card
 * that determines its strength for comparison in Big Two.
 */
data class PlayerHandCombination(
    val cards: List<PlayingCard>,
    val type: ComboType,
    val leadCard: PlayingCard,
    val strength: Int
) {
    companion object {
        fun createSingle(card: PlayingCard): PlayerHandCombination {
            return PlayerHandCombination(
                cards = listOf(card),
                type = ComboType.SINGLE,
                leadCard = card,
                strength = card.strength
            )
        }

        fun createPair(cards: List<PlayingCard>): PlayerHandCombination {
            val maxCard = cards.maxBy { it.strength }!!
            return PlayerHandCombination(
                cards = cards,
                type = ComboType.PAIR,
                leadCard = maxCard,
                strength = maxCard.strength
            )
        }

        fun createTriple(cards: List<PlayingCard>): PlayerHandCombination {
            val maxCard = cards.maxBy { it.strength }!!
            return PlayerHandCombination(
                cards = cards,
                type = ComboType.TRIPLE,
                leadCard = maxCard,
                strength = maxCard.strength
            )
        }
    }
}