package com.nowlsantos.big2ai.model.data

import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.model.enums.Rank
import com.nowlsantos.big2ai.model.enums.Suit
import kotlin.random.Random

class Deck {
    val cards = mutableListOf<PlayingCard>() // This is the instance variable

    companion object {
        /**
         * Creates a new unshuffled deck in Big 2 order (3♦ to 2♠)
         * Expert: Uses ArrayDeque for optimal insertion/removal
         */
        fun create(): Deck {
            val deck = Deck() // Create the Deck instance first

            // Populate the 'cards' property of the newly created deckInstance
            // Create in Big Two order: diamonds, clubs, hearts, spades
            Suit.entries.forEach { suit ->
                // Ranks from 3 to 2 (3,4,...,K,A,2)
                val orderedRanks = listOf(
                    Rank.THREE, Rank.FOUR, Rank.FIVE, Rank.SIX, Rank.SEVEN,
                    Rank.EIGHT, Rank.NINE, Rank.TEN, Rank.JACK, Rank.QUEEN,
                    Rank.KING, Rank.ACE, Rank.TWO
                )
                orderedRanks.forEach { rank ->
                    deck.cards.add(PlayingCard(rank, suit)) // Add to deckInstance.cards
                }
            }
            return deck // Return the populated deck instance
        }
    }

    /**
     * Shuffles deck using optimized Fisher-Yates algorithm
     * @param random Optional seeded random for deterministic testing
     */
    fun shuffle(random: Random = Random.Default) {
        val list = cards.toMutableList()
        for (i in list.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val temp = list[i]
            list[i] = list[j]
            list[j] = temp
        }
        cards.clear()
        cards.addAll(list)
    }

    /**
     * Deals a single card from the top of the deck
     * @throws IllegalStateException if deck is empty
     */
    fun deal(): PlayingCard {
        require(cards.isNotEmpty()) { "Cannot deal from empty deck" }
        return cards.removeAt(0)
    }

    /**
     * Resets deck to initial unshuffled state
     */
    fun reset() {
        cards.clear()
        // The fix here also benefits the reset function, as create() will now return a populated deck
        cards.addAll(create().cards)
    }

    /**
     * @return Number of remaining cards
     */
    fun size() = cards.size

    /**
     * @return True if deck has no cards
     */
    fun isEmpty() = cards.isEmpty()

    fun clone(): Deck {
        val newDeck = Deck()
        newDeck.cards.addAll(this.cards)
        return newDeck
    }
}