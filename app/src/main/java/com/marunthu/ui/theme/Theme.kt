package com.marunthu.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

// ---- Brand palette (medical teal + warm accents), tuned like 1mg / PharmEasy ----
val Teal          = Color(0xFF0E7C66)
val TealDeep      = Color(0xFF0A5C4C)
val Mint          = Color(0xFFCFF3EA)
val MintSurface   = Color(0xFFF3FBF8)
val Amber         = Color(0xFFF3A712)
val Coral         = Color(0xFFE0603A)
val Ink           = Color(0xFF12211C)
val InkSoft       = Color(0xFF5A6B65)

// Semantic safety colors
val SafeGreen   = Color(0xFF1E9E6A)
val WarnAmber   = Color(0xFFE8A400)
val DangerRed   = Color(0xFFD64545)

val HeroGradient = Brush.verticalGradient(listOf(Teal, TealDeep))
val ScanGradient = Brush.linearGradient(listOf(Teal, Color(0xFF12A588)))

private val Scheme = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Mint,
    onPrimaryContainer = TealDeep,
    secondary = Amber,
    onSecondary = Color.White,
    tertiary = Coral,
    background = MintSurface,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE7F1EE),
    onSurfaceVariant = InkSoft,
    error = DangerRed,
    outline = Color(0xFFBFD6CF),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

private val AppType = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp),
)

@Composable
fun MarunthuTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, shapes = AppShapes, typography = AppType, content = content)
}
