package com.ravi.samstudioapp.ui.theme

// Import the custom palette
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    onPrimary = OnPrimary,
    secondary = TealSecondary,
    onSecondary = OnSecondary,
    background = GrayBackground,
    onBackground = OnBackground,
    surface = GraySurface,
    onSurface = OnSurface,
    error = ErrorRed,
    onError = OnPrimary
)

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun SamStudioAppTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}