package com.malayspeak.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = GlassEmerald,
    onPrimary = FrostPaper,
    primaryContainer = GlassEmeraldMist,
    onPrimaryContainer = GlassEmeraldInk,
    secondary = GlassAmber,
    onSecondary = GlassAmberInk,
    secondaryContainer = GlassAmberMist,
    onSecondaryContainer = GlassAmberDeepInk,
    tertiary = GlassIndigo,
    onTertiary = FrostPaper,
    tertiaryContainer = GlassIndigoMist,
    onTertiaryContainer = GlassIndigoInk,
    background = BackdropLightTop,
    onBackground = InkDeep,
    surface = FrostPaper,
    onSurface = InkDeep,
    surfaceVariant = FrostSurfaceDim,
    onSurfaceVariant = InkMuted,
    outline = LineLight,
    outlineVariant = LineLightSoft,
    error = ErrorLight,
    onError = FrostPaper,
    errorContainer = ErrorLightContainer,
    onErrorContainer = ErrorLightInk,
    inverseSurface = InkDeep,
    inverseOnSurface = BackdropLightTop,
    surfaceTint = GlassEmerald
)

private val DarkColorScheme = darkColorScheme(
    primary = MintGlow,
    onPrimary = MintInk,
    primaryContainer = CanopyContainer,
    onPrimaryContainer = CanopyInk,
    secondary = AmberGlow,
    onSecondary = AmberInk,
    secondaryContainer = OchreContainer,
    onSecondaryContainer = OchreInk,
    tertiary = LavenderGlow,
    onTertiary = LavenderInk,
    tertiaryContainer = IndigoContainer,
    onTertiaryContainer = IndigoInk,
    background = BackdropDarkTop,
    onBackground = FrostWhite,
    surface = NightSurface,
    onSurface = FrostWhite,
    surfaceVariant = NightRaised,
    onSurfaceVariant = FrostMuted,
    outline = LineDark,
    outlineVariant = LineDarkSoft,
    error = ErrorDark,
    onError = ErrorDarkOn,
    errorContainer = ErrorDarkContainer,
    onErrorContainer = ErrorDarkInk,
    inverseSurface = FrostWhite,
    inverseOnSurface = BackdropDarkTop,
    surfaceTint = MintGlow
)

/**
 * Glassmorphism geometry: large rounded corners (20-28dp) for glass cards,
 * dialogs, search bars, and navigation surfaces.
 */
val MalaySpeakShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Immutable
data class MalaySpeakGlassTokens(
    val fill: Color,
    val fillElevated: Color,
    val fillSubtle: Color,
    val borderStart: Color,
    val borderEnd: Color,
    val borderHighlightStart: Color,
    val borderHighlightEnd: Color,
    val shadowColor: Color,
    val backdropTop: Color,
    val backdropMid: Color,
    val backdropBottom: Color,
    val blobPrimary: Color,
    val blobSecondary: Color,
    val blobAccent: Color,
    val borderWidth: Dp = 1.dp,
    val cardShadowElevation: Dp = 6.dp,
    val elevatedShadowElevation: Dp = 10.dp,
    val compactShape: Shape = RoundedCornerShape(20.dp),
    val searchShape: Shape = RoundedCornerShape(22.dp),
    val cardShape: Shape = RoundedCornerShape(24.dp),
    val navBarShape: Shape = RoundedCornerShape(26.dp),
    val heroShape: Shape = RoundedCornerShape(28.dp),
    val dialogShape: Shape = RoundedCornerShape(28.dp)
)

@Immutable
data class MalaySpeakRingColors(
    val overall: Color,
    val learned: Color,
    val goal: Color,
    val track: Color,
    val categoryPalette: List<Color>
)

@Immutable
data class MalaySpeakSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val screenHorizontal: Dp = 18.dp,
    val screenTop: Dp = 12.dp,
    val screenBottom: Dp = 28.dp,
    val sectionGap: Dp = 20.dp,
    val itemGap: Dp = 10.dp,
    val cardPadding: Dp = 18.dp,
    val cardPaddingCompact: Dp = 14.dp,
    val heroPadding: Dp = 20.dp,
    val dialogPadding: Dp = 24.dp
)

@Immutable
data class MalaySpeakSizes(
    val minTouchTarget: Dp = 48.dp,
    val buttonHeight: Dp = 48.dp,
    val buttonHeightLarge: Dp = 52.dp,
    val iconXs: Dp = 14.dp,
    val iconSm: Dp = 16.dp,
    val iconMd: Dp = 20.dp,
    val iconLg: Dp = 24.dp,
    val iconXl: Dp = 30.dp,
    val audioButton: Dp = 48.dp,
    val audioButtonLarge: Dp = 52.dp,
    val ringCompact: Dp = 64.dp,
    val ringCompactStroke: Dp = 6.dp,
    val ringMedium: Dp = 76.dp,
    val ringMediumStroke: Dp = 7.dp,
    val ringHero: Dp = 118.dp,
    val ringHeroStroke: Dp = 10.dp,
    val progressRing: Dp = 196.dp,
    val progressRingStroke: Dp = 12.dp,
    val concentricGap: Dp = 7.dp,
    val borderWidth: Dp = 1.dp,
    val accentStripeWidth: Dp = 4.dp,
    val accentStripeHeight: Dp = 36.dp,
    val badgeDot: Dp = 7.dp,
    val searchBadge: Dp = 38.dp,
    val loadingIndicator: Dp = 34.dp,
    val loadingStroke: Dp = 3.dp,
    val emptyMedallion: Dp = 68.dp
)

@Immutable
data class MalaySpeakComponentStyles(
    val buttonShape: Shape = RoundedCornerShape(16.dp),
    val chipShape: Shape = RoundedCornerShape(14.dp),
    val pillShape: Shape = RoundedCornerShape(12.dp),
    val iconButtonShape: Shape = RoundedCornerShape(14.dp),
    val navItemShape: Shape = RoundedCornerShape(20.dp),
    val buttonContentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    val pillPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
    val chipPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
)

@Immutable
data class MalaySpeakTextStyles(
    val eyebrow: TextStyle = EyebrowTextStyle,
    val bodyEmphasis: TextStyle = BodyEmphasisTextStyle,
    val pronunciation: TextStyle = PronunciationTextStyle,
    val metricValue: TextStyle = MetricValueTextStyle,
    val ringSmallValue: TextStyle = RingSmallValueTextStyle
)

private val LightGlassTokens = MalaySpeakGlassTokens(
    fill = FrostPaper.copy(alpha = 0.62f),
    fillElevated = FrostPaper.copy(alpha = 0.70f),
    fillSubtle = FrostPaper.copy(alpha = 0.44f),
    borderStart = FrostPaper.copy(alpha = 0.90f),
    borderEnd = FrostPaper.copy(alpha = 0.14f),
    borderHighlightStart = GlassEmerald.copy(alpha = 0.62f),
    borderHighlightEnd = FrostPaper.copy(alpha = 0.25f),
    shadowColor = GlassShadowLight.copy(alpha = 0.11f),
    backdropTop = BackdropLightTop,
    backdropMid = BackdropLightMid,
    backdropBottom = BackdropLightBottom,
    blobPrimary = BlobLightPrimary.copy(alpha = 0.28f),
    blobSecondary = BlobLightSecondary.copy(alpha = 0.24f),
    blobAccent = BlobLightAccent.copy(alpha = 0.20f)
)

private val DarkGlassTokens = MalaySpeakGlassTokens(
    fill = FrostPaper.copy(alpha = 0.13f),
    fillElevated = FrostPaper.copy(alpha = 0.17f),
    fillSubtle = FrostPaper.copy(alpha = 0.08f),
    borderStart = FrostPaper.copy(alpha = 0.34f),
    borderEnd = FrostPaper.copy(alpha = 0.04f),
    borderHighlightStart = MintGlow.copy(alpha = 0.64f),
    borderHighlightEnd = FrostPaper.copy(alpha = 0.08f),
    shadowColor = GlassShadowDark.copy(alpha = 0.34f),
    backdropTop = BackdropDarkTop,
    backdropMid = BackdropDarkMid,
    backdropBottom = BackdropDarkBottom,
    blobPrimary = BlobDarkPrimary.copy(alpha = 0.26f),
    blobSecondary = BlobDarkSecondary.copy(alpha = 0.24f),
    blobAccent = BlobDarkAccent.copy(alpha = 0.17f)
)

private val LightRingColors = MalaySpeakRingColors(
    overall = GlassEmerald,
    learned = GlassAmberRing,
    goal = GlassIndigo,
    track = InkDeep.copy(alpha = 0.11f),
    categoryPalette = listOf(GlassEmerald, GlassIndigo, GlassAmberRing)
)

private val DarkRingColors = MalaySpeakRingColors(
    overall = MintGlow,
    learned = AmberGlow,
    goal = LavenderGlow,
    track = FrostPaper.copy(alpha = 0.14f),
    categoryPalette = listOf(MintGlow, LavenderGlow, AmberGlow)
)

private val DefaultSpacing = MalaySpeakSpacing()
private val DefaultSizes = MalaySpeakSizes()
private val DefaultStyles = MalaySpeakComponentStyles()
private val DefaultTextStyles = MalaySpeakTextStyles()

private val LocalGlass = staticCompositionLocalOf { LightGlassTokens }
private val LocalRings = staticCompositionLocalOf { LightRingColors }
private val LocalSpacing = staticCompositionLocalOf { DefaultSpacing }
private val LocalSizes = staticCompositionLocalOf { DefaultSizes }
private val LocalStyles = staticCompositionLocalOf { DefaultStyles }
private val LocalTextStyles = staticCompositionLocalOf { DefaultTextStyles }

/** Central design-token accessor so all glass and ring styles update from one place. */
object MalaySpeakTheme {
    val glass: MalaySpeakGlassTokens
        @Composable @ReadOnlyComposable get() = LocalGlass.current

    val rings: MalaySpeakRingColors
        @Composable @ReadOnlyComposable get() = LocalRings.current

    val spacing: MalaySpeakSpacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current

    val sizes: MalaySpeakSizes
        @Composable @ReadOnlyComposable get() = LocalSizes.current

    val styles: MalaySpeakComponentStyles
        @Composable @ReadOnlyComposable get() = LocalStyles.current

    val textStyles: MalaySpeakTextStyles
        @Composable @ReadOnlyComposable get() = LocalTextStyles.current

    @Composable
    @ReadOnlyComposable
    fun cardBorder(highlighted: Boolean = false): BorderStroke = BorderStroke(
        width = sizes.borderWidth,
        color = if (highlighted) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    )

    @Composable
    @ReadOnlyComposable
    fun destructiveBorder(): BorderStroke = BorderStroke(
        width = sizes.borderWidth,
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
    )

    @Composable
    fun learnedButtonColors(isLearned: Boolean): ButtonColors = ButtonDefaults.buttonColors(
        containerColor = if (isLearned) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.primary,
        contentColor = if (isLearned) MaterialTheme.colorScheme.onSecondary
        else MaterialTheme.colorScheme.onPrimary
    )

    @Composable
    fun destructiveOutlinedButtonColors(): ButtonColors = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.error
    )
}

@Composable
fun MalaySpeakTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme
    val glassTokens = if (isDarkTheme) DarkGlassTokens else LightGlassTokens
    val ringColors = if (isDarkTheme) DarkRingColors else LightRingColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            window.statusBarColor = glassTokens.backdropTop.toArgb()
            window.navigationBarColor = glassTokens.backdropBottom.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDarkTheme
                isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalGlass provides glassTokens,
        LocalRings provides ringColors,
        LocalSpacing provides DefaultSpacing,
        LocalSizes provides DefaultSizes,
        LocalStyles provides DefaultStyles,
        LocalTextStyles provides DefaultTextStyles
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = MalaySpeakShapes,
            content = content
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
