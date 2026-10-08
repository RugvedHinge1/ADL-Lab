package com.scholr.app.ui.illustration

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.scholr.app.ui.theme.AcademicGreenLight
import com.scholr.app.ui.theme.ClayBase
import com.scholr.app.ui.theme.FieldChemistry
import com.scholr.app.ui.theme.FieldNeuroscience
import com.scholr.app.ui.theme.FieldPhysics
import com.scholr.app.ui.theme.ScholrPurple
import com.scholr.app.ui.theme.ScholrPurpleLight
import kotlin.math.cos
import kotlin.math.sin

/* ========================================================================
 *  KINETIC PATTERN BACKGROUND  (Sign In / Sign Up)
 *
 *  Soft 3D shapes — stars, pens, spheres, squircles — drift along slow
 *  Lissajous paths and rotate independently behind the auth form. One
 *  InfiniteTransition drives a single normalised clock; every shape reads it
 *  at its own phase and speed, so nothing ever visibly loops in lockstep.
 * ====================================================================== */

private enum class KineticShape { Sphere, Star, Pen, Squircle, Ring }

private data class KineticSprite(
    val shape: KineticShape,
    val color: Color,
    /** Home position in fractional screen coordinates. */
    val x: Float,
    val y: Float,
    /** Size as a fraction of the smaller screen dimension. */
    val scale: Float,
    /** Drift radius, again fractional. */
    val driftX: Float,
    val driftY: Float,
    val phase: Float,
    val speed: Float,
    val spin: Float,
    val alpha: Float
)

private val Sprites = listOf(
    KineticSprite(KineticShape.Star, ScholrPurpleLight, 0.12f, 0.13f, 0.075f, 0.05f, 0.04f, 0.00f, 1.0f, 1f, 0.85f),
    KineticSprite(KineticShape.Sphere, FieldNeuroscience, 0.86f, 0.10f, 0.058f, 0.04f, 0.05f, 0.35f, 0.8f, 0f, 0.70f),
    KineticSprite(KineticShape.Pen, ScholrPurple, 0.80f, 0.30f, 0.130f, 0.03f, 0.05f, 0.62f, 0.6f, -1f, 0.65f),
    KineticSprite(KineticShape.Squircle, AcademicGreenLight, 0.16f, 0.34f, 0.070f, 0.05f, 0.03f, 0.18f, 0.9f, 1f, 0.60f),
    KineticSprite(KineticShape.Ring, FieldPhysics, 0.90f, 0.55f, 0.070f, 0.04f, 0.06f, 0.77f, 0.7f, 1f, 0.55f),
    KineticSprite(KineticShape.Sphere, FieldChemistry, 0.09f, 0.63f, 0.045f, 0.05f, 0.05f, 0.44f, 1.1f, 0f, 0.60f),
    KineticSprite(KineticShape.Star, AcademicGreenLight, 0.22f, 0.86f, 0.055f, 0.04f, 0.04f, 0.90f, 0.85f, -1f, 0.55f),
    KineticSprite(KineticShape.Squircle, ScholrPurpleLight, 0.78f, 0.88f, 0.085f, 0.05f, 0.04f, 0.05f, 0.75f, 1f, 0.50f),
    KineticSprite(KineticShape.Sphere, ScholrPurple, 0.50f, 0.94f, 0.038f, 0.06f, 0.03f, 0.55f, 1.2f, 0f, 0.45f),
    KineticSprite(KineticShape.Ring, FieldNeuroscience, 0.46f, 0.06f, 0.048f, 0.05f, 0.03f, 0.28f, 0.95f, -1f, 0.45f)
)

/**
 * Full-bleed animated auth backdrop. Put your form inside the [content]
 * lambda — it renders above the drifting shapes.
 */
@Composable
fun KineticBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "kinetic")
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "kineticClock"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200), repeatMode = RepeatMode.Reverse
        ),
        label = "kineticPulse"
    )

    Box(modifier = modifier.background(ClayBase)) {

        Canvas(Modifier.fillMaxSize()) {
            val s = size.minDimension

            // Two lazy colour washes so the field is never flat grey.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ScholrPurpleLight.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.12f),
                    radius = s * 0.85f
                ),
                radius = s * 0.85f,
                center = Offset(size.width * 0.15f, size.height * 0.12f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AcademicGreenLight.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.92f, size.height * 0.82f),
                    radius = s * 0.75f
                ),
                radius = s * 0.75f,
                center = Offset(size.width * 0.92f, size.height * 0.82f)
            )

            Sprites.forEach { sprite ->
                val t = (clock * sprite.speed + sprite.phase) * 2f * Math.PI.toFloat()
                // Lissajous drift: different frequencies on each axis means the
                // shape traces a slow open figure rather than a circle.
                val cx = size.width * (sprite.x + sprite.driftX * sin(t))
                val cy = size.height * (sprite.y + sprite.driftY * cos(t * 0.73f))
                val r = s * sprite.scale * pulse
                val rot = (clock * 360f * sprite.spin) + sprite.phase * 180f
                val c = sprite.color.copy(alpha = sprite.alpha)

                when (sprite.shape) {
                    KineticShape.Sphere ->
                        claySphere(Offset(cx, cy), r, c)

                    KineticShape.Star ->
                        clayStar(Offset(cx, cy), r, c, rot)

                    KineticShape.Pen ->
                        clayPen(Offset(cx, cy), r * 2f, c, FieldChemistry, rot * 0.4f + 25f)

                    KineticShape.Squircle ->
                        claySquircle(
                            topLeft = Offset(cx - r, cy - r),
                            size = Size(r * 2f, r * 2f),
                            corner = r * 0.55f,
                            base = c,
                            rotationDeg = rot * 0.5f
                        )

                    KineticShape.Ring -> {
                        claySphere(Offset(cx, cy), r, c)
                        // punch the hole with the base colour so it reads as a
                        // torus without needing a blend mode
                        drawCircle(ClayBase, radius = r * 0.46f, center = Offset(cx, cy))
                        drawCircle(
                            color = Color.White.copy(alpha = 0.35f),
                            radius = r * 0.46f,
                            center = Offset(cx - r * 0.05f, cy - r * 0.06f)
                        )
                    }
                }
            }
        }

        content()
    }
}
