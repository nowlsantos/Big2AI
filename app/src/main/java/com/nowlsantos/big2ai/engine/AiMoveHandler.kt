package com.nowlsantos.big2ai.engine

import com.nowlsantos.big2ai.model.states.GameState
import com.nowlsantos.big2ai.model.data.PassMove
import com.nowlsantos.big2ai.model.data.PlayMove
import com.nowlsantos.big2ai.model.states.UiEffect
import com.nowlsantos.big2ai.model.states.UiEffect.*
import com.nowlsantos.big2ai.util.AI_THINK_DELAY_MS
import com.nowlsantos.big2ai.util.PASSED_DELAY_MS
import com.nowlsantos.big2ai.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class AiMoveHandler(
    private val scope: CoroutineScope,
    private val aiPlayers: Map<String, ExpertBig2AI>,
    private val onUpdateGameState: (GameState) -> Unit,
    private val onEmitUiEffect: (UiEffect) -> Unit,
    private val onTriggerNextAiTurn: () -> Unit, // Callback to trigger next AI turn
    private val getCurrentGameState: () -> GameState // Callback to get current game state
) {
    private val isPlayerThinking = AtomicBoolean(false)
    private var aiJob: Job? = null
    private val tag = "AiMoveHandler" // Define tag for logging within class methods
    /**
     * Triggers an AI player's turn if it's currently their turn and they are not human,
     * and the AI is not already in the process of thinking.
     * This function now encapsulates all AI-specific turn logic.
     */

    fun triggerAiTurnIfApplicable() {
        val gameState = getCurrentGameState()
        val currentPlayer = gameState.currentPlayer

        // Ensure the isPlayerThinking flag is reset before checking for a new AI turn
        // This handles cases where it might have been left true from a previous,
        // potentially interrupted, AI turn or an unexpected state.
        isPlayerThinking.set(false)
        AppLogger.d(tag, "AiMoveHandler:: Checking AI turn for ${currentPlayer?.name}. " +
                "Is human: ${currentPlayer?.isHuman}, " +
                "Is thinking: ${isPlayerThinking.get()}")

        // Only trigger AI if it's their turn AND they are not human AND AI is not already thinking
        if (currentPlayer != null && !currentPlayer.isHuman && !isPlayerThinking.get()) {
            val currentAi = aiPlayers[currentPlayer.id]
            if (currentAi == null) {
                AppLogger.e(tag, "AiMoveHandler: No AI instance found for player ID: ${currentPlayer.id}. Cannot make move.")
                scope.launch(Dispatchers.Main) { // Emit UI effect on Main thread
                    onEmitUiEffect(UiEffect.ShowError("Internal Error: AI not found for ${currentPlayer.name}"))
                }
                return
            }

            isPlayerThinking.set(true)
            onEmitUiEffect(UiEffect.ShowAction("${currentPlayer.name} is thinking..."))
            AppLogger.i(tag, "AiMoveHandler:: ${currentPlayer.name} is thinking...")

            aiJob = scope.launch(Dispatchers.Default) {
                try {
                    delay(AI_THINK_DELAY_MS) // Simulate thinking time
                    val aiMove = currentAi.makeMove(gameState)

                    withContext(Dispatchers.Main) {
                        onEmitUiEffect(UiEffect.ClearMessage)

                        // --- ADD THESE LOGGING LINES HERE FOR AI TURNS ---
                        if (aiMove is PlayMove) {
                            AppLogger.i(tag, "AiMoveHandler:: Move AI (${currentPlayer.name}) " +
                                    "attempting to play cards: ${aiMove.cards.joinToString { card -> "${card.rank.symbol}${card.suit.toChar()}" }}")
                        } else if (aiMove is PassMove) {
                            AppLogger.i(tag, "AiMoveHandler:: Move AI (${currentPlayer.name}) attempting to pass.")
                        }

                        // FIX: Use gameState.playMove() for AI moves as well
                        val newState = gameState.playMove(aiMove)

                        when (aiMove) {
                            is PlayMove -> {
                                AppLogger.d(tag, "AiMoveHandler:: AI Player ${currentPlayer.name} played: ${aiMove.cards.joinToString()}")
                                onEmitUiEffect(PlayCardsAnimationTrigger(aiMove.ownerId, aiMove.cards, aiMove.type))
                                onUpdateGameState(newState)
                            }
                            is PassMove -> {
                                AppLogger.d(tag, "AiMoveHandler:: AI Player ${currentPlayer.name} passed.")
                                onEmitUiEffect(PlayerPassed("${currentPlayer.name} passed."))
                                delay(PASSED_DELAY_MS) // Small delay for pass message
                                onEmitUiEffect(UiEffect.ClearMessage)
                                // For PassMove, trigger next AI turn immediately as no animation is awaited
                                onUpdateGameState(newState)
                                onTriggerNextAiTurn()
                            }
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e(tag, "AiMoveHandler:: Error during AI turn for ${currentPlayer.name}: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        onEmitUiEffect(UiEffect.ShowError("AI encountered an error: ${e.message}"))
                    }
                } finally {
                    isPlayerThinking.set(false) // Reset thinking flag
                }
            }
        } else {
            // NEW LOG: More detailed reason if AI turn is not applicable
            AppLogger.d(tag, "AiMoveHandler:: AI turn not applicable for ${currentPlayer?.name}. " +
                    "Is human: ${currentPlayer?.isHuman}, Is thinking: ${isPlayerThinking.get()}")
        }
    }

    /**
     * Cancels any ongoing AI turn processes.
     */

    fun cancelAiTurn() {
        aiJob?.cancel()
        isPlayerThinking.set(false)
        AppLogger.d(tag, "AiMoveHandler:: AI turn cancelled.")
    }
    /**
     * Resets the internal state of the AiMoveHandler.
     * It does NOT clear the aiPlayers map, as that's managed by GameStateMachine.
     */

    fun reset() { // <--- Simpler reset for AiMoveHandler's internal state
        cancelAiTurn()
        AppLogger.d(tag, "AiMoveHandler:: Internal state reset.")
    }
}
