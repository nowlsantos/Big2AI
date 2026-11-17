package com.nowlsantos.big2ai.model.data

import com.nowlsantos.big2ai.model.enums.PlayerPosition
import com.nowlsantos.big2ai.model.data.PlayingCard

data class Player(
    val id: String,
    val name: String,
    val position: PlayerPosition,
    val hand: MutableList<PlayingCard>,
    val selectedCards: Set<PlayingCard> = emptySet(),
    val isHuman: Boolean,
    val numberOfCards: Int = hand.size,
    val hasPassed: Boolean = false,
    val score: Int = 100,
    val action: String = "",
    val roundsWon: Int = 0
) {
    fun copy(
        id: String = this.id,
        name: String = this.name,
        position: PlayerPosition = this.position,
        hand: MutableList<PlayingCard> = this.hand.toMutableList(),
        selectedCards: Set<PlayingCard> = this.selectedCards.toSet(),
        isHuman: Boolean = this.isHuman,
        hasPassed: Boolean = this.hasPassed,
        score: Int = this.score,
        action: String = this.action,
    ) = Player(
        id, name, position, hand, selectedCards, isHuman, hand.size, hasPassed, score, action, roundsWon
    )
}