package com.nowlsantos.big2ai.model.enums

/**
 * Represents the suit of a playing card.
 * The 'value' allows for comparison, common in Big 2 for tie-breaking.
 * A common hierarchy is Spades (highest) > Hearts > Clubs > Diamonds (lowest).
 */
enum class Suit(val strength: Int) {
    DIAMONDS(1), CLUBS(2), HEARTS(3), SPADES(4);

    fun toChar() = when(this) {
        DIAMONDS -> "♦"
        CLUBS -> "♣"
        HEARTS -> "♥"
        SPADES -> "♠"
    }
}