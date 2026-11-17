package com.nowlsantos.big2ai.state

import android.util.Log
import com.nowlsantos.big2ai.engine.ExpertBig2AI
import com.nowlsantos.big2ai.model.enums.GamePhase
import com.nowlsantos.big2ai.model.data.Player
import com.nowlsantos.big2ai.model.states.GameInput
import com.nowlsantos.big2ai.model.enums.PlayerPosition
import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.util.AppLogger

class SetupState : GamePhaseState {
    private val tag = "SetupState" // Define a tag for logging within this class
    override val phase: GamePhase = GamePhase.SETUP

    override fun onEnter(context: GameStateMachine) {
        AppLogger.d(tag, "SetupState:: Entering SETUP phase.")
        /*context.updateGameState(context.gameState.value.copy( // Access value of StateFlow
            gamePhase = GamePhase.SETUP,
            players = emptyList(), // Clear existing players if any from a previous game
            currentPlayerId = "",
            lastPlayedHand = emptyList(),
            lastPlayerIdWhoPlayed = null,
            roundWinnerId = null,
            gameWinnerId = null,
            roundNumber = 0 // Should be 0 for a fresh game start
        ))*/

        // --- NEW IMPLEMENTATION: Automatically add players for initial setup ---
        // This simulates adding players when there's no UI/ViewModel yet.
        addDefaultPlayers(context)
    }

    override fun onExit(context: GameStateMachine) {
        AppLogger.d(tag, "SetupState:: Exiting SETUP phase.")
    }

    override fun handleInput(context: GameStateMachine, input: GameInput) {
        when (input) {
            is GameInput.AddPlayer -> addPlayer(context, input.player)
            is GameInput.SetPlayerReady -> setPlayerReady(context, input.playerId, input.isReady)
            is GameInput.StartGame -> startGame(context)
            is GameInput.ResetGame -> resetGame(context) // Allow resetting from setup
            else -> super.handleInput(context, input) // Report invalid action for others
        }
    }

    fun addDefaultPlayers(context: GameStateMachine) {
        val players = listOf(
            Player("p_west", "WestPlayer", PlayerPosition.WEST, mutableListOf<PlayingCard>(), emptySet(), false),
            Player("p_north", "NorthPlayer", PlayerPosition.NORTH, mutableListOf<PlayingCard>(), emptySet(), false),
            Player("p_east", "EastPlayer", PlayerPosition.EAST, mutableListOf<PlayingCard>(), emptySet(), false),
            Player("p_south", "SouthPlayer", PlayerPosition.SOUTH, mutableListOf<PlayingCard>(), emptySet(), true),
        )

        // Update the game state with players
        context.updateGameState(
            context.gameState.value.copy(players = players)
        )

        // --- NEW ADDITION: Populate the aiPlayers map in GameStateMachine ---
        players.forEach { player ->
            if (!player.isHuman) {
                // Instantiate ExpertBig2AI for each AI player
                val aiInstance = ExpertBig2AI(
                    playerId = player.id,
                    cardTracker = context.cardTracker, // Access from GameStateMachine
                    handEvaluator = context.handEvaluator, // Access from GameStateMachine
                    opponentModel = context.opponentModel // Access from GameStateMachine
                )
                // Add the AI instance to the GameStateMachine's aiPlayers map
                context.aiPlayers[player.id] = aiInstance
                AppLogger.d(tag, "SetupState:: Registered AI for ${player.name} with ID ${player.id}")
            }
        }
        AppLogger.d(tag, "SetupState:: Default players added and AI instances registered to GameStateMachine.")
    }

    /**
     * Handles the StartGame input. This method now verifies player count and readiness
     * before transitioning the game phase.
     */
    fun startGame(context: GameStateMachine) {
        val currentPlayers = context.gameState.value.players // Access value
        if (currentPlayers.size < 2) { // Assuming minimum 2 players for Big 2
            AppLogger.d(tag, "SetupState:: Cannot start game: Need at least 2 players. Current players: ${currentPlayers.size}")
            return
        }

        AppLogger.d(tag, "SetupState:: Starting game from SETUP. Transitioning to READY_TO_DEAL phase.")
        context.transitionTo(GamePhase.READY_TO_DEAL)
    }

    override fun resetGame(context: GameStateMachine) {
        AppLogger.d(tag, "SetupState:: Resetting game to initial state.")
        context.resetGame()
    }
}