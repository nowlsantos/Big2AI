package com.nowlsantos.big2ai.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.big2ai.model.data.PlayerUiState
import com.nowlsantos.big2ai.presentation.ui.theme.DarkGreen
import com.nowlsantos.big2ai.presentation.ui.theme.LightGray


@Composable
fun PlayerScore(players: List<PlayerUiState>) {
    val circleDiameter = 60.dp

    players.forEachIndexed { index, player ->
        //Log.d("Big2", "PlayerScore:: Index -> $index | Player -> ${player.name} | Score -> ${player.score}")
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    //.weight(1f)
                    .padding(start = 4.dp)
                    .width(70.dp)
                    .height(420.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                if (index == 0) {
                    // West Player
                    Box(
                        modifier = Modifier
                            .size(circleDiameter)
                            .clip(CircleShape)
                            .background(DarkGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${player.score}",
                            color = LightGray,
                            fontSize = 22.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Column(modifier = Modifier
                //.weight(1.1f)
                .width(450.dp)
                .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ){
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (index == 1) {
                        //North Player
                        Box(
                            modifier = Modifier
                                .size(circleDiameter)
                                .clip(CircleShape)
                                .background(DarkGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${player.score}",
                                color = LightGray,
                                fontSize = 22.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (index == 3) {
                        //South Player
                        Box(
                            modifier = Modifier
                                .size(circleDiameter)
                                .clip(CircleShape)
                                .background(DarkGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${player.score}",
                                color = LightGray,
                                fontSize = 22.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    //.weight(1f)
                    .padding(end = 4.dp)
                    .width(70.dp)
                    .height(400.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                if (index == 2) {
                    //East Player
                    Box(
                        modifier = Modifier
                            .size(circleDiameter)
                            .clip(CircleShape)
                            .background(DarkGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${player.score}",
                            color = LightGray,
                            fontSize = 22.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

        }
    }
}