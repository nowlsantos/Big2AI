package com.nowlsantos.big2ai.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.big2ai.presentation.ui.theme.BrightRed
import com.nowlsantos.big2ai.presentation.ui.theme.PureWhite

@Composable
fun ErrorMessage(
    isVisible: Boolean,
    message: String?,
    modifier: Modifier = Modifier // Allow external modifiers for positioning
) {
    val density = LocalDensity.current
    val slideOffset = with(density) { 100.dp.toPx().toInt() } // Slide from 100dp below its position

    AnimatedVisibility(
        visible = isVisible && message != null,
        enter = slideInVertically(initialOffsetY = { slideOffset }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { slideOffset }) + fadeOut(),
        modifier = modifier // Apply the external modifier here
    ) {
        Card(
            shape = RectangleShape,
            colors = CardDefaults.cardColors(containerColor = BrightRed),
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message ?: "Unknown Error", // Provide a fallback message
                    color = PureWhite,
                    fontSize = 26.sp,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}