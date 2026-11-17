package com.nowlsantos.big2ai.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.nowlsantos.big2ai.model.states.GameUiState
import com.nowlsantos.big2ai.presentation.ui.theme.BrightRed
import com.nowlsantos.big2ai.presentation.ui.theme.FlatBlack
import com.nowlsantos.big2ai.presentation.ui.theme.MatteGray
import com.nowlsantos.big2ai.presentation.ui.theme.PureWhite
import com.nowlsantos.big2ai.presentation.ui.theme.SubtleGreen

@Composable
fun GameOverDialog(
    uiState: GameUiState,
    onStartNewGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    var winner = ""
    val fontSize = 20.sp

    AlertDialog(
        modifier = modifier,
        onDismissRequest = {}, // No direct dismissal, user must click 'Start New Game'
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Game Over!",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrightRed
                )
                Spacer(modifier.height(8.dp))

                // Display the overall game winner clearly
                uiState.gameWinnerName?.let { gameWinner ->
                    winner = gameWinner
                    Text(
                        text = "$gameWinner with the highest number points wins the game!",
                        fontSize = 24.sp,
                        //fontWeight = FontWeight.SemiBold,
                        color = MatteGray,
                        textAlign = TextAlign.Center
                    )
                } ?: run {
                    // Fallback if somehow no winner is identified (e.g., all players reached threshold)
                    Text(
                        text = "It's a draw!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FlatBlack
                    )
                }

                Spacer(modifier.height(20.dp))

                Text(
                    text = "Final Scores:",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrightRed,
                )

                Spacer(modifier.height(10.dp))

                // Table Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Player",
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Score",
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize,
                        modifier = Modifier.weight(0.7f),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "Rounds Won",
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }
                Spacer(Modifier.height(8.dp))

                // Player Rows
                uiState.players.forEach { player ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = player.name,
                            fontSize = fontSize,
                            fontWeight = if (winner == player.name) FontWeight.Bold else FontWeight.Normal,
                            color = if (winner == player.name) SubtleGreen else FlatBlack,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = player.score.toString(),
                            fontSize = fontSize,
                            fontWeight = if (winner == player.name) FontWeight.Bold else FontWeight.Normal,
                            color = if (winner == player.name) SubtleGreen else FlatBlack,
                            modifier = Modifier.weight(0.7f),
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = player.roundsWon.toString(),
                            fontSize = fontSize,
                            fontWeight = if (winner == player.name) FontWeight.Bold else FontWeight.Normal,
                            color = if (winner == player.name) SubtleGreen else FlatBlack,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        },

        confirmButton = {
            Button(
                onClick = onStartNewGame,
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SubtleGreen,
                    disabledContainerColor = MatteGray
                ),
                modifier = modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    "Start New Game",
                    fontSize = 22.sp,
                    color = PureWhite
                )
            }
        },
        dismissButton = {}, // Keep empty if no dismiss button is desired
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        ),
    )
}