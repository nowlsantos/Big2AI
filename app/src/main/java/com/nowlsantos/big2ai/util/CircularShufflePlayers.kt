package com.nowlsantos.big2ai.util

import com.nowlsantos.big2ai.model.data.PlayerUiState // Changed import from Player to PlayerUiState
import com.nowlsantos.big2ai.model.enums.PlayerPosition

/**
 * Shuffles a list of players by circularly rotating them, ensuring a specific player starts
 * and the relative order of others is maintained.
 *
 * This version is updated to work directly with `PlayerUiState` objects.
 *
 * @param allPlayers The complete list of players (as PlayerUiState objects).
 * @param startingPlayerId The ID of the player who should start the new order.
 * @param canonicalOrder The fixed, circular order of players by their PlayerPosition.
 * For Big Two, this might be a standard table order like SOUTH -> WEST -> NORTH -> EAST.
 * It's crucial that all players in `allPlayers` have a position present in this order.
 * @return A new list of players, starting with `startingPlayerName` and followed by others
 * in the `canonicalOrder`. If `startingPlayerName` is not found, returns `allPlayers` unchanged.
 */
fun CircularShufflePlayers(
    allPlayers: List<PlayerUiState>, // Changed type to PlayerUiState
    startingPlayerName: String,
    canonicalOrder: List<PlayerPosition>
): List<PlayerUiState> { // Changed return type to PlayerUiState
    // Basic validation
    if (allPlayers.isEmpty() || allPlayers.size != canonicalOrder.size) {
        // For Big 2, you always expect 4 players.
        // Log an error or warning if player count doesn't match canonical order size
        return allPlayers
    }

    val startingPlayer = allPlayers.find { it.name == startingPlayerName }
    val startingPosition = startingPlayer?.position

    if (startingPosition == null) {
        // Starting player not found, return original list
        return allPlayers
    }

    // Find the index of the starting player's position in the canonical order
    val startIndex = canonicalOrder.indexOf(startingPosition)
    if (startIndex == -1) {
        // Starting position not found in canonical order, return original list
        return allPlayers
    }

    // Create the new ordered list of positions
    val reorderedPositions = mutableListOf<PlayerPosition>()
    for (i in 0 until canonicalOrder.size) {
        reorderedPositions.add(canonicalOrder[(startIndex + i) % canonicalOrder.size])
    }

    // Create a map from PlayerPosition to PlayerUiState for efficient lookup
    val playerMapByPosition = allPlayers.associateBy { it.position }

    // Map these reordered positions to actual PlayerUiState objects using the map
    val reorderedPlayers = reorderedPositions.mapNotNull { position ->
        playerMapByPosition[position]
    }

    // Final validation: ensure all original players are present in the reordered list
    if (reorderedPlayers.size != allPlayers.size) {
        // This case indicates a mismatch (e.g., duplicate positions or missing players)
        // For robustness, you might log an error here.
        return allPlayers // Fallback to original
    }

    return reorderedPlayers
}