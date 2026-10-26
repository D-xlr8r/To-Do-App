package com.dxlr8r.notifyme.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dxlr8r.notifyme.data.Item

object Brand {
    // One green family: a deep forest green for anything you tap or should notice,
    // and pale tints of the same green for backgrounds and highlights.
    val Green900 = Color(0xFF123524)
    val Green800 = Color(0xFF1B4D35)   // primary
    val Green700 = Color(0xFF256244)
    val Green600 = Color(0xFF2F7A55)
    val Green200 = Color(0xFFB7DCC6)
    val Green100 = Color(0xFFD9EEE1)
    val Green50 = Color(0xFFEDF7F1)
    val Mist = Color(0xFFF6FAF7)       // app background: crisp white with a breath of green
    val Ink = Color(0xFF121A15)
    val CalendarDot = Color(0xFFE0453A)

    // Kept for places that referenced the old names
    val Ivory = Mist
    val Sage = Green800
    val SageLight = Green100
    val Charcoal = Ink
}

private val Light = lightColorScheme(
    primary = Brand.Green800,
    onPrimary = Color.White,
    primaryContainer = Brand.Green100,
    onPrimaryContainer = Brand.Green900,
    inversePrimary = Brand.Green200,
    secondary = Brand.Green600,
    onSecondary = Color.White,
    secondaryContainer = Brand.Green50,
    onSecondaryContainer = Brand.Green900,
    tertiary = Color(0xFFB98A2E),
    background = Brand.Mist,
    onBackground = Brand.Ink,
    surface = Brand.Mist,
    onSurface = Brand.Ink,
    surfaceVariant = Color(0xFFE8F0EB),
    onSurfaceVariant = Color(0xFF55615A),
    surfaceTint = Brand.Green800,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFDFC),
    surfaceContainer = Color(0xFFF0F6F2),
    surfaceContainerHigh = Color(0xFFE8F1EB),
    surfaceContainerHighest = Color(0xFFE0EBE4),
    outline = Color(0xFFC3D3C9),
    outlineVariant = Color(0xFFDDE8E1),
    error = Color(0xFFC0392B),
    errorContainer = Color(0xFFFBE3E0),
    inverseSurface = Brand.Green900,
    inverseOnSurface = Color(0xFFEAF5EE),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8ED1AA),
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF1B4D35),
    onPrimaryContainer = Color(0xFFCDEEDA),
    inversePrimary = Brand.Green800,
    secondary = Color(0xFF9CC9AE),
    secondaryContainer = Color(0xFF203A2B),
    onSecondaryContainer = Color(0xFFD5EADD),
    background = Color(0xFF0E1511),
    onBackground = Color(0xFFE2EBE5),
    surface = Color(0xFF0E1511),
    onSurface = Color(0xFFE2EBE5),
    surfaceVariant = Color(0xFF1F2A24),
    onSurfaceVariant = Color(0xFFA6B5AC),
    surfaceTint = Color(0xFF8ED1AA),
    surfaceContainerLowest = Color(0xFF09100C),
    surfaceContainerLow = Color(0xFF151E19),
    surfaceContainer = Color(0xFF19231D),
    surfaceContainerHigh = Color(0xFF212C25),
    surfaceContainerHighest = Color(0xFF2A362F),
    outline = Color(0xFF45554B),
    outlineVariant = Color(0xFF2E3B33),
    error = Color(0xFFF0958B),
    inverseSurface = Color(0xFFE2EBE5),
    inverseOnSurface = Color(0xFF16201A),
)

@Immutable
data class PriorityColors(val background: Color, val dot: Color, val label: String)

@Immutable
data class PriorityPalette(
    val none: PriorityColors,
    val low: PriorityColors,
    val medium: PriorityColors,
    val high: PriorityColors,
) {
    fun of(priority: Int): PriorityColors = when (priority) {
        Item.PRIORITY_HIGH -> high
        Item.PRIORITY_MEDIUM -> medium
        Item.PRIORITY_LOW -> low
        else -> none
    }
}

private val LightPriority = PriorityPalette(
    none = PriorityColors(Color.White, Color(0xFFC8CBC6), "None"),
    low = PriorityColors(Color(0xFFE2F2E8), Color(0xFF3E9A66), "Low"),
    medium = PriorityColors(Color(0xFFFDF3D6), Color(0xFFE2B444), "Medium"),
    high = PriorityColors(Color(0xFFFCE5E2), Color(0xFFE07A70), "High"),
)

private val DarkPriority = PriorityPalette(
    none = PriorityColors(Color(0xFF1C1F1D), Color(0xFF5A5F5B), "None"),
    low = PriorityColors(Color(0xFF1F2E24), Color(0xFF8CC49C), "Low"),
    medium = PriorityColors(Color(0xFF332D1E), Color(0xFFEBC66A), "Medium"),
    high = PriorityColors(Color(0xFF362523), Color(0xFFE9968D), "High"),
)

val LocalPriority = staticCompositionLocalOf { LightPriority }

private val base = Typography()
private val AppTypography = base.copy(
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
)

val ClockStyle = TextStyle(fontWeight = FontWeight.Light, letterSpacing = (-2).sp)

@Composable
fun NotifyTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPriority provides if (dark) DarkPriority else LightPriority) {
        MaterialTheme(
            colorScheme = if (dark) Dark else Light,
            typography = AppTypography,
            content = content,
        )
    }
}
