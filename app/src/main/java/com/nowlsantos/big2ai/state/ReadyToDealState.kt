package com.nowlsantos.big2ai.state


import com.nowlsantos.big2ai.model.states.GameInput
import com.nowlsantos.big2ai.model.enums.GamePhase
import com.nowlsantos.big2ai.model.states.UiEffect
import kotlinx.coroutines.launch
import com.nowlsantos.big2ai.util.AppLogger

class ReadyToDealState : GamePhaseState {
    private val tag = "ReadyToDealState" // Define a tag for logging within this class
    override val phase: GamePhase = GamePhase.READY_TO_DEAL

    override fun onEnter(context: GameStateMachine) {
        AppLogger.d(tag, "ReadyToDealState:: Entering READY_TO_DEAL phase.")
        context.setupNewRoundState() // This method will update GameState with new cards and AI setup.

        // Add 1-second delay before dealing
        context.scope.launch {
            context.emitUiEffect(UiEffect.TriggerDealingAnimation) // Tell UI to start animation
            AppLogger.d(tag, "ReadyToDealState:: Emitted TriggerDealingAnimation UiEffect.")
            // Given the current structure, DealState's onEnter also expects to trigger animation,
            // so we'll transition *to* DealState, and DealState will *then* manage the animation triggering.
            // Let's keep the transition to DEALING here as it was.
            context.transitionTo(GamePhase.DEALING)
        }
    }

    override fun onExit(context: GameStateMachine) {
        AppLogger.d(tag, "ReadyToDealState:: Exiting READY_TO_DEAL phase.")
    }

    override fun handleInput(context: GameStateMachine, input: GameInput) {
        when (input) {
            is GameInput.ResetGame -> resetGame(context)
            else -> super.handleInput(context, input) // Report invalid action for others
        }
    }

    override fun resetGame(context: GameStateMachine) {
        AppLogger.d(tag, "ReadyToDealState:: Resetting game to initial state.")
        // CHANGED: Use the new `resetGame` method
        context.resetGame()
    }
}