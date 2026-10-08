package com.scholr.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/* ------------------------------------------------------------------------
 *  TYPOGRAPHY — Plus Jakarta Sans
 *
 *  The project ships compiling out of the box on the platform sans so you can
 *  run it immediately. To switch to the real brand face:
 *
 *    1. Download Plus Jakarta Sans from fonts.google.com
 *    2. Drop these four static files into  app/src/main/res/font/
 *         plus_jakarta_sans_medium.ttf     (500)
 *         plus_jakarta_sans_semibold.ttf   (600)
 *         plus_jakarta_sans_bold.ttf       (700)
 *         plus_jakarta_sans_extrabold.ttf  (800)
 *       (res/font filenames must be lowercase with underscores only)
 *    3. Uncomment the `PlusJakartaSans` block below and point
 *       `ScholrFontFamily` at it.
 *
 *  Nothing else in the app needs to change — every text style resolves
 *  through ScholrFontFamily.
 * ---------------------------------------------------------------------- */

/*
import androidx.compose.ui.text.font.Font
import com.scholr.app.R

val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_medium,     FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold,   FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold,       FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold,  FontWeight.ExtraBold),
)
*/

/** Single switch point for the whole app's face. */
val ScholrFontFamily: FontFamily = FontFamily.Default
// val ScholrFontFamily: FontFamily = PlusJakartaSans   // <- flip to this after step 3

/**
 * The "Scholr" wordmark in the header. Large display weight, tight tracking.
 */
val LogoStyle = TextStyle(
    fontFamily = ScholrFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 26.sp,
    lineHeight = 30.sp,
    letterSpacing = (-0.6).sp,
    color = TextHeading
)

val DisplayStyle = TextStyle(
    fontFamily = ScholrFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 34.sp,
    lineHeight = 40.sp,
    letterSpacing = (-1).sp,
    color = TextHeading
)

/**
 * Bold (700) for headlines, Medium (500) for body — per the design language.
 */
val ScholrTypography = Typography(
    displayLarge = DisplayStyle,
    displayMedium = TextStyle(
        fontFamily = ScholrFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.6).sp,
        color = TextHeading
    ),
    headlineMedium = TextStyle(
        fontFamily = ScholrFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp,
        color = TextHeading
    ),
    headlineSmall = TextStyle(
        fontFamily = ScholrFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp,
        color = TextHeading
    ),
    titleMedium = TextStyle(
        fontFamily = ScholrFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = TextHeading
    ),
    titleSmall = TextStyle(
        fontFamily = ScholrFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        color = TextHeading
    ),
    bodyLarge = TextStyle(
        fontFamily = ScholrFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 23.sp,
        color = TextBody
    ),
    bodyMedium = TextStyle(
        fontFamily = ScholrFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.5.sp,
        lineHeight = 20.sp,
        color = TextBody
    ),
    bodySmall = TextStyle(
        fontFamily = ScholrFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        color = TextBody
    ),
    labelLarge = TextStyle(
        fontFamily = ScholrFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = ScholrFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = ScholrFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.9.sp
    )
)
