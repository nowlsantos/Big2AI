package com.nowlsantos.big2ai.presentation.ui.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.nowlsantos.big2ai.model.data.LastPlayedHandState
import com.nowlsantos.big2ai.model.states.GameInput
import com.nowlsantos.big2ai.model.enums.GamePhase
import com.nowlsantos.big2ai.presentation.model.GameViewModel
import com.nowlsantos.big2ai.model.data.LocalCoordinate
import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.model.enums.PlayerPosition
import com.nowlsantos.big2ai.model.states.UiEffect
import com.nowlsantos.big2ai.presentation.event.UserActionEvent
import com.nowlsantos.big2ai.presentation.ui.components.BackFaceView
import com.nowlsantos.big2ai.presentation.ui.animation.CardDealingOrchestrator
import com.nowlsantos.big2ai.presentation.ui.animation.PlayingCardOrchestrator
import com.nowlsantos.big2ai.presentation.ui.components.ActionButton
import com.nowlsantos.big2ai.presentation.ui.components.CenterPlayedHand
import com.nowlsantos.big2ai.presentation.ui.components.ErrorMessage
import com.nowlsantos.big2ai.presentation.ui.components.GameOverDialog
import com.nowlsantos.big2ai.presentation.ui.components.PlayerEast
import com.nowlsantos.big2ai.presentation.ui.components.PlayerNorth
import com.nowlsantos.big2ai.presentation.ui.components.RoundEndDialog
import kotlinx.coroutines.delay
import com.nowlsantos.big2ai.presentation.ui.components.PlayerScore
import com.nowlsantos.big2ai.presentation.ui.components.PlayerSouth
import com.nowlsantos.big2ai.presentation.ui.components.PlayerWest
import com.nowlsantos.big2ai.presentation.ui.components.PlayerWins
import com.nowlsantos.big2ai.presentation.ui.components.StartButton
import com.nowlsantos.big2ai.presentation.ui.theme.FlatBlack
import com.nowlsantos.big2ai.util.AppLogger
import kotlinx.coroutines.flow.collectLatest

@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val tag = "GameScreen"
    val uiState by viewModel.uiState.collectAsState()

    // --- Local UI States for Animations and Transient Displays ---
    var localIsDealingAnimationRunning by remember { mutableStateOf(false) }
    var localIsPlayAnimationRunning by remember { mutableStateOf(false) }
    var localAnimatingPlayedCards by remember { mutableStateOf<List<PlayingCard>>(emptyList()) }
    var localAnimatingPlayerId by remember { mutableStateOf<String?>(null) }
    var localDisplayedLastPlayedHand by remember { mutableStateOf<LastPlayedHandState?>(null) }

    var isMessageVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var playerAction by remember { mutableStateOf("") }
    var playerPassed by remember { mutableStateOf("") }

    val southPlayer = uiState.players.find { it.position == PlayerPosition.SOUTH }
    val selectedCards = southPlayer?.selectedCards
    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp.toFloat()
    val screenHeight = config.screenHeightDp.toFloat()
    var localCoordinate by remember { mutableStateOf(LocalCoordinate.initial()) }

    val handStateVisible =
        uiState.currentPhase == GamePhase.PLAYING ||
                uiState.currentPhase == GamePhase.ROUND_END ||
                uiState.currentPhase == GamePhase.GAME_OVER

    var showRoundEndDialog by remember { mutableStateOf(false) }
    var showGameOverDialog by remember { mutableStateOf(false) }
    var isHumanTurn by remember { mutableStateOf(false) }
    var startingPlayerName by remember { mutableStateOf<String?>(null) }
    // NEW: Local state to immediately disable buttons after a human action is initiated
    var localHumanActionInProgress by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.gameStateMachine.uiEffect.collectLatest { effect ->
            //AppLogger.d(tag, "GameScreen:: UiEffect received: $effect")
            when (effect) {
                is UiEffect.ShowAction -> {
                    playerAction = effect.message
                }
                is UiEffect.ShowError -> {
                    errorMessage = effect.message
                    isMessageVisible = true
                    if (localHumanActionInProgress) {
                        //AppLogger.d(tag, "GameScreen:: Error received, re-enabling action buttons.")
                        localHumanActionInProgress = false
                    }
                }
                is UiEffect.PlayerPassed -> {
                    playerPassed = effect.message
                }
                is UiEffect.ShowWinner -> {
                    showRoundEndDialog = true
                }
                is UiEffect.ShowGameOver -> {
                    showGameOverDialog = true
                }
                is UiEffect.PlayCardsAnimationTrigger -> {
                    localIsPlayAnimationRunning = true
                    localAnimatingPlayedCards = effect.cards
                    localAnimatingPlayerId = effect.playerId
                }
                is UiEffect.TriggerDealingAnimation -> {
                    localIsDealingAnimationRunning = true
                }
                UiEffect.ClearMessage -> {
                    errorMessage = ""
                    isMessageVisible = false
                    playerAction = ""
                    playerPassed = ""
                    //AppLogger.d(tag, "GameScreen:: UiEffect.ClearMessage received, clearing generic message.")
                }
            }
        }
    }

    LaunchedEffect(isMessageVisible, errorMessage) {
        if (isMessageVisible && errorMessage.isNotBlank()) {
            delay(3000L) // General error message display duration
            isMessageVisible = false
            errorMessage = ""
        }
    }

    LaunchedEffect(uiState.canPlay, uiState.canPass, uiState.roundWinnerName) {
        isHumanTurn = uiState.canPlay && uiState.canPass
        startingPlayerName = uiState.roundWinnerName ?: "WestPlayer"
    }

    LaunchedEffect(isHumanTurn) {
        if (!isHumanTurn && localHumanActionInProgress) {
            //AppLogger.d(tag, "GameScreen:: isHumanTurn became false. Resetting localHumanActionInProgress to false.")
            localHumanActionInProgress = false
            localIsDealingAnimationRunning = false
        }
    }

    // Effect to clear localDisplayedLastPlayedHand on game phase changes (new game, new round)
    LaunchedEffect(uiState.currentPhase) {
        if (uiState.currentPhase == GamePhase.SETUP || uiState.currentPhase == GamePhase.READY_TO_DEAL || uiState.currentPhase == GamePhase.DEALING) {
            //AppLogger.d(tag, "GameScreen:: Clearing localDisplayedLastPlayedHand due to phase change: ${uiState.currentPhase}")
            localDisplayedLastPlayedHand = null // Clear on new game/round setup or dealing phase
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .border(3.dp, FlatBlack)
            .onGloballyPositioned { coordinates ->
                localCoordinate = localCoordinate.copy(
                    containerSize = coordinates.size,
                    centerOffset = Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        PlayerWins(players = uiState.players)
        PlayerScore(players = uiState.players)

        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onGloballyPositioned { coordinates ->
                        localCoordinate = localCoordinate.copy(
                            westOffset = coordinates.positionInParent(),
                            verticalSize = IntSize(coordinates.size.width, coordinates.size.height)
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (handStateVisible) {
                    PlayerWest(
                        uiState = uiState,
                        playerAction = playerAction,
                        playerPassed = playerPassed,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.7f)
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            localCoordinate = localCoordinate.copy(
                                northOffset = coordinates.positionInParent(),
                                horizontalSize = IntSize(coordinates.size.width, coordinates.size.height)
                            )
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (handStateVisible) {
                        PlayerNorth(
                            uiState = uiState,
                            playerAction = playerAction,
                            playerPassed = playerPassed,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.currentPhase == GamePhase.SETUP) {
                        BackFaceView()
                    }

                    if (uiState.currentPhase == GamePhase.SETUP || uiState.currentPhase == GamePhase.READY_TO_DEAL) {
                        Box(modifier = Modifier.fillMaxHeight(),
                            contentAlignment = Alignment.BottomCenter) {
                            StartButton(
                                onClick = { viewModel.onEvent(GameInput.StartGame) }
                            )
                        }
                    }

                    if (handStateVisible && localDisplayedLastPlayedHand != null) {
                        localDisplayedLastPlayedHand?.let {
                            CenterPlayedHand(
                                uiState.roundNumber,
                                lastPlayedHandState = it
                            )
                        }
                    }

                    // Human player controls
                    if (isHumanTurn) {
                        southPlayer?.let { player ->
                            ActionButton(
                                onUserActionEvent = { event ->
                                    when (event) {
                                        UserActionEvent.PlayCardsClicked -> {
                                            localHumanActionInProgress = true
                                            viewModel.onEvent(GameInput.PlayCards(player.id, player.selectedCards.toList()))
                                        }
                                        UserActionEvent.PassTurnClicked -> {
                                            localHumanActionInProgress = true
                                            viewModel.onEvent(GameInput.PassTurn(player.id))
                                        }
                                        UserActionEvent.SortByRankClicked -> {
                                            viewModel.onEvent(GameInput.SortByRank)
                                        }
                                        UserActionEvent.SortBySuitClicked -> {
                                            viewModel.onEvent(GameInput.SortBySuit)
                                        }
                                    }
                                },
                                // Buttons are enabled only if it's the human's turn AND no action is currently in progress
                                enabled = !localHumanActionInProgress
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(0.7f)
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            localCoordinate = localCoordinate.copy(
                                southOffset = coordinates.positionInParent()
                            )
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (handStateVisible) {
                        southPlayer?.let {
                            PlayerSouth(
                                uiState = uiState,
                                player = it,
                                selectedCards = selectedCards?.toList() ?: emptyList(),
                                playerAction = playerAction,
                                playerPassed = playerPassed,
                                onCardClick = { card -> viewModel.onEvent(GameInput.CardSelection(card)) }
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onGloballyPositioned { coordinates ->
                        localCoordinate = localCoordinate.copy(
                            eastOffset = coordinates.positionInParent()
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (handStateVisible) {
                    PlayerEast(
                        uiState = uiState,
                        playerAction = playerAction,
                        playerPassed = playerPassed,
                    )
                }
            }
        }

        if (localIsDealingAnimationRunning) {
            //AppLogger.d(tag, "Displaying CardDealingOrchestrator based on local state.")
            CardDealingOrchestrator(
                players = uiState.players,
                //startingPlayerName = startingPlayerName.toString(),
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                localCoordinate = localCoordinate,
                // Add roundNumber as a key to force recomposition and restart internal effects
                key = uiState.roundNumber, // Assuming uiState has a roundNumber property
                onAnimationFinished = {
                    AppLogger.d(tag, "Dealing animation finished by Orchestrator. Resetting local state and informing ViewModel.")
                    localIsDealingAnimationRunning = false
                    viewModel.dealingAnimationFinished()
                },
            )
        }

        if (localIsPlayAnimationRunning && localAnimatingPlayedCards.isNotEmpty() && localAnimatingPlayerId != null) {
            //AppLogger.d(tag, "Displaying PlayingCardOrchestrator based on local state.")
            val animatingPlayerPosition = uiState.players.find { it.id == localAnimatingPlayerId }?.position

            if (animatingPlayerPosition != null) {
                PlayingCardOrchestrator(
                    animatingCards = localAnimatingPlayedCards,
                    layoutCoordinate = localCoordinate,
                    animatingPlayerPosition = animatingPlayerPosition,
                    onAnimationFinished = {
                        //AppLogger.d(tag, "Play card animation reported finished by Orchestrator. " +
                        //        "Resetting local states and informing ViewModel.")
                        localIsPlayAnimationRunning = false // Reset local animation flag
                        localAnimatingPlayedCards = emptyList() // Clear animating cards
                        localAnimatingPlayerId = null // Clear animating player ID
                        // Update localDisplayedLastPlayedHand to the NEW uiState.lastPlayedHand *ONLY NOW*
                        localDisplayedLastPlayedHand = uiState.lastPlayedHand
                        /*AppLogger.d(tag, "Animation finished. " +
                                "Updating localDisplayedLastPlayedHand to new uiState.lastPlayedHand: " +
                                "${uiState.lastPlayedHand?.cards?.size} cards")*/
                        viewModel.playCardAnimationFinished() // Inform ViewModel
                    }
                )
            }
        }

        ErrorMessage(
            isVisible = isMessageVisible,
            message = errorMessage,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (uiState.currentPhase == GamePhase.ROUND_END) {
            RoundEndDialog(
                uiState = uiState,
                onNextRoundClicked = {
                    viewModel.onEvent(GameInput.ReadyForNextRound)
                    showRoundEndDialog = false
                },
                modifier = Modifier
            )
        }

        if (uiState.currentPhase == GamePhase.GAME_OVER) {
            GameOverDialog(
                uiState = uiState,
                onStartNewGame = {
                    viewModel.onEvent(GameInput.ResetGame)
                    showGameOverDialog = false
                    localDisplayedLastPlayedHand = null
                },
                modifier = Modifier
            )
        }
    }
}
