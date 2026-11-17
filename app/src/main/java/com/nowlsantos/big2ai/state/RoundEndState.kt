package com.nowlsantos.big2ai.state

import com.nowlsantos.big2ai.model.enums.GamePhase
import com.nowlsantos.big2ai.model.states.GameInput
import com.nowlsantos.big2ai.model.states.GameState
import com.nowlsantos.big2ai.model.data.Player
import com.nowlsantos.big2ai.model.states.UiEffect
import com.nowlsantos.big2ai.util.MAX_GAME_SCORE_THRESHOLD
import com.nowlsantos.big2ai.util.ROUND_WINNER_DELAY
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.nowlsantos.big2ai.util.AppLogger
import com.nowlsantos.big2ai.util.ScoreCalculator

class RoundEndState : GamePhaseState {
    override val phase: GamePhase = GamePhase.ROUND_END
    private val tag = "RoundEndState" // Define a tag for logging within this class

    override fun onEnter(context: GameStateMachine) {
        val gameState = context.gameState.value
        val roundWinner = gameState.players.find { it.id == gameState.roundWinnerId }
        AppLogger.d(tag, "RoundEndState:: Entering ROUND_END | Winner -> ${roundWinner?.name}.")

        context.scope.launch {
            delay(ROUND_WINNER_DELAY)
            context.emitUiEffect(UiEffect.ShowWinner("Round winner: ${roundWinner?.name}!"))
        }

        // Call the refactored function to handle all round-end processing
        processRoundEndScoringAndTransition(context, gameState)
    }

    override fun onExit(context: GameStateMachine) {
        AppLogger.d(tag, "RoundEndState:: Exiting ROUND_END phase.")
    }

    override fun handleInput(context: GameStateMachine, input: GameInput) {
        when (input) {
            is GameInput.ReadyForNextRound -> readyForNextRound(context)
            //is GameInput.ResetGame -> resetGame(context) // Allow resetting from round end
            else -> super.handleInput(context, input) // Report invalid action for others
        }
    }

    /**
     * Refactored function to calculate scores, update game state with scores,
     * check for game over, and transition the game phase accordingly.
     *
     * @param context The GameStateMachine instance to interact with.
     * @param gameState The GameState at the beginning of the round end.
     */
    private fun processRoundEndScoringAndTransition(context: GameStateMachine, gameState: GameState) {
        // Use the ScoreCalculator utility to get the updated GameState with scores and incremented round number
        val gameStateAfterScoring = ScoreCalculator.calculateRoundScores(gameState)

        // Update GameState in the context with the new scores and incremented round number.
        context.updateGameState(gameStateAfterScoring)

        // Now, use the players from gameStateAfterScoring for game over check
        val updatedPlayersAfterScoring = gameStateAfterScoring.players

        // Check for Game Over condition
        // The game winner is the player with the lowest score at the end of the round.
        val gameWinner: Player? = updatedPlayersAfterScoring.maxByOrNull { it.score }
        //val isGameOver = updatedPlayersAfterScoring.any { it.score >= MAX_GAME_SCORE_THRESHOLD }
        val isGameOver = updatedPlayersAfterScoring.any { it.score <= 0 }

        if (isGameOver) {
            val finalGameWinnerId = gameWinner?.id

            AppLogger.d(tag, "RoundEndState:: Game Over! Final scores: " +
                    "${updatedPlayersAfterScoring.joinToString { "${it.name}: ${it.score}" }}. " +
                    "Game Winner: ${gameWinner?.name ?: "N/A"}")
            // Set game winner ID in GameState and transition to GAME_OVER phase
            context.updateGameState(context.gameState.value.copy(gameWinnerId = finalGameWinnerId))
            context.transitionTo(GamePhase.GAME_OVER)
        }
        AppLogger.d(tag, "RoundEndState:: Current scores after processing: ${updatedPlayersAfterScoring.joinToString {"${it.name}: ${it.score}" }}")
    }

    // --- readyForNextRound now simply transitions ---
    override fun readyForNextRound(context: GameStateMachine) {
        val gameState = context.gameState.value
        if (gameState.gameWinnerId != null) {
            AppLogger.d(tag, "RoundEndState:: Game is already over. ResetGame should be used.")
            return
        }
        AppLogger.d(tag, "RoundEndState:: Ready for next round received. Transitioning to READY_TO_DEAL.")
        context.transitionTo(GamePhase.READY_TO_DEAL) // <-- Just transition here
    }

    override fun resetGame(context: GameStateMachine) {
        AppLogger.d(tag, "RoundEndState:: Resetting game to initial state.")
        context.resetGame()
    }
}