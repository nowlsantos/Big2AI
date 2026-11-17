package com.nowlsantos.big2ai.state

import com.nowlsantos.big2ai.engine.ExpertBig2AI
import com.nowlsantos.big2ai.engine.BigTwoHandEvaluator
import com.nowlsantos.big2ai.engine.CardTracker
import com.nowlsantos.big2ai.engine.OpponentModel
import com.nowlsantos.big2ai.model.enums.ComboType
import com.nowlsantos.big2ai.model.data.Deck
import com.nowlsantos.big2ai.model.enums.GamePhase
import com.nowlsantos.big2ai.model.states.GameState
import com.nowlsantos.big2ai.model.data.Player
import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.model.states.GameInput
import com.nowlsantos.big2ai.model.enums.PlayerPosition
import com.nowlsantos.big2ai.model.enums.Rank
import com.nowlsantos.big2ai.model.enums.Suit
import com.nowlsantos.big2ai.model.states.UiEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.nowlsantos.big2ai.util.AppLogger

class GameStateMachine(
    val scope: CoroutineScope,
    val cardTracker: CardTracker = CardTracker(),
    val handEvaluator: BigTwoHandEvaluator = BigTwoHandEvaluator(),
    val opponentModel: OpponentModel = OpponentModel(cardTracker, handEvaluator),
    val aiPlayers: MutableMap<String, ExpertBig2AI>,
) {
    private val tag = "GameStateMachine" // Define a tag for logging within this class
    // Declare single, persistent instances of each game phase state
    private val setupState: GamePhaseState = SetupState()
    private val readyToDealState: GamePhaseState = ReadyToDealState()
    private val dealState: GamePhaseState = DealState()
    private val playState: GamePhaseState = PlayState()
    private val roundEndState: GamePhaseState = RoundEndState()
    private val gameOverState: GamePhaseState = GameOverState()

    // Publicly exposed GameState for UI rendering and inspection.
    private val _gameState = MutableStateFlow(GameState.initial())
    val gameState = _gameState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<UiEffect>(extraBufferCapacity = 1) // Buffer 1 to ensure immediate collection
    val uiEffect: SharedFlow<UiEffect> = _uiEffect.asSharedFlow()

    // Holds the currently active GamePhaseState instance
    private var currentPhaseState: GamePhaseState

    init {
        // Initialize the state machine by setting the initial phase and entering it
        currentPhaseState = setupState // Set the initial state to the pre-created SetupState instance
        currentPhaseState.onEnter(this) // Manually call onEnter for the initial state
    }

    /**
     * Helper method to emit UI effects.
     */
    fun emitUiEffect(effect: UiEffect) {
        scope.launch {
            _uiEffect.emit(effect)
        }
    }

    /**
     * Handles incoming game inputs by delegating to the current active game phase state.
     * All state transitions and logic are encapsulated within the GamePhaseState implementations.
     */
    fun handleInput(input: GameInput) { // REMOVED: suspend
        val currentPhaseBeforeInput = _gameState.value.gamePhase
        AppLogger.d(tag, "GameStateMachine:: Handling input: $input in phase: $currentPhaseBeforeInput")

        // Call to handleInput is now non-suspend
        currentPhaseState.handleInput(this@GameStateMachine, input)
        AppLogger.d(tag, "GameStateMachine:: Input $input processed. Current phase is now: ${_gameState.value.gamePhase}")
    }

    /**
     * Helper method to update the internal GameState flow.
     * This ensures all updates go through a single channel.
     */
    fun updateGameState(newState: GameState) {
        _gameState.value = newState
        AppLogger.d(tag, "GameStateMachine:: Phase: ${newState.gamePhase}, Current Player: ${newState.currentPlayer?.name}")
    }

    fun transitionTo(phase: GamePhase) { // REMOVED: suspend
        // REMOVED: stateMachineMutex.withLock { ... }
        val oldPhase = currentPhaseState.phase
        AppLogger.d(tag, "GameStateMachine:: Attempting to transition from $oldPhase to $phase")

        if (oldPhase == phase) {
            AppLogger.d(tag, "GameStateMachine:: Already in $phase. No state change. Re-entering current state.")
            currentPhaseState.onEnter(this) // Re-enter to re-run state logic if needed
            return // Simple return from non-suspend function
        }

        // 1. Call onExit for the old state (will be non-suspend now)
        currentPhaseState.onExit(this)

        // 2. Update the internal _gameState flow to reflect the new phase.
        _gameState.update { it.copy(gamePhase = phase) }

        // 3. Update the internal currentPhaseState instance
        currentPhaseState = when (phase) {
            GamePhase.SETUP -> setupState
            GamePhase.READY_TO_DEAL -> readyToDealState
            GamePhase.DEALING -> dealState
            GamePhase.PLAYING -> playState
            GamePhase.ROUND_END -> roundEndState
            GamePhase.GAME_OVER -> gameOverState
        }

        // 4. Call onEnter for the new state (will be non-suspend now)
        AppLogger.d(tag, "GameStateMachine:: Successfully transitioned to ${currentPhaseState.phase}. Calling onEnter.")
        currentPhaseState.onEnter(this)
    }

    /**
     * This function performs the comprehensive reset of game data for a new round:
     * creates a new deck, deals cards, and prepares AI players for their new hands.
     * This is called by ReadyToDealState's onEnter.
     */
    fun setupNewRoundState() {
        val previousGameState = _gameState.value
        //AppLogger.d(tag, "GameStateMachine:: Setting up new round state for Round ${previousGameState.roundNumber + 1}.")

        val deck = Deck.create().apply {
            shuffle()
            shuffle()
        }

        val newPlayersList = previousGameState.players.map { player ->
            player.copy(
                hand = mutableListOf(),
                selectedCards = emptySet(),
                hasPassed = false
            )
        }

        for (i in 0 until 52) {
            if (deck.isEmpty()) break
            val card = deck.deal()
            val playerIndex = i % newPlayersList.size
            newPlayersList[playerIndex].hand.add(card)
        }

        // Ensure AI hands are sorted if that's part of your AI logic prep
        newPlayersList.forEach { player ->
            if (!player.isHuman) {
                player.hand.sortWith(compareBy({ it.rank.value }, { it.suit.ordinal }))
            }
        }

        val threeDiamondsCard = PlayingCard(Rank.THREE, Suit.DIAMONDS)
        var playerWithThreeOfDiamonds: Player? = null

        newPlayersList.forEach { player ->
            if (player.hand.contains(threeDiamondsCard)) {
                playerWithThreeOfDiamonds = player
                AppLogger.d(tag, "GameStateMachine:: Player ${player.name} (${player.id}) has 3♦. Hand: ${player.hand.joinToString()}")
            }
        }

        // --- Strictly determine first player by 3 of Diamonds holder ---
        val firstPlayerIdForNewRound = playerWithThreeOfDiamonds?.id
            ?: newPlayersList.firstOrNull()?.id // Fallback if no 3D found (should not happen in a 52-card deal)
            ?: "" // Further fallback for extreme error cases

        /*AppLogger.d(tag, "GameStateMachine:: Determined first player for new round:" +
                " ${newPlayersList.find { it.id == firstPlayerIdForNewRound }?.name} " +
                "(ID: $firstPlayerIdForNewRound)")
        if (firstPlayerIdForNewRound.isEmpty()) {
            AppLogger.e(tag, "GameStateMachine:: ERROR: firstPlayerIdForNewRound is empty! This should not happen if players are present.")
        } else if (newPlayersList.find { it.id == firstPlayerIdForNewRound }?.hand?.contains(threeDiamondsCard) == false) {
            AppLogger.e(tag, "GameStateMachine:: WARNING: First player " +
                    "(${newPlayersList.find { it.id == firstPlayerIdForNewRound }?.name}) " +
                    "does NOT have 3♦ despite being designated " +
                    "(fallback used). This may indicate an issue in the 3♦ detection or an invalid game state.")
        }*/

        val newGameState = previousGameState.copy(
            deck = deck, // Note: This deck is now "empty" from GameState's perspective after dealing
            players = newPlayersList.toList(),
            currentPlayerId = firstPlayerIdForNewRound,
            roundNumber = previousGameState.roundNumber + 1, // Increment round number here
            lastPlayedHand = emptyList(),
            lastPlayerIdWhoPlayed = null,
            roundWinnerId = null, // Reset round winner for the new round
            isDealingAnimationPlaying = false, // This flag is removed from GameUiState, but GameState might still use it for internal logic, let's keep it here for now if needed by internal logic
            currentTrick = emptyList(),
            passCount = 0,
            trickHistory = emptyList(),
            isFirstMoveOfGame = true,
            playersWhoPassedInCurrentTrick = emptySet(),
            gameWinnerId = null // Reset game winner on new round setup (only if starting a completely new game, handled in resetGame)
        )
        //updateGameState(newGameState)
        updateGameState(newGameState)
        AppLogger.d(tag, "GameStateMachine:: New round GameState set. ${newGameState.roundNumber}")
    }

    // Helper for setting the card dealing animation state
    /*fun setDealingAnimationState(
        isAnimating: Boolean,
        card: PlayingCard? = null,
        targetPosition: PlayerPosition? = null
    ) {
        _gameState.value = _gameState.value.copy(
            isDealingAnimationPlaying = isAnimating,
        )
        AppLogger.d(tag, "GameStateMachine:: setDealingAnimationState:: Animating:: $isAnimating")
    }*/

    /**
     * Resets the game to its initial setup state.
     * This method should be called when starting a new game.
     */
    fun resetGame() {
        AppLogger.d(tag, "GameStateMachine:: Resetting entire game.")
        currentPhaseState.onExit(this) // Call onExit for current state

        _gameState.value = GameState.initial()
        aiPlayers.clear() // <--- CLEAR THE AI INSTANCES MAP ONLY ON FULL GAME RESET

        currentPhaseState = setupState
        currentPhaseState.onEnter(this)
        AppLogger.d(tag, "GameStateMachine:: Game reset to SETUP phase.")
    }

    /**
     * Triggers a UI animation effect for playing cards.
     * This function is called by PlayState.
     */
    fun triggerPlayCardsAnimation(cards: List<PlayingCard>, playerId: String, combinationType: ComboType) {
        scope.launch {
            AppLogger.d(tag, "GameStateMachine::triggerPlayCardsAnimation: Triggering animation for $playerId playing ${combinationType.name}.")
            // Ensure UiEffect.PlayCardsAnimationTrigger is defined to accept these three parameters
            _uiEffect.emit(UiEffect.PlayCardsAnimationTrigger(playerId, cards, combinationType))
        }
    }
}