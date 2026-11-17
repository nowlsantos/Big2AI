package com.nowlsantos.big2ai.model.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

/**
 * A data class to hold the coordinates of various player positions for animation purposes.
 * This is derived from the root layout's global position.
 */
data class LocalCoordinate(
    val horizontalSize: IntSize = IntSize.Companion.Zero, // Size of North/South player hand containers
    val verticalSize: IntSize = IntSize.Companion.Zero, // Size of East/West player hand containers
    val westOffset: Offset = Offset.Companion.Zero,
    val eastOffset: Offset = Offset.Companion.Zero,
    val northOffset: Offset = Offset.Companion.Zero,
    val southOffset: Offset = Offset.Companion.Zero,
    val centerOffset: Offset = Offset.Companion.Zero, // Center of the playing area (needed for Played Card animation)
    val containerSize: IntSize = IntSize.Companion.Zero // Size of the root container (needed for Orchestrators)
) {
    companion object {
        fun initial(): LocalCoordinate = LocalCoordinate(
            horizontalSize = IntSize(0, 0),
            verticalSize = IntSize(0, 0),
            westOffset = Offset(0f, 0f),
            eastOffset = Offset(0f, 0f),
            northOffset = Offset(0f, 0f),
            southOffset = Offset(0f, 0f),
            centerOffset = Offset(0f, 0f), // Initialize all properties
            containerSize = IntSize(0, 0)  // Initialize all properties
        )
    }
}