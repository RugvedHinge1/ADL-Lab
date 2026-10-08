package com.scholr.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/* ------------------------------------------------------------------------
 *  SCHOLR — Premium Claymorphism palette
 *  Every colour used anywhere in the app is declared here. Nothing is
 *  hard-coded at call sites so the whole aesthetic can be re-tuned centrally.
 * ---------------------------------------------------------------------- */

/** Base surface — light lavender-grey. The "clay" everything is carved from. */
val ClayBase = Color(0xFFF0EEF6)

/** A hair lighter than the base — used for raised card faces. */
val ClaySurface = Color(0xFFF7F5FB)

/** Pure card white for paper cards. */
val ClayCardWhite = Color(0xFFFDFCFF)

/** Slightly darker recess tone used inside inset wells. */
val ClayRecess = Color(0xFFE7E4F0)

/* --- Brand ------------------------------------------------------------- */
val ScholrPurple = Color(0xFF6366F1)
val ScholrPurpleLight = Color(0xFF818CF8)
val ScholrPurpleDeep = Color(0xFF4F46E5)

val AcademicGreen = Color(0xFF10B981)
val AcademicGreenLight = Color(0xFF34D399)
val AcademicGreenDeep = Color(0xFF059669)

/* --- Text -------------------------------------------------------------- */
val TextHeading = Color(0xFF2E2A3B)
val TextBody = Color(0xFF7A748C)
val TextOnClay = Color(0xFFFFFFFF)

/* --- The shadow system (the entire "clay" effect lives on these two) ---- */
/** #A39BBE @ 45% — the extruded outer dark shadow, offset +8dp. */
val ClayShadowDark = Color(0x73A39BBE)

/** #FFFFFF @ 90% — the extruded outer light shadow, offset -6dp. */
val ClayShadowLight = Color(0xE6FFFFFF)

/** Soft inner shadow for search bars and pressed buttons. */
val ClayInnerShadow = Color(0x1A000000)

/** Inner highlight that closes off the bottom-right of an inset well. */
val ClayInnerHighlight = Color(0xB3FFFFFF)

/* --- Field accents used by paper cards / vertical tags ----------------- */
val FieldComputerScience = Color(0xFF6366F1)
val FieldNeuroscience = Color(0xFFEC4899)
val FieldBiology = Color(0xFF10B981)
val FieldPhysics = Color(0xFF0EA5E9)
val FieldChemistry = Color(0xFFF59E0B)
val FieldMathematics = Color(0xFF8B5CF6)
val FieldMedicine = Color(0xFFEF4444)
val FieldPsychology = Color(0xFF14B8A6)

/* --- Reusable gradients ------------------------------------------------ */

/** Purple clay fill used by primary buttons and active nav pills. */
val PurpleClayBrush = Brush.linearGradient(
    colors = listOf(ScholrPurpleLight, ScholrPurpleDeep)
)

/** Green clay fill reserved for the Home anchor in the bottom bar. */
val GreenClayBrush = Brush.linearGradient(
    colors = listOf(AcademicGreenLight, AcademicGreenDeep)
)

/** The subtle top-light gradient that makes a flat surface read as extruded. */
val SurfaceClayBrush = Brush.linearGradient(
    colors = listOf(Color(0xFFFCFBFE), Color(0xFFEDEAF4))
)

/** Recessed well fill. */
val RecessClayBrush = Brush.linearGradient(
    colors = listOf(Color(0xFFE6E3EF), Color(0xFFF4F2F9))
)
