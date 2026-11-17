package com.nowlsantos.big2ai.engine

import com.nowlsantos.big2ai.model.enums.ComboType
import com.nowlsantos.big2ai.model.data.PlayerHandCombination
import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.model.enums.Rank

class BigTwoHandEvaluator {
    private val tag = "BigTwoHandEvaluator"

    // Define custom Big Two rank order (3 lowest, 2 highest)
    private val rankOrder = listOf(
        Rank.THREE, Rank.FOUR, Rank.FIVE, Rank.SIX, Rank.SEVEN, Rank.EIGHT,
        Rank.NINE, Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING, Rank.ACE, Rank.TWO
    )

    companion object {
        fun evaluateHand(
            cards: List<PlayingCard>,
            currentTrick: PlayerHandCombination?
        ): PlayerHandCombination? {
            val evaluator = BigTwoHandEvaluator()
            val combo = evaluator.evaluateHand(cards) ?: return null
            if (currentTrick == null) return combo

            return if (evaluator.beats(combo, currentTrick)) combo else null
        }
    }

    fun findCombinations(hand: List<PlayingCard>): List<PlayerHandCombination> {
        if (hand.isEmpty()) return emptyList()

        val combinations = mutableListOf<PlayerHandCombination>()
        val sortedHand = hand.sortedBy { it.strength }

        // Singles
        sortedHand.forEach { combinations.add(createSingle(it)) }

        // Pairs and Triples
        sortedHand.groupBy { it.rank }.values.forEach { group ->
            if (group.size >= 2) {
                combinations.add(createPair(group.take(2)))
            }
            if (group.size >= 3) {
                combinations.add(createTriple(group.take(3)))
            }
        }

        // Five-card combinations
        if (hand.size >= 5) {
            combinations.addAll(findFiveCardCombinations(hand))
        }

        return combinations.distinctBy { it.cards.sortedBy { it.strength }.joinToString() }
    }

    fun evaluateHand(cards: List<PlayingCard>): PlayerHandCombination? {
        return when (cards.size) {
            1 -> createSingle(cards[0])
            2 -> if (isPair(cards)) createPair(cards) else null
            3 -> if (isTriple(cards)) createTriple(cards) else null
            5 -> evaluateFiveCardCombo(cards)
            else -> null
        }
    }

    fun isPair(cards: List<PlayingCard>): Boolean {
        return cards.size == 2 && cards[0].rank == cards[1].rank
    }

    fun beats(moveCombo: PlayerHandCombination, lastPlayedHand: PlayerHandCombination): Boolean {
        // Bomb vs non-bomb handling
        if (moveCombo.type.isBomb() != lastPlayedHand.type.isBomb()) {
            return moveCombo.type.isBomb()
        }

        // Card count mismatch
        if (moveCombo.cards.size != lastPlayedHand.cards.size) return false

        // Special flush comparison
        if (moveCombo.type == ComboType.FLUSH && lastPlayedHand.type == ComboType.FLUSH) {
            return compareFlushes(moveCombo.cards, lastPlayedHand.cards)
        }

        // Five-card combo type hierarchy
        if (moveCombo.cards.size == 5 && lastPlayedHand.cards.size == 5 &&
            moveCombo.type != lastPlayedHand.type) {
            return moveCombo.type.rankOrder > lastPlayedHand.type.rankOrder
        }

        // Same type comparison
        return moveCombo.strength > lastPlayedHand.strength
    }

    private fun evaluateFiveCardCombo(cards: List<PlayingCard>): PlayerHandCombination? {
        return findStraightFlush(cards) ?: findFourOfAKind(cards) ?:
        findFullHouse(cards) ?: findFlush(cards) ?: findStraight(cards)
    }

    private fun findFiveCardCombinations(hand: List<PlayingCard>): List<PlayerHandCombination> {
        return hand.combinations(5).mapNotNull { evaluateFiveCardCombo(it) }.distinctBy {
            it.cards.sortedBy { it.strength }.joinToString()
        }
    }

    private fun findStraightFlush(cards: List<PlayingCard>): PlayerHandCombination? {
        if (!isFlush(cards) || !isStraight(cards)) return null
        val comboType = ComboType.STRAIGHT_FLUSH
        return if (isWheelStraight(cards)) {
            // For A-2-3-4-5, the '5' card determines its relative strength among straights (lowest)
            val fiveCard = cards.first { it.rank == Rank.FIVE }
            PlayerHandCombination(cards, comboType, fiveCard, (1000.0 + fiveCard.strength).toInt())
        } else {
            val maxCard = cards.maxBy { it.strength }
            PlayerHandCombination(cards, comboType, maxCard, (1000.0 + maxCard.strength).toInt())
        }
    }

    private fun findFourOfAKind(cards: List<PlayingCard>): PlayerHandCombination? {
        val groups = cards.groupBy { it.rank }
        val fourGroup = groups.values.firstOrNull { it.size == 4 } ?: return null
        val maxCard = fourGroup.maxBy { it.strength }
        return PlayerHandCombination(
            cards = cards,
            type = ComboType.FOUR_OF_A_KIND,
            leadCard = maxCard,
            strength = 800 + (maxCard.strength)
        )
    }

    private fun findFullHouse(cards: List<PlayingCard>): PlayerHandCombination? {
        val groups = cards.groupBy { it.rank }.values
        val tripleGroup = groups.firstOrNull { it.size == 3 } ?: return null
        val pairGroup = groups.firstOrNull { it.size == 2 } ?: return null

        val tripleRank = rankOrder.indexOf(tripleGroup.first().rank)
        val pairRank = rankOrder.indexOf(pairGroup.first().rank)
        return PlayerHandCombination(
            cards = cards,
            type = ComboType.FULL_HOUSE,
            leadCard = tripleGroup.maxBy { it.strength },
            strength = tripleRank * 100 + pairRank
        )
    }

    private fun findFlush(cards: List<PlayingCard>): PlayerHandCombination? {
        if (!isFlush(cards)) return null
        val maxCard = cards.maxBy { it.strength }
        return PlayerHandCombination(
            cards = cards,
            type = ComboType.FLUSH,
            leadCard = maxCard,
            strength = maxCard.strength
        )
    }

    private fun findStraight(cards: List<PlayingCard>): PlayerHandCombination? {
        if (!isStraight(cards)) return null
        val comboType = ComboType.STRAIGHT
        return if (isWheelStraight(cards)) {
            // For A-2-3-4-5, the '5' card determines its relative strength among straights (lowest)
            val fiveCard = cards.first { it.rank == Rank.FIVE }
            PlayerHandCombination(cards, comboType, fiveCard, fiveCard.strength)
        } else {
            val maxCard = cards.maxBy { it.strength }
            PlayerHandCombination(cards, comboType, maxCard, maxCard.strength)
        }
    }

    private fun createSingle(card: PlayingCard): PlayerHandCombination {
        return PlayerHandCombination(
            cards = listOf(card),
            type = ComboType.SINGLE,
            leadCard = card,
            strength = card.strength
        )
    }

    private fun createPair(cards: List<PlayingCard>): PlayerHandCombination {
        val maxCard = cards.maxBy { it.strength }
        return PlayerHandCombination(
            cards = cards,
            type = ComboType.PAIR,
            leadCard = maxCard,
            strength = maxCard.strength
        )
    }

    private fun createTriple(cards: List<PlayingCard>): PlayerHandCombination {
        val maxCard = cards.maxBy { it.strength }
        return PlayerHandCombination(
            cards = cards,
            type = ComboType.TRIPLE,
            leadCard = maxCard,
            strength = maxCard.strength
        )
    }

    private fun isTriple(cards: List<PlayingCard>): Boolean {
        return cards.size == 3 && cards.distinctBy { it.rank }.size == 1
    }

    private fun isFlush(cards: List<PlayingCard>): Boolean {
        return cards.size == 5 && cards.map { it.suit }.distinct().size == 1
    }

    private fun isStraight(cards: List<PlayingCard>): Boolean {
        if (cards.size != 5) return false
        val ranks = cards.map { rankOrder.indexOf(it.rank) }.sorted()
        // Check for consecutive ranks OR the specific wheel straight (A-2-3-4-5)
        return (ranks == (ranks[0]..ranks[0]+4).toList()) || isWheelStraight(cards)
    }

    private fun isWheelStraight(cards: List<PlayingCard>): Boolean {
        val ranks = cards.map { rankOrder.indexOf(it.rank) }.sorted()
        // The only valid "wheel" straight is A-2-3-4-5.
        // In rankOrder: 3(0), 4(1), 5(2), ..., Ace(11), 2(12)
        // So A-2-3-4-5 corresponds to indices [0, 1, 2, 11, 12] when sorted.
        return ranks == listOf(
            rankOrder.indexOf(Rank.THREE),
            rankOrder.indexOf(Rank.FOUR),
            rankOrder.indexOf(Rank.FIVE),
            rankOrder.indexOf(Rank.ACE),
            rankOrder.indexOf(Rank.TWO)
        ).sorted()
    }

    private fun compareFlushes(moveCards: List<PlayingCard>, lastCards: List<PlayingCard>): Boolean {
        val sortedMove = moveCards.sortedByDescending { it.strength }
        val sortedLast = lastCards.sortedByDescending { it.strength }
        // Compare flushes by their highest card, then by suit if ranks are equal.
        // This assumes PlayingCard.strength already incorporates suit for tie-breaking.
        return sortedMove.zip(sortedLast).any { (mine, theirs) ->
            mine.strength > theirs.strength
        }
    }

    private fun <T> List<T>.combinations(k: Int): List<List<T>> {
        val combinations = mutableListOf<List<T>>()
        fun backtrack(start: Int, current: MutableList<T>) {
            if (current.size == k) {
                combinations.add(ArrayList(current))
                return
            }
            for (i in start until size) {
                current.add(this[i])
                backtrack(i + 1, current)
                current.removeAt(current.size - 1)
            }
        }
        backtrack(0, mutableListOf())
        return combinations
    }
}
