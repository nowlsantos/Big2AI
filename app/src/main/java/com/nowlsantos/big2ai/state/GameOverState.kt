package com.nowlsantos.big2ai.state

import com.nowlsantos.big2ai.model.states.GameInput
import com.nowlsantos.big2ai.model.enums.GamePhase
import com.nowlsantos.big2ai.model.states.UiEffect
import com.nowlsantos.big2ai.util.UI_MESSAGE_DELAY_MS
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.nowlsantos.big2ai.util.AppLogger
import kotlin.time.Duration.Companion.milliseconds

class GameOverState : GamePhaseState {
    override val phase: GamePhase = GamePhase.GAME_OVER
    private val tag = "GameOverState" // Define a tag for logging within this class

    override fun onEnter(context: GameStateMachine) {
        AppLogger.d(tag,"GameOverState:: Entering GAME_OVER phase. Game winner: " +
                "${context.gameState.value.players.find { it.id == context.gameState.value.gameWinnerId }?.name}")

        context.scope.launch {
            delay(UI_MESSAGE_DELAY_MS.milliseconds)
            context.emitUiEffect(UiEffect.ShowWinner(
                context.gameState.value.players.find { it.id == context.gameState.value.gameWinnerId }?.name ?: "Unknown"
            ))
        }
    }

    override fun onExit(context: GameStateMachine) {
        AppLogger.d(tag,"GameOverState:: Exiting GAME_OVER phase.")
    }

    override fun handleInput(context: GameStateMachine, input: GameInput) {
        when (input) {
            is GameInput.ResetGame -> resetGame(context)
            else -> super.handleInput(context, input)
        }
    }

    override fun resetGame(context: GameStateMachine) {
        AppLogger.d(tag,"GameOverState:: Resetting game to initial state.")
        context.resetGame()
    }
}