package com.scholr.app.ui.illustration

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.scholr.app.ui.clay.clayWell
import com.scholr.app.ui.theme.AcademicGreenLight
import com.scholr.app.ui.theme.ScholrPurple
import com.scholr.app.ui.theme.ScholrPurpleDeep
import com.scholr.app.ui.theme.ScholrPurpleLight
import kotlin.math.sin

/* ========================================================================
 *  THE TEST-TUBE LOADER
 *
 *  A horizontal test tube laid on its side, recessed into the clay. Liquid
 *  fills left to right behind a rippling meniscus, bubbles rise and pop, and
 *  a glass specular streak runs along the top. This is the very first thing
 *  a user sees, so it is doing a lot of the brand work on its own.
 * ====================================================================== */

@Composable
fun TestTubeLoader(
    progress: Float,
    modifier: Modifier = Modifier,
    liquidStart: Color = ScholrPurpleLight,
    liquidEnd: Color = ScholrPurpleDeep,
    accent: Color = AcademicGreenLight
) {
    val transition = rememberInfiniteTransition(label = "testTube")

    // Drives the meniscus ripple and the bubble column.
    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )
    val bubbleClock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubbles"
    )
    val shimmer by transition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val p = progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .clayWell(cornerRadius = 17.dp, offset = 3.dp, blur = 9.dp)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val h = size.height
            val w = size.width
            val radius = h / 2f
            val inset = h * 0.13f

            val tube = Rect(inset, inset, w - inset, h - inset)
            val tubePath = Path().apply {
                addRoundRect(
                    RoundRect(tube, CornerRadius(tube.height / 2f, tube.height / 2f))
                )
            }

            clipPath(tubePath) {
                // --- liquid body -------------------------------------
                val fillEdge = tube.left + tube.width * p
                if (p > 0f) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(liquidStart, liquidEnd),
                            startX = tube.left,
                            endX = fillEdge.coerceAtLeast(tube.left + 1f)
                        ),
                        topLeft = Offset(tube.left, tube.top),
                        size = Size((fillEdge - tube.left).coerceAtLeast(0f), tube.height)
                    )

                    // --- rippling meniscus at the leading edge --------
                    val meniscus = Path()
                    val amp = tube.height * 0.16f
                    meniscus.moveTo(fillEdge, tube.top)
                    var y = tube.top
                    while (y <= tube.bottom) {
                        val phase = (y / tube.height) * 2.4f + wave * 6.283f
                        meniscus.lineTo(fillEdge + sin(phase) * amp, y)
                        y += 1.5f
                    }
                    meniscus.lineTo(fillEdge - amp * 2f, tube.bottom)
                    meniscus.lineTo(fillEdge - amp * 2f, tube.top)
                    meniscus.close()
                    drawPath(meniscus, liquidEnd.copy(alpha = 0.85f))

                    // --- bubbles drifting toward the meniscus ---------
                    val bubbles = 7
                    for (i in 0 until bubbles) {
                        val seed = i / bubbles.toFloat()
                        val travel = ((bubbleClock + seed) % 1f)
                        val bx = tube.left + tube.width * p * travel
                        val by = tube.top + tube.height *
                            (0.30f + 0.42f * sin((seed * 9f) + travel * 5f))
                        val br = tube.height * (0.06f + 0.05f * ((i % 3) / 2f)) *
                            (0.4f + 0.6f * (1f - travel))
                        drawCircle(
                            color = Color.White.copy(alpha = 0.42f * (1f - travel)),
                            radius = br,
                            center = Offset(bx, by)
                        )
                    }

                    // --- a green reagent streak for a bit of life -----
                    drawLine(
                        color = accent.copy(alpha = 0.55f),
                        start = Offset(tube.left + tube.width * 0.02f, tube.center.y),
                        end = Offset(fillEdge - amp, tube.center.y),
                        strokeWidth = tube.height * 0.07f,
                        cap = StrokeCap.Round
                    )
                }

                // --- unfilled glass ----------------------------------
                if (p < 1f) {
                    drawRect(
                        color = Color.White.copy(alpha = 0.35f),
                        topLeft = Offset(fillEdge, tube.top),
                        size = Size((tube.right - fillEdge).coerceAtLeast(0f), tube.height)
                    )
                }

                // --- travelling glass shimmer ------------------------
                val sx = tube.left + tube.width * shimmer
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.45f),
                            Color.Transparent
                        ),
                        startX = sx - tube.width * 0.12f,
                        endX = sx + tube.width * 0.12f
                    ),
                    topLeft = Offset(tube.left, tube.top),
                    size = Size(tube.width, tube.height)
                )
            }

            // --- glass rim + top specular ----------------------------
            drawPath(
                path = tubePath,
                color = Color.White.copy(alpha = 0.75f),
                style = Stroke(width = h * 0.05f)
            )
            drawLine(
                color = Color.White.copy(alpha = 0.65f),
                start = Offset(tube.left + radius * 0.6f, tube.top + tube.height * 0.24f),
                end = Offset(tube.right - radius * 0.6f, tube.top + tube.height * 0.24f),
                strokeWidth = tube.height * 0.09f,
                cap = StrokeCap.Round
            )

            // --- graduation ticks along the tube ---------------------
            for (i in 1 until 8) {
                val x = tube.left + tube.width * (i / 8f)
                drawLine(
                    color = ScholrPurple.copy(alpha = 0.22f),
                    start = Offset(x, tube.bottom - tube.height * 0.30f),
                    end = Offset(x, tube.bottom - tube.height * 0.08f),
                    strokeWidth = h * 0.035f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
