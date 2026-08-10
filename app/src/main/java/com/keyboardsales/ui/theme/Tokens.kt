// GENERATED FROM design/tokens.json — DO NOT EDIT
// Regenerar: python3 design/gen-tokens.py --out .

package com.keyboardsales.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Escalas de dimension. No dependen del modo. */
object Dim {
    val spacing0 = 0.dp
    val spacing1 = 4.dp
    val spacing2 = 8.dp
    val spacing3 = 12.dp
    val spacing4 = 16.dp
    val spacing5 = 20.dp
    val spacing6 = 24.dp
    val spacing8 = 32.dp
    val spacing10 = 40.dp
    val spacing12 = 48.dp
    val radiusXs = 4.dp
    val radiusSm = 8.dp
    val radiusMd = 12.dp
    val radiusLg = 20.dp
    val radiusXl = 28.dp
    val radiusPill = 999.dp
    val sizeTouchMin = 48.dp
    val sizeControlHeight = 48.dp
    val sizeControlHeightSm = 36.dp
    val sizeChipHeight = 32.dp
    val sizeRowMinHeight = 56.dp
    val sizeRowMinHeightLg = 72.dp
    val sizeAvatarSm = 32.dp
    val sizeAvatarMd = 40.dp
    val sizeAvatarLg = 56.dp
    val sizeThumbMd = 56.dp
    val sizeProgressHeight = 4.dp
    val sizeBoardColumnWidth = 280.dp
    val sizeScreenMaxWidth = 560.dp
    val borderWidthHairline = 1.dp
    val borderWidthFocus = 2.dp
    val borderOffsetFocus = 2.dp
    val iconSizeSm = 16.dp
    val iconSizeMd = 20.dp
    val iconSizeLg = 24.dp
    val iconStroke = 1.75.dp
    val kbBarHeight = 48.dp
    val kbBarHeightExpanded = 96.dp
    val kbBarPadH = 8.dp
    val kbBarGap = 6.dp
    val kbChipHeight = 36.dp
    val kbAnchorSize = 40.dp
    val kbRowHeight = 46.dp
    val kbRowCount = 4
    val kbKeyGutter = 4.dp
    val kbKeyRadius = 8.dp
    val kbPadH = 4.dp
    val kbPadV = 4.dp
    val kbTouchMin = 44.dp
    val kbPanelHeight = 192.dp
    val kbPopupHeight = 40.dp
    val kbPopupOffset = 6.dp
    val kbCardWidth = 132.dp
    val kbCardImageHeight = 88.dp
    val kbConfirmHeightMin = 180.dp
    val kbBarNoticeHeight = 48.dp
    val zBase = 0
    val zBar = 10
    val zSticky = 20
    val zSheet = 30
    val zModal = 40
    val zToast = 50
    val zKeypopup = 60
}

/** Duraciones de motion, en milisegundos. */
object Motion {
    const val motionInstant = 0
    const val motionFast = 100
    const val motionBase = 160
    const val motionPanel = 220
    const val motionSheet = 280
    const val motionUndoDuration = 3000
    const val motionUndoDurationA11y = 10000
    const val motionToastDuration = 4000
    const val motionSkeleton = 1200
}

/** Estilos tipograficos. Familia del sistema: sin fuente propia (04.2 §5.1). */
object Type {
    // type/key/letter
    val typeKeyLetterSize = 22.sp
    val typeKeyLetterWeight = 400
    val typeKeyLetterLineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
    // type/key/label
    val typeKeyLabelSize = 11.sp
    val typeKeyLabelWeight = 500
    val typeKeyLabelLineHeight = 14.sp
    // type/bar/suggestion
    val typeBarSuggestionSize = 16.sp
    val typeBarSuggestionWeight = 400
    val typeBarSuggestionLineHeight = 20.sp
    // type/bar/suggestion-emphasis
    val typeBarSuggestionEmphasisSize = 16.sp
    val typeBarSuggestionEmphasisWeight = 600
    val typeBarSuggestionEmphasisLineHeight = 20.sp
    // type/bar/chip-price
    val typeBarChipPriceSize = 13.sp
    val typeBarChipPriceWeight = 600
    val typeBarChipPriceLineHeight = 16.sp
    // type/heading/h1
    val typeHeadingH1Size = 28.sp
    val typeHeadingH1Weight = 700
    val typeHeadingH1LineHeight = 34.sp
    // type/heading/h2
    val typeHeadingH2Size = 22.sp
    val typeHeadingH2Weight = 600
    val typeHeadingH2LineHeight = 28.sp
    // type/heading/h3
    val typeHeadingH3Size = 18.sp
    val typeHeadingH3Weight = 600
    val typeHeadingH3LineHeight = 24.sp
    // type/body/large
    val typeBodyLargeSize = 17.sp
    val typeBodyLargeWeight = 400
    val typeBodyLargeLineHeight = 24.sp
    // type/body/regular
    val typeBodyRegularSize = 15.sp
    val typeBodyRegularWeight = 400
    val typeBodyRegularLineHeight = 21.sp
    // type/body/regular-medium
    val typeBodyRegularMediumSize = 15.sp
    val typeBodyRegularMediumWeight = 500
    val typeBodyRegularMediumLineHeight = 21.sp
    // type/body/small
    val typeBodySmallSize = 13.sp
    val typeBodySmallWeight = 400
    val typeBodySmallLineHeight = 18.sp
    // type/supporting/label
    val typeSupportingLabelSize = 12.sp
    val typeSupportingLabelWeight = 500
    val typeSupportingLabelLineHeight = 16.sp
    // type/supporting/button
    val typeSupportingButtonSize = 15.sp
    val typeSupportingButtonWeight = 600
    val typeSupportingButtonLineHeight = 20.sp
    // type/mono/price
    val typeMonoPriceSize = 15.sp
    val typeMonoPriceWeight = 600
    val typeMonoPriceLineHeight = 20.sp
}

@Immutable
data class AppColors(
    val surfaceKeyboard: Color,
    val surfaceBar: Color,
    val surfacePanel: Color,
    val surfaceApp: Color,
    val surfaceRaised: Color,
    val surfaceOverlay: Color,
    val surfaceScrim: Color,
    val surfaceInverse: Color,
    val surfaceTrack: Color,
    val surfacePlaceholder: Color,
    val keyLetterBg: Color,
    val keyLetterBgPressed: Color,
    val keyModifierBg: Color,
    val keyModifierBgPressed: Color,
    val keyActionBg: Color,
    val keyActionBgPressed: Color,
    val keyLabel: Color,
    val keyLabelSecondary: Color,
    val keyLabelOnAction: Color,
    val keyPopupBg: Color,
    val keyPopupBgSelected: Color,
    val contentPrimary: Color,
    val contentSecondary: Color,
    val contentTertiary: Color,
    val contentDisabled: Color,
    val contentOnAccent: Color,
    val contentOnError: Color,
    val contentInverse: Color,
    val contentLink: Color,
    val borderSubtle: Color,
    val borderDefault: Color,
    val borderStrong: Color,
    val borderFocus: Color,
    val accentDefault: Color,
    val accentPressed: Color,
    val accentSubtle: Color,
    val accentOnSubtle: Color,
    val accentDisabled: Color,
    val feedbackError: Color,
    val feedbackErrorSolid: Color,
    val feedbackErrorSubtle: Color,
    val feedbackErrorOnSubtle: Color,
    val feedbackWarning: Color,
    val feedbackWarningSubtle: Color,
    val feedbackWarningOnSubtle: Color,
    val feedbackSuccess: Color,
    val feedbackSuccessSubtle: Color,
    val feedbackSuccessOnSubtle: Color,
    val feedbackInfo: Color,
    val feedbackInfoSubtle: Color,
    val feedbackInfoOnSubtle: Color,
    val statePressed: Color,
    val stateSelected: Color,
)

val LightColors = AppColors(
    surfaceKeyboard = Color(0xFFE1E4EA),
    surfaceBar = Color(0xFFE1E4EA),
    surfacePanel = Color(0xFFF7F8FA),
    surfaceApp = Color(0xFFF7F8FA),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceOverlay = Color(0xFFFFFFFF),
    surfaceScrim = Color(0x73101216),
    surfaceInverse = Color(0xFF22252C),
    surfaceTrack = Color(0xFFE1E4EA),
    surfacePlaceholder = Color(0xFFE1E4EA),
    keyLetterBg = Color(0xFFFFFFFF),
    keyLetterBgPressed = Color(0xFFC9CDD6),
    keyModifierBg = Color(0xFFC9CDD6),
    keyModifierBgPressed = Color(0xFF9BA1AE),
    keyActionBg = Color(0xFF4F4CD4),
    keyActionBgPressed = Color(0xFF413CB0),
    keyLabel = Color(0xFF191C22),
    keyLabelSecondary = Color(0xFF545A67),
    keyLabelOnAction = Color(0xFFFFFFFF),
    keyPopupBg = Color(0xFFFFFFFF),
    keyPopupBgSelected = Color(0xFF4F4CD4),
    contentPrimary = Color(0xFF191C22),
    contentSecondary = Color(0xFF545A67),
    contentTertiary = Color(0xFF6E7583),
    contentDisabled = Color(0xFFC9CDD6),
    contentOnAccent = Color(0xFFFFFFFF),
    contentOnError = Color(0xFFFFFFFF),
    contentInverse = Color(0xFFF7F8FA),
    contentLink = Color(0xFF413CB0),
    borderSubtle = Color(0x14101216),
    borderDefault = Color(0x1F101216),
    borderStrong = Color(0xFF6E7583),
    borderFocus = Color(0xFF4F4CD4),
    accentDefault = Color(0xFF4F4CD4),
    accentPressed = Color(0xFF35318B),
    accentSubtle = Color(0xFFEEEFFE),
    accentOnSubtle = Color(0xFF413CB0),
    accentDisabled = Color(0xFFC9CDD6),
    feedbackError = Color(0xFFDC2626),
    feedbackErrorSolid = Color(0xFFDC2626),
    feedbackErrorSubtle = Color(0xFFFEF2F2),
    feedbackErrorOnSubtle = Color(0xFFB91C1C),
    feedbackWarning = Color(0xFFD97706),
    feedbackWarningSubtle = Color(0xFFFFFBEB),
    feedbackWarningOnSubtle = Color(0xFFB45309),
    feedbackSuccess = Color(0xFF16A34A),
    feedbackSuccessSubtle = Color(0xFFF0FDF4),
    feedbackSuccessOnSubtle = Color(0xFF15803D),
    feedbackInfo = Color(0xFF2563EB),
    feedbackInfoSubtle = Color(0xFFEFF6FF),
    feedbackInfoOnSubtle = Color(0xFF1D4ED8),
    statePressed = Color(0x1F101216),
    stateSelected = Color(0xFFEEEFFE),
)

val DarkColors = AppColors(
    surfaceKeyboard = Color(0xFF191C22),
    surfaceBar = Color(0xFF191C22),
    surfacePanel = Color(0xFF101216),
    surfaceApp = Color(0xFF101216),
    surfaceRaised = Color(0xFF22252C),
    surfaceOverlay = Color(0xFF22252C),
    surfaceScrim = Color(0x99101216),
    surfaceInverse = Color(0xFFE1E4EA),
    surfaceTrack = Color(0xFF2B2F38),
    surfacePlaceholder = Color(0xFF2B2F38),
    keyLetterBg = Color(0xFF3E434E),
    keyLetterBgPressed = Color(0xFF545A67),
    keyModifierBg = Color(0xFF2B2F38),
    keyModifierBgPressed = Color(0xFF3E434E),
    keyActionBg = Color(0xFF6366E4),
    keyActionBgPressed = Color(0xFF4F4CD4),
    keyLabel = Color(0xFFF7F8FA),
    keyLabelSecondary = Color(0xFF9BA1AE),
    keyLabelOnAction = Color(0xFFFFFFFF),
    keyPopupBg = Color(0xFF22252C),
    keyPopupBgSelected = Color(0xFF6366E4),
    contentPrimary = Color(0xFFF7F8FA),
    contentSecondary = Color(0xFF9BA1AE),
    contentTertiary = Color(0xFF6E7583),
    contentDisabled = Color(0xFF545A67),
    contentOnAccent = Color(0xFFFFFFFF),
    contentOnError = Color(0xFFFFFFFF),
    contentInverse = Color(0xFF191C22),
    contentLink = Color(0xFFA2A4F6),
    borderSubtle = Color(0x14FFFFFF),
    borderDefault = Color(0x1FFFFFFF),
    borderStrong = Color(0xFF9BA1AE),
    borderFocus = Color(0xFF8183EF),
    accentDefault = Color(0xFF6366E4),
    accentPressed = Color(0xFF4F4CD4),
    accentSubtle = Color(0xFF1C1A45),
    accentOnSubtle = Color(0xFFC5C7FB),
    accentDisabled = Color(0xFF3E434E),
    feedbackError = Color(0xFFF87171),
    feedbackErrorSolid = Color(0xFFDC2626),
    feedbackErrorSubtle = Color(0xFF450A0A),
    feedbackErrorOnSubtle = Color(0xFFF87171),
    feedbackWarning = Color(0xFFFBBF24),
    feedbackWarningSubtle = Color(0xFF451A03),
    feedbackWarningOnSubtle = Color(0xFFFBBF24),
    feedbackSuccess = Color(0xFF4ADE80),
    feedbackSuccessSubtle = Color(0xFF052E16),
    feedbackSuccessOnSubtle = Color(0xFF4ADE80),
    feedbackInfo = Color(0xFF60A5FA),
    feedbackInfoSubtle = Color(0xFF172554),
    feedbackInfoOnSubtle = Color(0xFF60A5FA),
    statePressed = Color(0x1FFFFFFF),
    stateSelected = Color(0xFF1C1A45),
)

val LocalAppColors = compositionLocalOf { LightColors }

/** Punto de acceso unico al color. Nunca se escribe un Color(0x...) en un componente. */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
}
