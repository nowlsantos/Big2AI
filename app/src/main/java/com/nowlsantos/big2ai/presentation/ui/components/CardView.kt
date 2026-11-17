package com.nowlsantos.big2ai.presentation.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.util.CardImageMapper.drawableShortName
import com.nowlsantos.big2ai.util.CardImageMapper.getCardDrawableResId
import com.nowlsantos.big2ai.util.CARD_HEIGHT
import com.nowlsantos.big2ai.util.CARD_WIDTH

@Composable
fun CardView(
    card: PlayingCard,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    faceUp: Boolean = false,
    onClick: ((PlayingCard) -> Unit)? = null
) {
    val context = LocalContext.current
    val imageResId = context.getCardDrawableResId(card)

    // Animate the vertical offset for selected cards
    val offsetY by animateDpAsState(
        targetValue = if (isSelected) (-30).dp else 0.dp, // Lift card by 30dp when selected
        animationSpec = tween(durationMillis = 100),
        label = "cardOffsetAnimation"
    )

    // Base modifier that includes size and the animated offset
    val baseModifier = modifier
        .size(CARD_WIDTH, CARD_HEIGHT) // Use passed dimensions
        .offset(y = offsetY) // Apply the animated offset

    // Conditionally apply clickable if onClick is provided
    val clickableModifier = if (onClick != null) {
        baseModifier.clickable {
            onClick(card)
        }
    } else {
        baseModifier
    }

    if (imageResId != 0) {
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = if (faceUp) "${card.rank.symbol} of ${card.suit.toChar()}" else "Card Back",
            modifier = clickableModifier
        )
    } else {
        // Fallback in case imageResId is 0
        Text(
            text = card.drawableShortName, // Assuming PlayCard has this property
            modifier = clickableModifier // Apply clickable here too
                .background(Color.Gray) // Existing fallback background
        )
    }
}