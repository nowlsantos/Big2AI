package com.nowlsantos.big2ai.state

import com.nowlsantos.big2ai.model.enums.GamePhase
import com.nowlsantos.big2ai.model.states.GameInput
import com.nowlsantos.big2ai.util.AppLogger

class DealState : GamePhaseState {
    private val tag = "DealState" // Define a tag for logging within this class
    override val phase: GamePhase = GamePhase.DEALING

    override fun onEnter(context: GameStateMachine) {
        AppLogger.d(tag, "DealState:: Entering DEALING phase. Dealing cards...")

        // The GameState (with new deck and player hands) is ALREADY updated
        // by GameStateMachine.setupNewRoundState(), which was called by ReadyToDealState.
        // The UI dealing animation (if any) is triggered by UiEffect.TriggerDealingAnimation
        // emitted from ReadyToDealState.
        // DealState's responsibility here is primarily to manage its phase.
        // No direct animation state setting in GameState from here.

        //val gameState = context.gameState.value
        //AppLogger.d(tag, "Current player for dealing: ${context.gameState.value.players.find { it.id == context.gameState.value.roundWinnerId }?.name}")
    }

    override fun onExit(context: GameStateMachine) {
        AppLogger.d(tag, "Exiting DEALING phase.")
    }

    override fun handleInput(context: GameStateMachine, input: GameInput) {
        when (input) {
            is GameInput.DealingAnimationFinished -> dealingAnimationFinished(context)
            is GameInput.ResetGame -> resetGame(context) // Allow resetting from dealing
            else -> super.handleInput(context, input) // Report invalid action for others
        }
    }

    override fun dealingAnimationFinished(context: GameStateMachine) {
        AppLogger.d(tag, "Dealing animation finished.")
        // DealState is responsible for transitioning to PLAYING once dealing animation is done.
        context.transitionTo(GamePhase.PLAYING)
    }

    override fun resetGame(context: GameStateMachine) {
        AppLogger.d(tag, "Resetting game to initial state.")
        context.resetGame()
    }
}