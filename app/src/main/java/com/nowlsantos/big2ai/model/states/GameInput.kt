package com.nowlsantos.big2ai.model.states

import com.nowlsantos.big2ai.model.data.Player
import com.nowlsantos.big2ai.model.data.PlayingCard

/**
 * Represents all possible inputs/actions that can be applied to the core game state.
 * These are consumed by the GameStateMachine to drive game logic transitions.
 */
sealed class GameInput { // Renamed from UiEvent to GameInput to reflect its role as input to core game logic
    data class AddPlayer(val player: Player) : GameInput()
    data class SetPlayerReady(val playerId: String, val isReady: Boolean) : GameInput()
    data class PlayCards(val playerId: String, val cards: List<PlayingCard>) : GameInput()
    data class PassTurn(val playerId: String) : GameInput()
    data class CardSelection(var card: PlayingCard): GameInput()
    data object ReadyForNextRound : GameInput()
    data object StartGame : GameInput()
    data object DealingAnimationFinished : GameInput()
    data object ResetGame : GameInput()
    data object SortBySuit : GameInput()
    data object SortByRank : GameInput()
    data object PlayCardsAnimationFinished : GameInput()
}