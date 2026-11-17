package com.nowlsantos.big2ai.presentation.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nowlsantos.big2ai.model.data.LastPlayedHandState
import com.nowlsantos.big2ai.model.data.PlayerUiState
import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.model.enums.GamePhase
import com.nowlsantos.big2ai.model.enums.PlayerPosition
import com.nowlsantos.big2ai.model.states.GameInput
import com.nowlsantos.big2ai.model.states.GameState
import com.nowlsantos.big2ai.model.states.GameUiState
import com.nowlsantos.big2ai.model.states.UiEffect
import com.nowlsantos.big2ai.state.GameStateMachine
import com.nowlsantos.big2ai.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(val gameStateMachine: GameStateMachine) : ViewModel() {
    private val tag = "GameViewModel" // Define a tag for logging within this class

    // The UI State exposed to Composables.
    private val _uiState = MutableStateFlow(GameUiState.initial())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Observe GameState specifically for phase changes to trigger _displayedLastPlayedHand reset
            gameStateMachine.gameState.map { gameState ->
                AppLogger.d(tag, "GameState updated. Current Phase: ${gameState.gamePhase}")
                createCurrentGameUiState(gameState)
            }.collect { newState ->
                if (newState != _uiState.value) { // Only update if state has changed
                    _uiState.value = newState
                }
            }
        }
    }

    /**
     * Transforms the core GameState into the UI-specific GameUiState.
     * This function now solely relies on the GameState for all its data.
     */
    private fun createCurrentGameUiState(
        gameState: GameState,
    ): GameUiState {
        val localPlayer = gameState.players.firstOrNull { it.isHuman }
        val currentPlayer = gameState.players.firstOrNull { it.id == gameState.currentPlayerId }
        val canStartGame = gameState.players.size == 4 && gameState.gamePhase == GamePhase.SETUP

        // lastPlayedHand now directly reflects GameState's lastPlayedHand.
        // The UI (GameScreen) will manage when to display this based on animation completion.
        val lastPlayedHandStateForUi = if (gameState.lastPlayedHand.isNotEmpty()) {
            LastPlayedHandState(
                cards = gameState.lastPlayedHand,
                playerId = gameState.lastPlayerIdWhoPlayed,
                playerName = gameState.players.find { it.id == gameState.lastPlayerIdWhoPlayed }?.name ?: "Unknown Player",
                combinationType = GameState.sharedHandEvaluator.evaluateHand(gameState.lastPlayedHand)?.type?.name ?: "UNKNOWN"
            )
        } else {
            null
        }

        // Resolve winner names from IDs in GameState for UI display
        val roundWinnerDisplayName = gameState.roundWinnerId?.let { winnerId ->
            gameState.players.find { it.id == winnerId }?.name
        }

        val gameWinnerDisplayName = gameState.gameWinnerId?.let { winnerId ->
            gameState.players.find { it.id == winnerId }?.name
        }

        return GameUiState(
            currentPhase = gameState.gamePhase,
            players = gameState.players.map { player ->
                PlayerUiState(
                    id = player.id,
                    name = player.name,
                    score = player.score,
                    hand = player.hand,
                    numberOfCards = player.hand.size,
                    selectedCards = player.selectedCards,
                    isCurrentPlayer = player.id == gameState.currentPlayerId,
                    hasPassed = player.hasPassed,
                    position = player.position,
                    isHuman = player.isHuman,
                    roundsWon = player.roundsWon,
                    action = player.action
                )
            },
            currentTurnPlayerName = currentPlayer?.name,
            canPlay = gameState.gamePhase == GamePhase.PLAYING && currentPlayer?.name == localPlayer?.name,
            canPass = gameState.gamePhase == GamePhase.PLAYING && currentPlayer?.name == localPlayer?.name,
            canResetGame = gameState.gamePhase == GamePhase.GAME_OVER || gameState.gamePhase == GamePhase.ROUND_END || gameState.gamePhase == GamePhase.SETUP,
            canStartGame = canStartGame,
            roundWinnerName = roundWinnerDisplayName,
            gameWinnerName = gameWinnerDisplayName,
            lastPlayedHand = lastPlayedHandStateForUi,
            roundNumber = gameState.roundNumber,
        )
    }

    fun onEvent(input: GameInput) {
        when (input) {
            is GameInput.AddPlayer -> { /* Handled internally by SetupState if needed */ }
            is GameInput.StartGame -> startGame()
            is GameInput.DealingAnimationFinished -> dealingAnimationFinished()
            is GameInput.PassTurn -> passTurn(input.playerId)
            is GameInput.PlayCards -> playCards(input.playerId, input.cards)
            is GameInput.ReadyForNextRound -> readyForNextRound()
            is GameInput.ResetGame -> resetGame()
            is GameInput.SetPlayerReady -> setPlayerReady(input.playerId, input.isReady)
            is GameInput.CardSelection -> handleCardSelection(input.card)
            is GameInput.SortByRank -> sortByRank()
            is GameInput.SortBySuit -> sortBySuit()
            is GameInput.PlayCardsAnimationFinished -> null
        }
    }

    /**
     * Handles card selection by a human player.
     */
    private fun handleCardSelection(selectedCard: PlayingCard) {
        val currentGameState = gameStateMachine.gameState.value
        val updatedPlayers = currentGameState.players.map { player ->
            if (player.position == PlayerPosition.SOUTH && player.isHuman) {
                val currentSelectedCards = player.selectedCards.toMutableSet()
                if (currentSelectedCards.contains(selectedCard)) {
                    currentSelectedCards.remove(selectedCard)
                } else {
                    currentSelectedCards.add(selectedCard)
                }
                player.copy(selectedCards = currentSelectedCards.toSet())
            } else {
                player
            }
        }
        val newGameState = currentGameState.copy(players = updatedPlayers)
        gameStateMachine.updateGameState(newGameState)
    }

    private fun setPlayerReady(playerId: String, isReady: Boolean) {
        gameStateMachine.handleInput(GameInput.SetPlayerReady(playerId, isReady))
    }

    private fun startGame() {
        // ViewModel now only instructs the GameStateMachine to start the game.
        // The UI will observe UiEffect.TriggerDealingAnimation to start its animation.
        AppLogger.d(tag, "GameViewModel:: startGame: Forwarding StartGame input to GameStateMachine.")
        gameStateMachine.handleInput(GameInput.StartGame)
    }

    fun playCards(playerId: String, cards: List<PlayingCard>) {
        AppLogger.d(tag, "GameViewModel:: playCards: Received from UI. Player ID: $playerId, Cards: ${cards.joinToString()}")
        viewModelScope.launch { // Ensure this is launched in a coroutine
            val player = gameStateMachine.gameState.value.currentPlayer // Ensure currentPlayer is accessible
            val currentPlayerId = player?.id
            if (currentPlayerId != null) {
                gameStateMachine.handleInput(GameInput.PlayCards(playerId, cards))
            } else {
                gameStateMachine.emitUiEffect(UiEffect.ShowError("No current player to play cards."))
            }
        }
    }

    fun playCardAnimationFinished() {
        // ViewModel no longer manages animation states or displayed last hand.
        // It simply forwards the completion event to the GameStateMachine.
        AppLogger.d(tag, "GameViewModel:: playCardAnimationFinished: Received from UI. Forwarding to GameStateMachine.")
        gameStateMachine.handleInput(GameInput.PlayCardsAnimationFinished)
    }

    fun passTurn(playerId: String) {
        AppLogger.d(tag, "GameViewModel:: passTurn: Received from UI. Player ID: $playerId")
        gameStateMachine.handleInput(GameInput.PassTurn(playerId))
    }

    fun sortByRank() {
        AppLogger.d(tag, "GameViewModel:: sortByRank: Received from UI.")
        gameStateMachine.handleInput(GameInput.SortByRank)
    }

    fun sortBySuit() {
        AppLogger.d(tag, "GameViewModel:: sortBySuit: Received from UI.")
        gameStateMachine.handleInput(GameInput.SortBySuit)
    }

    fun dealingAnimationFinished() {
        // ViewModel no longer manages dealing animation states.
        // It simply forwards the completion event to the GameStateMachine.
        AppLogger.d(tag, "dealingAnimationFinished: Received from UI. Forwarding to GameStateMachine.")
        gameStateMachine.handleInput(GameInput.DealingAnimationFinished)
    }

    fun readyForNextRound() {
        AppLogger.d(tag, "readyForNextRound: Received from UI.")
        gameStateMachine.handleInput(GameInput.ReadyForNextRound)
    }

    fun resetGame() {
        AppLogger.d(tag, "resetGame: Received from UI.")
        gameStateMachine.handleInput(GameInput.ResetGame)
    }
}