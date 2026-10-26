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
    val Ivory = Color(0xFFFAF8F3)
    val Sage = Color(0xFF4F7A5E)
    val SageLight = Color(0xFFDCEADF)
    val Butter = Color(0xFFF6C85F)
    val Coral = Color(0xFFE6776C)
    val Charcoal = Color(0xFF1E211F)
    val CalendarDot = Color(0xFFE0564B)
}

private val Light = lightColorScheme(
    primary = Brand.Sage,
    onPrimary = Color.White,
    primaryContainer = Brand.SageLight,
    onPrimaryContainer = Color(0xFF1F3A28),
    secondary = Color(0xFF7D8C81),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEAF1EB),
    onSecondaryContainer = Color(0xFF243329),
    tertiary = Color(0xFFB98A2E),
    background = Brand.Ivory,
    onBackground = Brand.Charcoal,
    surface = Brand.Ivory,
    onSurface = Brand.Charcoal,
    surfaceVariant = Color(0xFFF1EEE7),
    onSurfaceVariant = Color(0xFF6B706C),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFDFCF9),
    surfaceContainer = Color(0xFFF6F4EE),
    surfaceContainerHigh = Color(0xFFF1EEE7),
    surfaceContainerHighest = Color(0xFFEBE8E0),
    outline = Color(0xFFD5D1C8),
    outlineVariant = Color(0xFFE8E4DB),
    error = Color(0xFFC4534A),
    inverseSurface = Color(0xFF2B2E2C),
    inverseOnSurface = Color(0xFFF2F0EA),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF9CC5A8),
    onPrimary = Color(0xFF14301E),
    primaryContainer = Color(0xFF2E4A37),
    onPrimaryContainer = Color(0xFFD5EADB),
    secondary = Color(0xFFA9B7AC),
    secondaryContainer = Color(0xFF2B332D),
    onSecondaryContainer = Color(0xFFDDE6DF),
    background = Color(0xFF121413),
    onBackground = Color(0xFFE4E3DE),
    surface = Color(0xFF121413),
    onSurface = Color(0xFFE4E3DE),
    surfaceVariant = Color(0xFF242826),
    onSurfaceVariant = Color(0xFFA8ADA9),
    surfaceContainerLowest = Color(0xFF0E100F),
    surfaceContainerLow = Color(0xFF1A1D1B),
    surfaceContainer = Color(0xFF1E2220),
    surfaceContainerHigh = Color(0xFF252927),
    surfaceContainerHighest = Color(0xFF2F3331),
    outline = Color(0xFF4A504C),
    outlineVariant = Color(0xFF353A37),
    error = Color(0xFFE88F86),
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
    low = PriorityColors(Color(0xFFE4F2E8), Color(0xFF6DAA7F), "Low"),
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
