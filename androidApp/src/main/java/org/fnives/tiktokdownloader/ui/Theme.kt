package org.fnives.tiktokdownloader.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Purple200 = Color(0xFFBB86FC)
val Purple500 = Color(0xFF6200EE)
val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)
val CloseToBlack = Color(0xFF0F0F0F)
val CloseToWhite = Color(0xFFF0F0F0)

val LightSurface = Color(0xFFFFFBFE)
val LightBackground = Color(0xFFFFFBFE)

val LightSurfaceContainer = Color(0xFFF2ECF4)
val DarkSurfaceContainer = Color(0xFF211F24)
val DarkSecondaryContainer = Purple200
val DarkOnSecondaryContainer = CloseToBlack
val LightSecondaryContainer = Purple500
val LightOnSecondaryContainer = CloseToWhite

val Typography = Typography(
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
)

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = CircleShape,
    extraLarge = RoundedCornerShape(24.dp),
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple200,
    onPrimary = Black,
    primaryContainer = Purple200,
    onPrimaryContainer = White,
    secondary = Purple200,
    onSecondary = Black,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = Purple200,
    onTertiary = Black,
    background = CloseToBlack,
    onBackground = CloseToWhite,
    surface = CloseToBlack,
    onSurface = CloseToWhite,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerLow = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainer,
    surfaceContainerHighest = DarkSurfaceContainer,
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Purple200,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple500,
    onPrimary = White,
    primaryContainer = Purple500,
    onPrimaryContainer = Black,
    secondary = Purple500,
    onSecondary = White,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = Purple500,
    onTertiary = White,

    background = LightBackground,
    onBackground = CloseToBlack,
    surface = LightSurface,
    onSurface = CloseToBlack,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerLow = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainer,
    surfaceContainerHighest = LightSurfaceContainer,

    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Purple500,

    error = Color(0xFFB3261E),
    onError = White
)

@Composable
fun TikTokDownloaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,

        content = content
    )
}