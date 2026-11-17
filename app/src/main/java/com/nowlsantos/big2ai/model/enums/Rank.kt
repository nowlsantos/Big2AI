package com.nowlsantos.big2ai.model.enums

/**
 * Represents the rank of a playing card.
 * The 'value' indicates the strength of the rank in Big 2 (3 is lowest, 2 is highest).
 */
enum class Rank(val value: Int, val symbol: String) {
    THREE(0, "3"),
    FOUR(1, "4"),
    FIVE(2, "5"),
    SIX(3, "6"),
    SEVEN(4, "7"),
    EIGHT(5, "8"),
    NINE(6, "9"),
    TEN(7, "T"),
    JACK(8, "J"),
    QUEEN(9, "Q"),
    KING(10, "K"),
    ACE(11, "A"),
    TWO(12, "2"); // 2 is the highest rank in Big Two

    fun toChar(): String = symbol // New helper function
}