package com.nowlsantos.big2ai.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import com.nowlsantos.big2ai.presentation.ui.theme.DarkGreen
import com.nowlsantos.big2ai.presentation.ui.theme.FlatBlack
import com.nowlsantos.big2ai.presentation.ui.theme.LightGray
import com.nowlsantos.big2ai.presentation.ui.theme.MatteGray
import com.nowlsantos.big2ai.presentation.ui.theme.PureWhite
import com.nowlsantos.big2ai.presentation.ui.theme.SubtleGreen

@Composable
fun RoundEndDialog(
    uiState: GameUiState,
    onNextRoundClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = {},
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Round ${uiState.roundNumber} Result",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlatBlack,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${uiState.roundWinnerName} won the round!",
                    fontSize = 26.sp,
                    color = SubtleGreen
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))

                // Table Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PLAYER",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "SCORE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(0.7f),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "ROUNDS WON",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }
                Spacer(Modifier.height(8.dp))

                // Player Rows
                uiState.players.forEach { player ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = player.name,
                            fontSize = 18.sp,
                            color = if (uiState.roundWinnerName == player.name) SubtleGreen else FlatBlack,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = player.score.toString(),
                            fontSize = 18.sp,
                            color = if (uiState.roundWinnerName == player.name) SubtleGreen else FlatBlack,
                            modifier = Modifier.weight(0.7f),
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = player.roundsWon.toString(),
                            fontSize = 18.sp,
                            color = if (uiState.roundWinnerName == player.name) SubtleGreen else FlatBlack,
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
                onClick = onNextRoundClicked,
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SubtleGreen,
                    disabledContainerColor = LightGray
                ),
                modifier = modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    "Next Round",
                    fontSize = 22.sp,
                    color = PureWhite
                )
            }
        },
        dismissButton = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        ),
    )
}