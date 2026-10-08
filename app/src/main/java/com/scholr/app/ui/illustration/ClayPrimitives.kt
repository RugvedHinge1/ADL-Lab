package com.scholr.app.ui.illustration

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate

/* ========================================================================
 *  CLAY PRIMITIVES
 *
 *  Rather than shipping PNG illustrations, every 3D shape in Scholr is drawn
 *  procedurally. Three ingredients give the claymorphic look on a Canvas:
 *
 *    1. an offset contact shadow underneath the form
 *    2. a radial gradient lit from the upper-left
 *    3. a small, tight specular dot near the light
 *
 *  Composing those three on spheres and squircles is enough to build the
 *  brain / folder / microscope and every floating kinetic shape.
 * ====================================================================== */

internal val ClayContactShadow = Color(0x33635B85)

/** A lit clay ball. The workhorse — brains, bubbles, blobs are all this. */
fun DrawScope.claySphere(
    center: Offset,
    radius: Float,
    base: Color,
    highlight: Color = Color.White,
    shadow: Boolean = true
) {
    if (radius <= 0f) return
    if (shadow) {
        drawCircle(
            color = ClayContactShadow,
            radius = radius * 1.02f,
            center = center + Offset(radius * 0.12f, radius * 0.16f)
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(highlight, base),
            center = center - Offset(radius * 0.36f, radius * 0.42f),
            radius = radius * 1.75f
        ),
        radius = radius,
        center = center
    )
    // Specular kiss.
    drawCircle(
        color = Color.White.copy(alpha = 0.55f),
        radius = radius * 0.17f,
        center = center - Offset(radius * 0.42f, radius * 0.46f)
    )
}

/** A lit clay squircle — cards, folders, microscope parts, floating tiles. */
fun DrawScope.claySquircle(
    topLeft: Offset,
    size: Size,
    corner: Float,
    base: Color,
    highlight: Color = Color.White,
    rotationDeg: Float = 0f,
    shadow: Boolean = true
) {
    if (size.width <= 0f || size.height <= 0f) return
    val pivot = Offset(topLeft.x + size.width / 2f, topLeft.y + size.height / 2f)
    rotate(rotationDeg, pivot) {
        if (shadow) {
            drawRoundRect(
                color = ClayContactShadow,
                topLeft = topLeft + Offset(size.minDimension * 0.07f, size.minDimension * 0.10f),
                size = size,
                cornerRadius = CornerRadius(corner)
            )
        }
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(highlight, base),
                start = topLeft,
                end = Offset(topLeft.x + size.width, topLeft.y + size.height)
            ),
            topLeft = topLeft,
            size = size,
            cornerRadius = CornerRadius(corner)
        )
        // Rim light along the top edge.
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.45f), Color.Transparent),
                startY = topLeft.y,
                endY = topLeft.y + size.height * 0.45f
            ),
            topLeft = topLeft,
            size = size,
            cornerRadius = CornerRadius(corner),
            style = Fill
        )
    }
}

/** A soft extruded stroke — brain folds, microscope arm, chart lines. */
fun DrawScope.clayStroke(
    path: Path,
    color: Color,
    width: Float,
    shadow: Boolean = true
) {
    if (shadow) {
        translate(width * 0.22f, width * 0.30f) {
            drawPath(
                path = path,
                color = ClayContactShadow,
                style = Stroke(width = width, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = width, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    )
    drawPath(
        path = path,
        color = Color.White.copy(alpha = 0.35f),
        style = Stroke(width = width * 0.34f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    )
}

/** Five-pointed clay star used in the kinetic auth background. */
fun DrawScope.clayStar(
    center: Offset,
    radius: Float,
    base: Color,
    rotationDeg: Float = 0f
) {
    val path = Path()
    val inner = radius * 0.44f
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) radius else inner
        val a = Math.toRadians((i * 36.0) - 90.0)
        val x = center.x + (r * Math.cos(a)).toFloat()
        val y = center.y + (r * Math.sin(a)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()

    rotate(rotationDeg, center) {
        translate(radius * 0.10f, radius * 0.14f) {
            drawPath(path, ClayContactShadow)
        }
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.95f), base),
                start = Offset(center.x - radius, center.y - radius),
                end = Offset(center.x + radius, center.y + radius)
            )
        )
    }
}

/** A rounded, slightly tapered "pen" shape for the kinetic background. */
fun DrawScope.clayPen(
    center: Offset,
    length: Float,
    base: Color,
    tip: Color,
    rotationDeg: Float = 0f
) {
    val w = length * 0.26f
    rotate(rotationDeg, center) {
        // barrel
        claySquircle(
            topLeft = Offset(center.x - w / 2f, center.y - length / 2f),
            size = Size(w, length * 0.76f),
            corner = w / 2f,
            base = base
        )
        // nib
        val nib = Path().apply {
            moveTo(center.x - w / 2f, center.y + length * 0.26f)
            lineTo(center.x + w / 2f, center.y + length * 0.26f)
            lineTo(center.x, center.y + length / 2f)
            close()
        }
        drawPath(nib, tip)
    }
}
