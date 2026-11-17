package com.nowlsantos.big2ai.model.enums

/**
 * Represents the typical positions of players on a game screen,
 * often relative to the local player. Using cardinal directions for clarity
 * and consistency with animation calculations.
 */
enum class PlayerPosition {
    NORTH, EAST, SOUTH, WEST;

    fun next(): PlayerPosition = when(this) {
        NORTH -> EAST
        EAST -> SOUTH
        SOUTH -> WEST
        WEST -> NORTH
    }
}