package com.scholr.app.ui.clay

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.scholr.app.ui.theme.ClayRadius

/**
 * Every clay corner in the app goes through here so the radius used to clip a
 * surface can never drift away from the radius used to draw its shadow.
 */
fun ClayShape(radius: Dp): RoundedCornerShape = RoundedCornerShape(radius)

val CardShape = RoundedCornerShape(ClayRadius.Card)
val SheetShape = RoundedCornerShape(
    topStart = ClayRadius.Sheet,
    topEnd = ClayRadius.Sheet,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)
val ButtonShape = RoundedCornerShape(ClayRadius.Button)
val PillShape = RoundedCornerShape(ClayRadius.Pill)
val TileShape = RoundedCornerShape(ClayRadius.Tile)
val IconShape = RoundedCornerShape(ClayRadius.Icon)
