package com.nowlsantos.big2ai.model.data

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel responsible for managing the UI state of the human player's hand,
 * particularly for card selection before a play is made.
 *
 * This ViewModel does NOT manage the actual game state or player's current hand from the game.
 * It's solely for local UI interaction state.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor() : ViewModel() {
    private val _playerUiState = MutableStateFlow(PlayerUiState.initial())
    val playerUiState = _playerUiState.asStateFlow()
}