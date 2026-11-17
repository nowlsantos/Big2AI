package com.nowlsantos.big2ai.state

import android.util.Log
import com.nowlsantos.big2ai.model.enums.GamePhase
import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.model.states.GameInput // Import GameInput
import com.nowlsantos.big2ai.model.data.Player

interface GamePhaseState {
    /** The GamePhase this state represents. */
    val phase: GamePhase

    /** Called when the state machine enters this state. */
    fun onEnter(context: GameStateMachine)

    /** Called when the state machine exits this state. */
    fun onExit(context: GameStateMachine)

    /** Handles a generic GameInput for this state. Implementations will use 'when' to differentiate. */
    fun handleInput(context: GameStateMachine, input: GameInput) {
        // Default implementation reports an invalid action.
        // Specific states will override this for inputs they support.
        reportInvalidAction("handleInput for ${input::class.simpleName}")
    }

    // --- Specific Action Methods (now delegated from handleInput) ---
    // These methods provide more specific overrides for each type of input,
    // making the handleInput 'when' statement cleaner.

    fun addPlayer(context: GameStateMachine, player: Player) { reportInvalidAction("addPlayer") }
    fun setPlayerReady(context: GameStateMachine, playerId: String, isReady: Boolean) { reportInvalidAction("playerReady") }
    fun startGame(context: GameStateMachine, players: List<Player>) { reportInvalidAction("startGame") }

    fun playCards(context: GameStateMachine, playerId: String, cards: List<PlayingCard>) { reportInvalidAction("playCards") }
    fun passTurn(context: GameStateMachine, playerId: String) { reportInvalidAction("passTurn") }

    fun readyForNextRound(context: GameStateMachine) { reportInvalidAction("readyForNextRound") } // For ROUND_END -> READY_TO_DEAL
    fun resetGame(context: GameStateMachine) { reportInvalidAction("resetGame") } // E.g., from Game Over to Setup

    // --- NEW: Method to handle DealingAnimationFinished input ---
    fun dealingAnimationFinished(context: GameStateMachine) { reportInvalidAction("dealingAnimationFinished") }

    /** Helper to report an action that's not valid in the current state. */
    fun reportInvalidAction(actionName: String) {
        Log.d("GamePhaseState", "Action '$actionName' is not valid in state '${this::class.simpleName}' (Phase: $phase)")
        // For stricter control, consider throwing an IllegalStateException:
        // throw IllegalStateException("Action '$actionName' not valid in state '$phase'")
    }
}