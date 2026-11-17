package com.nowlsantos.big2ai.engine

import com.nowlsantos.big2ai.model.states.GameState
import com.nowlsantos.big2ai.model.data.Move
import com.nowlsantos.big2ai.model.data.PassMove
import com.nowlsantos.big2ai.model.data.PlayMove
import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.model.enums.Rank
import com.nowlsantos.big2ai.model.enums.Suit
import kotlin.math.ln
import kotlin.math.sqrt

class MonteCarloTreeSearch(
    private val simState: GameState,
    private val validMoves: List<Move>,
    private val cardTracker: CardTracker,
    private val simulations: Int,
    private val handEvaluator: BigTwoHandEvaluator
) {
    // The aiPlayerId will be passed from ExpertBig2AI, representing the player for whom the MCTS is running.
    // This ID is needed for scoring in the rollout phase.
    // We pass it down to the root node and subsequently to all child nodes and the rollout function.
    private val root = MCTSNode(simState, aiPlayerId = simState.currentPlayerId)

    fun runSimulationsAndGetScore(aiPlayerId: String): Double {
        repeat(simulations) {
            var node = root

            // Selection: Traverse the tree to find the best node to expand/simulate from.
            // Continue while all moves from the current node are expanded and the game is not over.
            while (node.allMovesExpanded && node.state.gameWinnerId == null) {
                // Select the child with the highest UCB1 score.
                node.children.values.maxByOrNull { it.ucb1Score() }?.also { node = it }
                    ?: break // Break if no children (e.g., node has no valid moves to expand)
            }

            // Expansion: If the game is not over and the current node has unexpanded moves,
            // pick one to expand and add as a new child node.
            if (node.state.gameWinnerId == null && !node.allMovesExpanded) {
                node.expand(handEvaluator)?.let { node = it }
            }

            // Simulation (Rollout): Play out a random game from the selected/expanded node's state
            // until a terminal state (game over) or a maximum number of moves is reached.
            // The score is determined based on the outcome from the perspective of the AI player.
            val score = node.rollout(cardTracker, handEvaluator, aiPlayerId)

            // Backpropagation: Update the visit counts and win scores of the current node
            // and all its ancestors up to the root, based on the simulation's outcome.
            node.backpropagate(score)
        }
        // This return value might be adjusted based on how you want to select the best move
        // after all simulations are done (e.g., return the most visited child of the root).
        //return root.visits.toDouble()
        return root.wins / root.visits // Fix: Return win rate
    }

    fun runSimulationsAndGetBestMove(state: com.nowlsantos.big2ai.model.states.GameState, moves: kotlin.collections.List<com.nowlsantos.big2ai.model.data.Move>, string: kotlin.String) {}
}

class MCTSNode(
    val state: GameState,
    val parent: MCTSNode? = null,
    val move: Move? = null,
    val aiPlayerId: String // Add this to carry the AI player's ID through the tree
) {
    val children = mutableMapOf<Move, MCTSNode>()
    var visits = 0
    var wins = 0.0 // Can represent cumulative score (e.g., 1.0 for win, 0.0 for loss)

    // Checks if all possible moves from this node's state have been expanded into child nodes.
    val allMovesExpanded: Boolean
        get() = children.size == state.generateValidMoves().size

    // Calculates the UCB1 (Upper Confidence Bound 1) score for this node.
    // UCB1 balances exploitation (choosing nodes with high win rates) and exploration (choosing less visited nodes).
    fun ucb1Score(): Double {
        if (visits == 0) return Double.MAX_VALUE // Prioritize unvisited nodes for exploration.
        // Formula: (wins / visits) + C * sqrt(ln(parent_visits) / visits)
        // C (exploration parameter) is typically sqrt(2), here approximated as 1.41.
        return (wins / visits) + 1.41 * sqrt(ln(parent?.visits?.toDouble() ?: 1.0) / visits)
    }

    // Expands the current node by creating a new child node for an unexpanded move.
    fun expand(handEvaluator: BigTwoHandEvaluator): MCTSNode? {
        // Find moves that have not yet been added as children.
        val unexpandedMoves = state.generateValidMoves().filter { it !in children.keys }
        return unexpandedMoves.randomOrNull()?.let { move ->
            // Apply the move to the current state to get the new state for the child node.
            val newState = state.playMove(move)
            // Create a new MCTSNode with the new state, linking it to this node as its parent.
            // Pass the aiPlayerId down to the new node.
            val newNode = MCTSNode(newState, this, move, this.aiPlayerId)
            children[move] = newNode // Add the new node to this node's children map.
            newNode
        }
    }

    // Simulates a random game from the current node's state until game over or max moves.
    private val MAX_ROLLOUT_MOVES = 100 // A safeguard to prevent excessively long simulations.

    // aiPlayerIdForScoring is the ID of the AI player whose perspective we are scoring the game from.
    fun rollout(cardTracker: CardTracker, handEvaluator: BigTwoHandEvaluator, aiPlayerIdForScoring: String): Double {
        var tempState = state.clone() // Create a mutable copy of the current state for simulation.
        var currentRolloutPlayerId = tempState.currentPlayerId
        var movesCount = 0

        // Continue simulation as long as the game is not over and max moves are not exceeded.
        while (tempState.gameWinnerId == null && movesCount < MAX_ROLLOUT_MOVES) {
            val validMoves = tempState.generateValidMoves()
            val chosenMove = if (validMoves.isNotEmpty()) {
                // Choose a random valid move for the rollout. This can be made more sophisticated.
                chooseRolloutMove(tempState, validMoves, handEvaluator)
            } else {
                // If no valid moves are found, assume a pass for simplicity in rollout.
                PassMove(currentRolloutPlayerId)
            }
            tempState = tempState.playMove(chosenMove) // Apply the chosen move.
            currentRolloutPlayerId = tempState.currentPlayerId // Move to the next player.
            movesCount++
        }

        // Determine the score based on the outcome of the simulation.
        return if (tempState.gameWinnerId != null) {
            if (tempState.gameWinnerId == aiPlayerIdForScoring) 1.0 else 0.0
        } else {
            val aiPlayerHandSize = tempState.players.find { it.id == aiPlayerIdForScoring }?.hand?.size ?: 13
            val opponentHandSizes = tempState.players.filter { it.id != aiPlayerIdForScoring }.map { it.hand.size }
            val avgOpponentHandSize = if (opponentHandSizes.isNotEmpty()) opponentHandSizes.average() else 13.0

            // Normalize score to [0,1] range
            val rawScore = (avgOpponentHandSize - aiPlayerHandSize) / 13.0
            (rawScore + 1.0) / 2.0  // Map [-1,1] → [0,1]
        }
    }

    // Backpropagate: Update the visit counts and win scores of this node and all its ancestors.
    fun backpropagate(score: Double) {
        var node: MCTSNode? = this // Start from the current node.
        while (node != null) {
            node.visits++ // Increment visit count.
            node.wins += score // Add the simulation score to cumulative wins.
            node = node.parent // Move up to the parent.
        }
    }

    // Helper function to choose a move during the rollout phase.
    // This is a simplified strategy for quick simulation.
    private fun chooseRolloutMove(
        state: GameState,
        moves: List<Move>,
        handEvaluator: BigTwoHandEvaluator
    ): Move {
        // Get current player's hand size once
        val currentPlayer = state.players.find { it.id == state.currentPlayerId }
        val currentPlayerHandSize = currentPlayer?.hand?.size ?: 0

        // Optimized win check: Play move that uses all cards
        moves.filterIsInstance<PlayMove>()
            .firstOrNull {
                it.cards.size == currentPlayerHandSize }
            ?.let { return it }

        // Get the last played combo to determine valid beating moves.
        val lastCombo = state.lastPlayedHand.takeIf { it.isNotEmpty() }?.let {
            handEvaluator.evaluateHand(it)
        }

        // Special handling for the first move of the game (must play 3♦).
        if (state.isFirstMoveOfGame) {
            val threeDiamonds = PlayingCard(Rank.THREE, Suit.DIAMONDS)
            moves.filterIsInstance<PlayMove>().firstOrNull { it.cards.contains(threeDiamonds) }?.let { return it }
        }

        // If no trick is active (i.e., we are leading a new trick).
        if (lastCombo == null) {
            // Play the smallest possible single card or a combination if possible.
            return moves.filterIsInstance<PlayMove>()
                .minByOrNull { it.leadCard()?.strength ?: Int.MAX_VALUE } // Play lowest card possible
                ?: moves.firstOrNull() // Fallback to any move if no simple play moves found
                ?: PassMove(state.currentPlayerId) // Defensive: if no moves at all, assume pass
        }

        // If a trick is active, find moves that can beat the last played combination.
        val beatingMoves = moves.filterIsInstance<PlayMove>()
            .filter {
                val playedCombo = handEvaluator.evaluateHand(it.cards)
                playedCombo != null && handEvaluator.beats(playedCombo, lastCombo)
            }

        // Prioritize playing a beating move, otherwise pass.
        return when {
            beatingMoves.isNotEmpty() ->
                beatingMoves.minByOrNull { it.leadCard()!!.strength }!! // Play the smallest card that beats
            moves.any { it is PassMove } ->
                moves.first { it is PassMove } // If cannot beat, pass
            else ->
                moves.firstOrNull() // Fallback if no beating moves and no pass option (shouldn't happen in valid game state)
                    ?: PassMove(state.currentPlayerId) // Defensive: if nothing else, pass
        }
    }
}