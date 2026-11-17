package com.nowlsantos.big2ai.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.big2ai.model.enums.PlayerPosition
import com.nowlsantos.big2ai.presentation.ui.theme.BrightRed
import com.nowlsantos.big2ai.presentation.ui.theme.PureWhite

@Composable
fun PlayerPassedTurn(passed: String, playerPosition: PlayerPosition) {
    val width: Float = when (playerPosition) {
        PlayerPosition.WEST, PlayerPosition.EAST -> { 1.0f }
        PlayerPosition.NORTH, PlayerPosition.SOUTH -> { 0.4f }
    }
    Box(modifier = Modifier
        .fillMaxWidth(width)
        .background(BrightRed),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = passed,
            color = PureWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(10.dp)
        )
    }
}