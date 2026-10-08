package com.scholr.app.ui.clay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.scholr.app.ui.theme.ClayInnerHighlight
import com.scholr.app.ui.theme.ClayInnerShadow
import com.scholr.app.ui.theme.ClayShadowDark
import com.scholr.app.ui.theme.ClayShadowLight
import com.scholr.app.ui.theme.RecessClayBrush
import com.scholr.app.ui.theme.SurfaceClayBrush

/* ========================================================================
 *  SCHOLR CLAYMORPHIC MODIFIER ENGINE
 *
 *  Compose's built-in Modifier.shadow() can only cast one shadow, always
 *  down-and-away from a fixed light source. Clay needs *two* shadows with
 *  independent offsets — a dark one pushed down-right and a white one pulled
 *  up-left — so we drop to the framework Canvas and layer them ourselves.
 *
 *  Everything draws inside drawBehind {}, which is *not* clipped to the
 *  composable's bounds, so the shadows spill correctly outside the layout
 *  box. That is exactly why lists must carry contentPadding — otherwise the
 *  parent clips the spill.
 * ====================================================================== */

/**
 * Two ways to rasterise the blur.
 *
 * [Native] uses `Paint.setShadowLayer` — one draw call, perfectly smooth
 * falloff. This is the default and is what you want on virtually all
 * hardware.
 *
 * [Stacked] approximates the blur by layering concentric translucent rounded
 * rects. Slightly heavier and a touch less soft, but it goes through the
 * plain hardware path and therefore *cannot* be dropped by a renderer that
 * refuses shadow layers on shapes. If you ever see flat, shadowless cards on
 * some exotic device or an old emulator image, flip the global switch below
 * and everything in the app changes at once.
 */
enum class ClayShadowRenderer { Native, Stacked }

object ClayConfig {
    /** Global escape hatch — see [ClayShadowRenderer]. */
    var renderer: ClayShadowRenderer = ClayShadowRenderer.Native

    /** Master depth multiplier. Handy for demoing the design language. */
    var depthScale: Float = 1f
}

/* ------------------------------------------------------------------------
 *  1. EXTRUDED — the outer, "pushed out of the surface" clay effect
 * ---------------------------------------------------------------------- */

/**
 * Casts the dual clay shadow behind the composable.
 *
 * @param cornerRadius must match the radius of whatever background/clip you
 *        put on the same element, or the shadow will peek out at the corners.
 */
fun Modifier.clayExtruded(
    cornerRadius: Dp = 28.dp,
    darkColor: Color = ClayShadowDark,
    lightColor: Color = ClayShadowLight,
    darkOffset: Dp = 8.dp,
    lightOffset: Dp = (-6).dp,
    blur: Dp = 16.dp
): Modifier = this.drawBehind {
    val scale = ClayConfig.depthScale
    drawClayExtruded(
        radiusPx = cornerRadius.toPx(),
        darkColor = darkColor,
        lightColor = lightColor,
        darkOffsetPx = darkOffset.toPx() * scale,
        lightOffsetPx = lightOffset.toPx() * scale,
        blurPx = blur.toPx() * scale
    )
}

internal fun DrawScope.drawClayExtruded(
    radiusPx: Float,
    darkColor: Color,
    lightColor: Color,
    darkOffsetPx: Float,
    lightOffsetPx: Float,
    blurPx: Float
) {
    if (size.width <= 0f || size.height <= 0f) return

    when (ClayConfig.renderer) {
        ClayShadowRenderer.Native -> drawIntoCanvas { canvas ->
            val paint = Paint()
            val frameworkPaint = paint.asFrameworkPaint()
            frameworkPaint.isAntiAlias = true
            // A fully transparent fill means only the shadow layer is painted.
            frameworkPaint.color = android.graphics.Color.TRANSPARENT

            // Dark shadow, offset down-right.
            frameworkPaint.setShadowLayer(
                blurPx.coerceAtLeast(0.1f),
                darkOffsetPx,
                darkOffsetPx,
                darkColor.toArgb()
            )
            canvas.drawRoundRect(
                0f, 0f, size.width, size.height, radiusPx, radiusPx, paint
            )

            // Light shadow, pulled up-left. This is what sells the 3D.
            frameworkPaint.setShadowLayer(
                blurPx.coerceAtLeast(0.1f),
                lightOffsetPx,
                lightOffsetPx,
                lightColor.toArgb()
            )
            canvas.drawRoundRect(
                0f, 0f, size.width, size.height, radiusPx, radiusPx, paint
            )

            frameworkPaint.clearShadowLayer()
        }

        ClayShadowRenderer.Stacked -> {
            stackedShadow(radiusPx, lightColor, lightOffsetPx, blurPx)
            stackedShadow(radiusPx, darkColor, darkOffsetPx, blurPx)
        }
    }
}

/** Concentric-ring blur approximation used by [ClayShadowRenderer.Stacked]. */
private fun DrawScope.stackedShadow(
    radiusPx: Float,
    color: Color,
    offsetPx: Float,
    blurPx: Float
) {
    val steps = 7
    for (i in steps downTo 1) {
        val t = i / steps.toFloat()
        val grow = blurPx * t
        drawRoundRect(
            color = color.copy(alpha = color.alpha * 0.14f),
            topLeft = Offset(offsetPx - grow, offsetPx - grow),
            size = Size(size.width + grow * 2f, size.height + grow * 2f),
            cornerRadius = CornerRadius(radiusPx + grow)
        )
    }
}

/* ------------------------------------------------------------------------
 *  2. INSET — the recessed well used by search bars and pressed buttons
 * ---------------------------------------------------------------------- */

/**
 * Paints a soft inner shadow *on top of* the element's background but behind
 * its content, so text stays crisp.
 */
fun Modifier.clayInset(
    cornerRadius: Dp = 24.dp,
    shadowColor: Color = ClayInnerShadow,
    highlightColor: Color = ClayInnerHighlight,
    offset: Dp = 4.dp,
    blur: Dp = 10.dp
): Modifier = this.drawWithContent {
    drawContent()
    drawClayInset(
        radiusPx = cornerRadius.toPx(),
        shadowColor = shadowColor,
        highlightColor = highlightColor,
        offsetPx = offset.toPx(),
        blurPx = blur.toPx()
    )
}

internal fun DrawScope.drawClayInset(
    radiusPx: Float,
    shadowColor: Color,
    highlightColor: Color,
    offsetPx: Float,
    blurPx: Float
) {
    if (size.width <= 0f || size.height <= 0f) return

    val outline = Path().apply {
        addRoundRect(
            RoundRect(
                Rect(Offset.Zero, size),
                CornerRadius(radiusPx, radiusPx)
            )
        )
    }

    when (ClayConfig.renderer) {
        ClayShadowRenderer.Native -> drawIntoCanvas { canvas ->
            canvas.save()
            // Clip to the well, then cast the shadow of its own outline
            // inward — the classic inner-shadow trick.
            canvas.clipPath(outline)

            val paint = Paint()
            val fw = paint.asFrameworkPaint()
            fw.isAntiAlias = true
            fw.style = android.graphics.Paint.Style.STROKE
            fw.strokeWidth = blurPx.coerceAtLeast(1f) * 2f
            fw.color = android.graphics.Color.TRANSPARENT

            fw.setShadowLayer(
                blurPx.coerceAtLeast(0.1f), offsetPx, offsetPx, shadowColor.toArgb()
            )
            canvas.nativeCanvas.drawPath(outline.asAndroidPath(), fw)

            fw.setShadowLayer(
                blurPx.coerceAtLeast(0.1f), -offsetPx, -offsetPx, highlightColor.toArgb()
            )
            canvas.nativeCanvas.drawPath(outline.asAndroidPath(), fw)

            fw.clearShadowLayer()
            canvas.restore()
        }

        ClayShadowRenderer.Stacked -> {
            // Stroke the outline a few times with growing width, clipped in.
            val steps = 5
            for (i in steps downTo 1) {
                val t = i / steps.toFloat()
                drawRoundRect(
                    color = shadowColor.copy(alpha = shadowColor.alpha * 0.30f),
                    topLeft = Offset(offsetPx, offsetPx),
                    size = size,
                    cornerRadius = CornerRadius(radiusPx),
                    style = Stroke(width = blurPx * t)
                )
            }
        }
    }

    // A whisper of a rim so the well reads even against a busy background.
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                shadowColor.copy(alpha = 0.10f),
                Color.Transparent,
                highlightColor.copy(alpha = 0.30f)
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        ),
        cornerRadius = CornerRadius(radiusPx),
        style = Stroke(width = 1.2f)
    )
}

/* ------------------------------------------------------------------------
 *  3. CONVENIENCE COMPOSITES
 * ---------------------------------------------------------------------- */

/**
 * The everyday raised clay surface: dual shadow + clipped gradient face.
 * Use this for cards, header bars, nav bars, tiles.
 */
fun Modifier.claySurface(
    cornerRadius: Dp = 28.dp,
    brush: Brush = SurfaceClayBrush,
    darkOffset: Dp = 8.dp,
    lightOffset: Dp = (-6).dp,
    blur: Dp = 16.dp,
    darkColor: Color = ClayShadowDark,
    lightColor: Color = ClayShadowLight
): Modifier = this
    .clayExtruded(cornerRadius, darkColor, lightColor, darkOffset, lightOffset, blur)
    .clip(ClayShape(cornerRadius))
    .background(brush)

/** Solid-colour flavour of [claySurface]. */
fun Modifier.claySurfaceSolid(
    cornerRadius: Dp = 28.dp,
    color: Color,
    darkOffset: Dp = 8.dp,
    lightOffset: Dp = (-6).dp,
    blur: Dp = 16.dp
): Modifier = this
    .clayExtruded(cornerRadius, ClayShadowDark, ClayShadowLight, darkOffset, lightOffset, blur)
    .clip(ClayShape(cornerRadius))
    .background(color)

/**
 * A recessed well — clipped, filled with the recess gradient, inner shadow on
 * top. Search fields, segmented controls, the bookmark button, chat input.
 */
fun Modifier.clayWell(
    cornerRadius: Dp = 24.dp,
    brush: Brush = RecessClayBrush,
    offset: Dp = 4.dp,
    blur: Dp = 10.dp
): Modifier = this
    .clip(ClayShape(cornerRadius))
    .background(brush)
    .clayInset(cornerRadius = cornerRadius, offset = offset, blur = blur)

/** Adds uniform inner padding after a clay treatment. Purely for readability. */
fun Modifier.clayPadding(all: Dp): Modifier = this.padding(all)
