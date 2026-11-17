package com.nowlsantos.big2ai.presentation.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.big2ai.presentation.ui.theme.DarkGreen
import com.nowlsantos.big2ai.presentation.ui.theme.FlatBlack
import com.nowlsantos.big2ai.presentation.ui.theme.PureWhite

@Composable
fun StartButton(
    onClick: () -> Unit
) {
    ElevatedButton(
        onClick = onClick,
        shape = RectangleShape,
        colors = ButtonDefaults.elevatedButtonColors(containerColor = DarkGreen),
    ) {
        Text(
            text = "Start Game",
            color = PureWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(10.dp)
        )
    }
}