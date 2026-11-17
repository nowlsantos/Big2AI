package com.nowlsantos.big2ai.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@Composable
fun DelayedContentDisplay(
    delayMillis: Long,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }

    // LaunchedEffect will run its block when this composable enters the composition.
    // It will be cancelled if DelayedContentDisplay leaves the composition.
    LaunchedEffect(key1 = Unit) { // 'Unit' as key means this effect runs once when composable enters.
        delay(delayMillis) // Pause execution for the specified duration
        showContent = true // After delay, set state to true to show content
    }

    // Use AnimatedVisibility for a smooth fade-in/slide-in effect
    AnimatedVisibility(
        visible = showContent,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        content() // The actual content to be displayed
    }
}