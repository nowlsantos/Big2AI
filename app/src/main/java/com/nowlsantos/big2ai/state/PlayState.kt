package com.nowlsantos.big2ai.state

import com.nowlsantos.big2ai.engine.BigTwoHandEvaluator
import com.nowlsantos.big2ai.engine.AiMoveHandler
import com.nowlsantos.big2ai.engine.CardValidation
import com.nowlsantos.big2ai.model.states.GameInput
import com.nowlsantos.big2ai.model.enums.GamePhase
import com.nowlsantos.big2ai.model.states.GameState
import com.nowlsantos.big2ai.model.data.PassMove
import com.nowlsantos.big2ai.model.data.PlayMove
import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.model.states.UiEffect
import com.nowlsantos.big2ai.util.UI_MESSAGE_DELAY_MS
import com.nowlsantos.big2ai.util.AppLogger
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class PlayState : GamePhaseState {
    override val phase: GamePhase = GamePhase.PLAYING
    private val isPlayAnimationRunning = AtomicBoolean(false) // Keep if used for internal state/logic
    private val pendingMove = AtomicReference<Pair<PlayMove, GameState>?>(null) // Keep if used for internal state/logic
    private var aiMoveHandler: AiMoveHandler? = null
    private val tag = "PlayState" // Define a tag for logging within this class

    init {
        AppLogger.d(tag, "Instance created: ${this.hashCode()}")
    }

    override fun onEnter(context: GameStateMachine) {
        AppLogger.d(tag, "Entering PLAYING phase.")
        val gameState = context.gameState.value
        context.emitUiEffect(UiEffect.ClearMessage) // Clear any previous messages

        // Initialize AiMoveHandler here. It will handle AI turn triggering.
        aiMoveHandler = AiMoveHandler(
            scope = context.scope,
            aiPlayers = context.aiPlayers,
            onUpdateGameState = { newState -> context.updateGameState(newState) },
            onEmitUiEffect = { effect -> context.emitUiEffect(effect) },
            onTriggerNextAiTurn = { aiMoveHandler?.triggerAiTurnIfApplicable() }, // AI triggers itself or next AI
            getCurrentGameState = { context.gameState.value }
        )

        val currentPlayer = gameState.players.find { it.id == gameState.currentPlayerId }
        // If it's an AI's turn on entering PlayState, trigger it.
        if (currentPlayer != null && !currentPlayer.isHuman) {
            aiMoveHandler?.triggerAiTurnIfApplicable() // Direct call to AI handler
        }
    }

    override fun onExit(context: GameStateMachine) {
        aiMoveHandler?.cancelAiTurn() // Ensure any ongoing AI turns are canceled
        aiMoveHandler = null
        pendingMove.set(null)
        isPlayAnimationRunning.set(false)
        AppLogger.d(tag, "Exiting PLAYING phase.")
        context.emitUiEffect(UiEffect.ClearMessage)
    }

    override fun handleInput(context: GameStateMachine, input: GameInput) {
        when (input) {
            is GameInput.PlayCards -> playCards(context, input.playerId, input.cards)
            is GameInput.PlayCardsAnimationFinished -> handleAnimationFinished(context)
            is GameInput.PassTurn -> passTurn(context, input.playerId)
            is GameInput.SortByRank -> sortByRank(context)
            is GameInput.SortBySuit -> sortBySuit(context)
            is GameInput.ReadyForNextRound -> {/* not handled in this state */}
            is GameInput.ResetGame -> resetGame(context)
            else -> AppLogger.d(tag, "Input ${input.javaClass.simpleName} not handled in PlayState")
        }
    }
    /**
     * Handles the completion of the play cards animation.
     * This method is called by GameStateMachine when it receives a PlayCardsAnimationFinished input.
     */
    private fun handleAnimationFinished(context: GameStateMachine) {
        val gameState = context.gameState.value
        AppLogger.i(tag, "Animation finished. Current player (after animation): ${gameState.currentPlayer?.name}")

        val currentPlayer = gameState.currentPlayer
        if (currentPlayer != null) {
            // Check for round winner immediately after animation finishes and state is updated
            val roundWinner = gameState.players.find { it.id == gameState.roundWinnerId }
            roundWinner?.let {
                AppLogger.e(tag, "Round Winner detected -> ${it.name}. Transitioning to ROUND_END.")
                context.transitionTo(GamePhase.ROUND_END)
                return
            }

            // If no round winner, trigger next AI turn if current player is AI, otherwise wait for human.
            if (!currentPlayer.isHuman) {
                AppLogger.d(tag, "Current player is AI (${currentPlayer.name}). Triggering AI turn.")
                aiMoveHandler?.triggerAiTurnIfApplicable() // Trigger the next AI turn
            } else {
                AppLogger.d(tag, "It's Human player's turn (${currentPlayer.name}). Waiting for human input.")
            }
        } else {
            AppLogger.e(tag, "handleAnimationFinished: Current player is null after animation.")
        }
    }

    /**
     * Handles a player playing cards. Contains robust validation and turn progression logic.
     */
    override fun playCards(context: GameStateMachine, playerId: String, cards: List<PlayingCard>) {
        AppLogger.d(tag, "PlayCards handleInput - Received PlayCards from VM. Player ID: $playerId, Cards: ${cards.joinToString()}")
        val gameState = context.gameState.value
        val currentPlayer = gameState.players.find { it.id == playerId }

        AppLogger.d(tag, "PlayCards playCards: " +
                "Validating play by ${currentPlayer?.name} " +
                "(Human: ${currentPlayer?.isHuman}) with ${cards.joinToString()}")

        // Use CardValidation object for validating the move against game rules
        val validationError = CardValidation.validatePlayMove(
            gameState = gameState,
            player = currentPlayer!!,
            cards = cards,
            handEvaluator = BigTwoHandEvaluator() // Pass an instance of your hand evaluator
        )

        if (validationError != null) {
            // Differentiate error handling based on player type
            if (currentPlayer.isHuman) {
                AppLogger.w(tag, "Invalid play by $playerId with ${cards.joinToString()}: $validationError")
                context.emitUiEffect(UiEffect.ShowError(validationError))
            } else {
                // If AI makes an invalid move, log it as a critical error but don't show to user.
                AppLogger.e(tag, "AI BUG: AI Player ${currentPlayer.name} (${currentPlayer.id}) attempted an invalid move: $validationError. Cards: ${cards.joinToString()}")
            }
            return // Stop processing invalid move
        }

        // If validation passes, apply the move
        val attemptedPlayCombo = BigTwoHandEvaluator().evaluateHand(cards) // Re-evaluate once validation is passed
        val playMove = PlayMove(cards, attemptedPlayCombo!!.type, playerId) // !! is safe here because validation passed
        val newState = gameState.playMove(playMove) // Applies move, updates hand, lastPlayedHand, etc.
        context.updateGameState(newState) // Update the central game state

        //context.emitUiEffect(UiEffect.ShowAction("${currentPlayer.name} played a ${attemptedPlayCombo.type}.")) // Commented out in original
        context.triggerPlayCardsAnimation(playMove.cards, playMove.ownerId, playMove.type)
    }

    /**
     * Handles a player passing their turn.
     */
    override fun passTurn(context: GameStateMachine, playerId: String) {
        val gameState = context.gameState.value
        val currentPlayer = gameState.currentPlayer // Get current player here

        // Ensure currentPlayer exists and matches the input playerId
        if (currentPlayer == null || currentPlayer.id != playerId) {
            AppLogger.w(tag, "Invalid pass input. Not current player's turn or player not found: $playerId")
            if (currentPlayer?.isHuman == true) { // Check if the *attempting* player is human
                context.emitUiEffect(UiEffect.ShowError("It's not your turn to pass."))
            } else {
                AppLogger.e(tag, "AI BUG: AI Player $playerId (or unknown) attempted to pass out of turn.")
            }
            return
        }

        // Validation: Cannot pass if leading a trick (table is empty)
        if (gameState.lastPlayedHand.isEmpty()) {
            if (currentPlayer.isHuman) {
                AppLogger.w(tag, "Player $playerId tried to pass when leading! This is not allowed. Ignoring move.")
                context.emitUiEffect(UiEffect.ShowError("You cannot pass when leading a trick!"))
            } else {
                AppLogger.e(tag, "AI BUG: AI Player ${currentPlayer.name} (${currentPlayer.id}) attempted to pass when leading!")
            }
            return
        }

        // Validation: Cannot pass on the very first move of the game (3♦ rule)
        if (gameState.isFirstMoveOfGame) {
            if (currentPlayer.isHuman) {
                AppLogger.w(tag, "Player $playerId tried to pass on first move of game. Not allowed.")
                context.emitUiEffect(UiEffect.ShowError("Cannot pass on the first move of the game. The player with 3♦ must start."))
            } else {
                AppLogger.e(tag, "AI BUG: AI Player ${currentPlayer.name} (${currentPlayer.id}) attempted to pass on the first move of the game!")
            }
            return
        }

        AppLogger.d(tag, "PassTurn by ${currentPlayer.name} (Human: ${currentPlayer.isHuman})")

        context.scope.launch {
            context.emitUiEffect(UiEffect.PlayerPassed("${currentPlayer.name} passed."))
            delay(UI_MESSAGE_DELAY_MS.milliseconds) // Small delay for message visibility
            context.emitUiEffect(UiEffect.ClearMessage) // Clear message after delay

            val passMove = PassMove(playerId)
            val newStateAfterPass = gameState.playMove(passMove) // Applies pass, adds to playersWhoPassedInCurrentTrick
            context.updateGameState(newStateAfterPass) // Update the central game state

            val trickLeaderId = newStateAfterPass.lastPlayerIdWhoPlayed ?: "" // Who initiated this trick

            // Determine if all players (who haven't led this trick) have now passed
            val allOthersPassed = newStateAfterPass.players.all { player ->
                player.id == trickLeaderId || newStateAfterPass.playersWhoPassedInCurrentTrick.contains(player.id)
            }

            if (allOthersPassed) {
                // If all active players have passed, the trick ends. The trick initiator gets the lead.
                AppLogger.d(tag, "All players passed on current trick. ${gameState.players.find { it.id == trickLeaderId }?.name} takes the lead.")
                val newGameStateForLead = newStateAfterPass.copy(
                    lastPlayedHand = emptyList(), // Clear the table
                    lastPlayerIdWhoPlayed = null, // Reset last played
                    currentTrick = emptyList(),
                    passCount = 0,
                    playersWhoPassedInCurrentTrick = emptySet(), // Reset for a completely new trick
                    currentPlayerId = trickLeaderId // The trick initiator gets to lead the new trick
                )
                context.updateGameState(newGameStateForLead)
                // Trigger AI turn if the new leader is an AI
                val newLeader = newGameStateForLead.players.find { it.id == trickLeaderId }
                if (newLeader != null && !newLeader.isHuman) {
                    aiMoveHandler?.triggerAiTurnIfApplicable()
                }
            } else {
                // Trick continues, find the next player who has NOT passed in THIS current trick cycle
                val playersInOrder = newStateAfterPass.players
                val currentPlayerIndex = playersInOrder.indexOfFirst { it.id == currentPlayer.id }
                var nextActivePlayerId: String? = null

                // Iterate through players in canonical order starting from next after current passer
                for (i in 1..playersInOrder.size) {
                    val potentialNextIndex = (currentPlayerIndex + i) % playersInOrder.size
                    val potentialNextPlayer = playersInOrder[potentialNextIndex]

                    // If this player hasn't passed in *this current trick*, they are the next active player
                    if (!newStateAfterPass.playersWhoPassedInCurrentTrick.contains(potentialNextPlayer.id)) {
                        nextActivePlayerId = potentialNextPlayer.id
                        break
                    }
                }

                // Fallback: This should theoretically not be null if the trick hasn't ended and not everyone passed.
                if (nextActivePlayerId == null) {
                    AppLogger.e(tag, "Could not find next active player after pass. This indicates a logic error. Falling back to trick leader.")
                    nextActivePlayerId = trickLeaderId.takeIf { it.isNotEmpty() } ?: playersInOrder.firstOrNull()?.id ?: ""
                }

                AppLogger.d(tag, "${currentPlayer.name} passed. Moving to next player: ${newStateAfterPass.players.find { it.id == nextActivePlayerId }?.name}")
                val newGameState = newStateAfterPass.copy(
                    currentPlayerId = nextActivePlayerId,
                    passCount = newStateAfterPass.passCount + 1
                )
                context.updateGameState(newGameState)

                // Trigger AI turn if the determined next active player is an AI
                val nextActivePlayer = newGameState.players.find { it.id == nextActivePlayerId }
                if (nextActivePlayer != null && !nextActivePlayer.isHuman) {
                    aiMoveHandler?.triggerAiTurnIfApplicable()
                }
            }
        }
    }

    override fun resetGame(context: GameStateMachine) {
        context.resetGame()
        AppLogger.d(tag, "Resetting game.")
    }

    override fun toString(): String {
        return phase.name
    }

    fun sortByRank(context: GameStateMachine) {
        val gameState = context.gameState.value
        val currentPlayerId = gameState.currentPlayerId
        val updatedPlayers = gameState.players.map { player ->
            if (player.id == currentPlayerId) {
                player.copy(
                    hand = player.hand.sortedWith(compareBy({ it.rank.value }, { it.suit.ordinal })) as MutableList<PlayingCard>
                ) // Cast to MutableList
            } else {
                player
            }
        }
        context.updateGameState(gameState.copy(players = updatedPlayers))
    }

    fun sortBySuit(context: GameStateMachine) {
        val gameState = context.gameState.value
        val currentPlayerId = gameState.currentPlayerId
        val updatedPlayers = gameState.players.map { player ->
            if (player.id == currentPlayerId) {
                player.copy(
                    hand = player.hand.sortedWith(compareBy({ it.suit.ordinal }, { it.rank.value })) as MutableList<PlayingCard>
                )
            } else {
                player
            }
        }
        context.updateGameState(gameState.copy(players = updatedPlayers))
        AppLogger.d(tag, "Player hand sorted by suit.")
    }
}
