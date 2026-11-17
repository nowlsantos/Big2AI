package com.nowlsantos.big2ai.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.big2ai.model.data.LastPlayedHandState
import com.nowlsantos.big2ai.presentation.ui.theme.LightGray

@Composable
fun CenterPlayedHand(
    roundNumber: Int,
    lastPlayedHandState: LastPlayedHandState?
) {
    var lastPlayerToPlay = ""

    Box(modifier = Modifier
        .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Display Game round number
        Text(
            text = "Game Round $roundNumber",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = LightGray,
            modifier = Modifier
                .padding(top = 100.dp)
                .align(Alignment.TopCenter)
        )

        // Center played hand
        lastPlayedHandState?.let { playedHand ->
            lastPlayerToPlay = playedHand.playerName!!

            Row(modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                playedHand.cards.forEach { card ->
                    CardView(
                        card = card,
                        isSelected = false,
                        faceUp = true,
                        onClick = null,
                        modifier = Modifier.padding(horizontal = 3.dp)
                    )
                }
            }
        }

        // Display who played the hand
        Text(
            text = "$lastPlayerToPlay played",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = LightGray,
            modifier = Modifier
                .padding(bottom = 100.dp)
                .align(Alignment.BottomCenter)
        )
    }
}