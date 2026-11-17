package com.nowlsantos.big2ai.model.data

import com.nowlsantos.big2ai.model.data.PlayingCard

/**
 * Represents the last played hand on the table.
 */
data class LastPlayedHandState(
    val cards: List<PlayingCard>,
    val playerId: String?,
    val playerName: String?, // Name of the player who played these cards
    val combinationType: String? = null
    // Add other UI-specific properties if needed, e.g., validation errors
)