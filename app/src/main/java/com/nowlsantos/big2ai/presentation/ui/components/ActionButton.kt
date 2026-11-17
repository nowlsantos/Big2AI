package com.nowlsantos.big2ai.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.big2ai.presentation.event.UserActionEvent
import com.nowlsantos.big2ai.presentation.ui.theme.*

@Composable
fun ActionButton(
    onUserActionEvent: (UserActionEvent) -> Unit,
    enabled: Boolean,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                ControlButton(
                    name = "PLAY CARDS",
                    color = SubtleGreen,
                    enabled = enabled,
                    onClick = { onUserActionEvent(UserActionEvent.PlayCardsClicked) },
                )

                ControlButton(
                    name = "PASS TURN",
                    color = BrightRed,
                    enabled = enabled,
                    onClick = { onUserActionEvent(UserActionEvent.PassTurnClicked) },
                )
            }

            Row {
                ControlButton(
                    name = "SORT RANK",
                    color = PaleBlue,
                    enabled = enabled,
                    onClick = { onUserActionEvent(UserActionEvent.SortByRankClicked) },
                )

                ControlButton(
                    name = "SORT SUIT",
                    color = CoolOrange,
                    enabled = enabled,
                    onClick = { onUserActionEvent(UserActionEvent.SortBySuitClicked) },
                )
            }
        }
    }
}

@Composable
private fun ControlButton(
    color: Color,
    name: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    ElevatedButton(
        onClick = onClick,
        shape = RectangleShape,
        enabled = enabled,
        colors = ButtonDefaults.elevatedButtonColors(containerColor = color),
        modifier = Modifier.width(174.dp)
    ) {
        if (enabled) {
            Text(
                text = name,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = PureWhite,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}