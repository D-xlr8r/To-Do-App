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
    // Violet family: a strong violet for actions, a deep violet for text accents,
    // and pale lavender tints for backgrounds and highlights.
    val Violet = Color(0xFF7C3AED)      // brand
    val VioletDeep = Color(0xFF5B21B6)  // brand-d
    val VioletInk = Color(0xFF3B1A86)   // brand-x
    val VioletTint = Color(0xFFEDE9FE)  // brand-t
    val Lavender = Color(0xFFC4B5FD)
    val Bg = Color(0xFFF8F7FC)
    val Ink = Color(0xFF111114)
    val Muted = Color(0xFF5F5F72)
    val Line = Color(0xFFECE8F5)
    val CalendarDot = Color(0xFFDC3545)

    // Older names used around the app
    val Ivory = Bg
    val Sage = Violet
    val SageLight = VioletTint
    val Charcoal = Ink
}

private val Light = lightColorScheme(
    primary = Brand.Violet,
    onPrimary = Color.White,
    primaryContainer = Brand.VioletTint,
    onPrimaryContainer = Brand.VioletInk,
    inversePrimary = Brand.Lavender,
    secondary = Brand.VioletDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3F0FA),
    onSecondaryContainer = Brand.VioletInk,
    tertiary = Color(0xFFC58A00),
    background = Brand.Bg,
    onBackground = Brand.Ink,
    surface = Brand.Bg,
    onSurface = Brand.Ink,
    surfaceVariant = Color(0xFFEEEAF7),
    onSurfaceVariant = Brand.Muted,
    surfaceTint = Brand.Violet,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFCFBFE),
    surfaceContainer = Color(0xFFF6F2FF),
    surfaceContainerHigh = Color(0xFFF3F0FA),
    surfaceContainerHighest = Color(0xFFEEEAF7),
    outline = Color(0xFFD4CCE8),
    outlineVariant = Brand.Line,
    error = Color(0xFFDC3545),
    errorContainer = Color(0xFFFCE4E6),
    inverseSurface = Color(0xFF221B33),
    inverseOnSurface = Color(0xFFF2F0FA),
)

private val Dark = darkColorScheme(
    primary = Brand.Lavender,
    onPrimary = Color(0xFF2A1068),
    primaryContainer = Color(0xFF2E2350),
    onPrimaryContainer = Color(0xFFE6DEFF),
    inversePrimary = Brand.Violet,
    secondary = Color(0xFFB9A9F5),
    secondaryContainer = Color(0xFF29253A),
    onSecondaryContainer = Color(0xFFE6DEFF),
    background = Color(0xFF121019),
    onBackground = Color(0xFFF2F0FA),
    surface = Color(0xFF121019),
    onSurface = Color(0xFFF2F0FA),
    surfaceVariant = Color(0xFF29253A),
    onSurfaceVariant = Color(0xFFA9A6BD),
    surfaceTint = Brand.Lavender,
    surfaceContainerLowest = Color(0xFF0C0A12),
    surfaceContainerLow = Color(0xFF17141F),
    surfaceContainer = Color(0xFF1C1A26),
    surfaceContainerHigh = Color(0xFF24202F),
    surfaceContainerHighest = Color(0xFF2E2A44),
    outline = Color(0xFF4A4563),
    outlineVariant = Color(0xFF2A2640),
    error = Color(0xFFF28B95),
    inverseSurface = Color(0xFFF2F0FA),
    inverseOnSurface = Color(0xFF1C1A26),
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
    low = PriorityColors(Color(0xFFE3F4EA), Color(0xFF34A06A), "Low"),
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
