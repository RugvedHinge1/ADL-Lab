package com.scholr.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ClayOutline = Color(0x33A39BBE)

private val ScholrColorScheme = lightColorScheme(
    primary = ScholrPurple,
    onPrimary = TextOnClay,
    primaryContainer = ScholrPurpleLight,
    onPrimaryContainer = TextOnClay,
    secondary = AcademicGreen,
    onSecondary = TextOnClay,
    secondaryContainer = AcademicGreenLight,
    background = ClayBase,
    onBackground = TextHeading,
    surface = ClaySurface,
    onSurface = TextHeading,
    surfaceVariant = ClayRecess,
    onSurfaceVariant = TextBody,
    outline = ClayOutline,
    error = FieldMedicine
)

/**
 * Scholr is a deliberately single-mode (light clay) design language — the whole
 * shadow system depends on a light base surface, so there is no dark scheme.
 * System bars are drawn transparent with dark icons.
 */
@Composable
fun ScholrTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = ScholrColorScheme,
        typography = ScholrTypography,
        content = content
    )
}
