package com.nowlsantos.big2ai.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.nowlsantos.big2ai.R
import com.nowlsantos.big2ai.util.CARD_WIDTH
import com.nowlsantos.big2ai.util.CARD_HEIGHT

@Composable
fun BackFaceView(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.card_back),
        contentDescription = "Card back",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(CARD_WIDTH, CARD_HEIGHT) // Use passed dimensions
    )
}