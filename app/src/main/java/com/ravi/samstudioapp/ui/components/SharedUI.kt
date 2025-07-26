package com.ravi.samstudioapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color as ComposeColor

// Shared colors
val Black = ComposeColor(0xFF121212)
val DarkGray = ComposeColor(0xFF080809)
val LightGray = ComposeColor(0xFFE0E0E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeumorphicBorderBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    backgroundColor: Color = Black,
    borderColor: Color = Color.White.copy(alpha = 0.10f),
    borderWidth: Dp = 1.dp,
    shadowColor: Color = Color.Black.copy(alpha = 0.25f),
    shadowElevation: Dp = 8.dp,
    contentPadding: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(cornerRadius)
            )
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(contentPadding)
    ) {
        content()
    }
} 