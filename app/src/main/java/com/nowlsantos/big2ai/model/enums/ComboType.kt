package com.nowlsantos.big2ai.model.enums

enum class ComboType(val rankOrder: Int) {
    SINGLE(0),
    PAIR(1),
    TRIPLE(2),
    STRAIGHT(5),
    FLUSH(6),
    FULL_HOUSE(7),
    FOUR_OF_A_KIND(9),
    STRAIGHT_FLUSH(10),
    NONE(0);

    fun isBomb(): Boolean = this == FOUR_OF_A_KIND || this == STRAIGHT_FLUSH
    fun isHighValueCombo(): Boolean = rankOrder >= STRAIGHT.rankOrder
    fun isFiveCardCombo(): Boolean {
        return when (this) {
            STRAIGHT, FLUSH, FULL_HOUSE,
            FOUR_OF_A_KIND, STRAIGHT_FLUSH -> true
            else -> false
        }
    }
}

// Add this extension property to ComboType.kt
val ComboType.rankOrderValue: Int
    get() = when (this) {
        ComboType.STRAIGHT_FLUSH -> 10
        ComboType.FOUR_OF_A_KIND -> 9
        ComboType.FULL_HOUSE -> 7
        ComboType.FLUSH -> 6
        ComboType.STRAIGHT -> 5
        else -> 0
    }