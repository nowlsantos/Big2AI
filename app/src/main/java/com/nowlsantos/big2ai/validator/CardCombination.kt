// File: CardCombination.kt
package com.nowlsantos.big2ai.validator

import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.validator.CardCombination.FiveCardCombination.Type

sealed interface CardCombination : Comparable<CardCombination> {
    val cards: List<PlayingCard>
    val ranking: Int // For general combination type ranking

    data class Single(override val cards: List<PlayingCard>) : CardCombination {
        override val ranking = 1
        init { require(cards.size == 1) }
    }

    data class Pair(override val cards: List<PlayingCard>) : CardCombination {
        override val ranking = 2
        init { require(cards.size == 2 && cards[0].rank == cards[1].rank) }
    }

    data class Triple(override val cards: List<PlayingCard>) : CardCombination {
        override val ranking = 3 // Rank triples after pairs and before five-card hands
        init {
            require(cards.size == 3 &&
                    cards[0].rank == cards[1].rank &&
                    cards[1].rank == cards[2].rank)
        }
    }

    data class FiveCardCombination(
        override val cards: List<PlayingCard>,
        val type: Type
    ) : CardCombination {
        enum class Type(val ranking: Int) {
            STRAIGHT(4), // Shifted rankings due to new Triple type
            FLUSH(5),
            FULL_HOUSE(6),
            FOUR_OF_A_KIND(7),
            STRAIGHT_FLUSH(8);

            // RENAMED: 'compareRanking' instead of 'compareTo' to avoid overriding final method
            fun compareRanking(other: Type): Int {
                return this.ranking.compareTo(other.ranking)
            }
        }

        override val ranking = type.ranking
    }

    override fun compareTo(other: CardCombination): Int {
        // 1. Compare by general combination type (Single, Pair, Triple, FiveCardCombination)
        val generalTypeComparison = this.ranking.compareTo(other.ranking)
        if (generalTypeComparison != 0) {
            return generalTypeComparison
        }

        // 2. If general types are the same, compare by specific rules
        return when (this) {
            is Single -> {
                // For singles, compare the single card
                this.cards[0].compareTo(other.cards[0])
            }
            is Pair -> {
                // For pairs, compare the highest card (or any card as they are the same rank)
                this.cards[0].compareTo(other.cards[0])
            }
            is Triple -> {
                // For triples, compare the highest card (or any card as they are the same rank)
                this.cards[0].compareTo(other.cards[0])
            }
            is FiveCardCombination -> {
                // Both are FiveCardCombinations, compare by their specific type and then by highest card if types are same.
                val otherFiveCard = other as FiveCardCombination
                // UPDATED CALL: Use 'compareRanking' now
                val typeComparison = this.type.compareRanking(otherFiveCard.type)
                if (typeComparison != 0) {
                    typeComparison
                } else {
                    // If the 5-card types are the same, compare by the highest card within the combination.
                    // Special rules for Full House and Four of a Kind: compare by the rank of the set.
                    when (this.type) {
                        Type.STRAIGHT, Type.FLUSH, Type.STRAIGHT_FLUSH -> {
                            // For these, comparison is by the highest card in the combination
                            this.cards.last().compareTo(otherFiveCard.cards.last())
                        }
                        Type.FULL_HOUSE -> {
                            // Compare by the rank of the three-of-a-kind
                            val thisTripletRank = this.cards.groupBy { it.rank }.maxByOrNull { it.value.size }!!.key.value
                            val otherTripletRank = otherFiveCard.cards.groupBy { it.rank }.maxByOrNull { it.value.size }!!.key.value

                            val tripletComp = thisTripletRank.compareTo(otherTripletRank)
                            if (tripletComp != 0) {
                                tripletComp
                            } else {
                                // If triplets are same, compare by the rank of the pair (standard rule, though usually only triplet rank matters)
                                val thisPairRank = this.cards.groupBy { it.rank }.minByOrNull { it.value.size }!!.key.value
                                val otherPairRank = otherFiveCard.cards.groupBy { it.rank }.minByOrNull { it.value.size }!!.key.value
                                thisPairRank.compareTo(otherPairRank)
                            }
                        }
                        Type.FOUR_OF_A_KIND -> {
                            // Compare by the rank of the four-of-a-kind
                            val thisQuadRank = this.cards.groupBy { it.rank }.maxByOrNull { it.value.size }!!.key.value
                            val otherQuadRank = otherFiveCard.cards.groupBy { it.rank }.maxByOrNull { it.value.size }!!.key.value

                            val quadComp = thisQuadRank.compareTo(otherQuadRank)
                            if (quadComp != 0) {
                                quadComp
                            } else {
                                // If quads are same, compare by the kicker card (Big 2 standard rule)
                                val thisKicker = this.cards.first { it.rank.value != thisQuadRank }
                                val otherKicker = otherFiveCard.cards.first { it.rank.value != otherQuadRank }
                                thisKicker.compareTo(otherKicker)
                            }
                        }
                    }
                }
            }
        }
    }
}