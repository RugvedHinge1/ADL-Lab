package com.scholr.app.ui.theme

import androidx.compose.ui.unit.dp

/* ------------------------------------------------------------------------
 *  Corner radii — the spec calls these out explicitly, so they live in one
 *  place rather than being sprinkled through the composables.
 * ---------------------------------------------------------------------- */
object ClayRadius {
    val Card = 28.dp
    val Sheet = 32.dp
    val Button = 24.dp
    val Pill = 20.dp
    val Icon = 18.dp
    val Tile = 26.dp
}

/* ------------------------------------------------------------------------
 *  Shadow geometry. The dark shadow is pushed +8dp down-right, the light
 *  shadow -6dp up-left. Blur is tuned per surface size.
 * ---------------------------------------------------------------------- */
object ClayDepth {
    val DarkOffset = 8.dp
    val LightOffset = (-6).dp
    val Blur = 16.dp

    // A shallower variant for small controls (chips, icon buttons).
    val DarkOffsetSmall = 5.dp
    val LightOffsetSmall = (-4).dp
    val BlurSmall = 10.dp

    // A deeper variant for "extruded further" selected tiles.
    val DarkOffsetDeep = 12.dp
    val LightOffsetDeep = (-9).dp
    val BlurDeep = 22.dp
}

object ClaySpacing {
    /** LazyColumn contentPadding so the clay shadows can breathe without clipping. */
    val ListPadding = 16.dp
    val Screen = 20.dp
    val Gutter = 16.dp
    val Tight = 8.dp
}
