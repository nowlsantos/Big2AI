package com.nowlsantos.big2ai.model.states

import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.model.enums.ComboType

sealed class UiEffect {
    data class ShowWinner(val playerName: String) : UiEffect()
    data class ShowError(val message: String) : UiEffect()
    data class ShowAction(val message: String) : UiEffect()
    data class PlayerPassed(val message: String): UiEffect()
    object TriggerDealingAnimation : UiEffect()
    data class PlayCardsAnimationTrigger(val playerId: String, val cards: List<PlayingCard>, val combinationType: ComboType) : UiEffect()
    data class ShowGameOver(val message: String): UiEffect()
    object ClearMessage: UiEffect()
    // object NavigateToMenu : UiEffect() // Good for future navigation
}