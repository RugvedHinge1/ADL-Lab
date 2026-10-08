package com.scholr.app.ui.illustration

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.scholr.app.ui.theme.AcademicGreen
import com.scholr.app.ui.theme.AcademicGreenLight
import com.scholr.app.ui.theme.FieldChemistry
import com.scholr.app.ui.theme.FieldNeuroscience
import com.scholr.app.ui.theme.FieldPhysics
import com.scholr.app.ui.theme.ScholrPurple
import com.scholr.app.ui.theme.ScholrPurpleDeep
import com.scholr.app.ui.theme.ScholrPurpleLight

/* ========================================================================
 *  ONBOARDING ILLUSTRATIONS
 *
 *  Three high-fidelity 3D claymorphic scenes, drawn on Canvas so they scale
 *  crisply to any density and can be re-tinted from the palette. Each one
 *  gently bobs and breathes via an InfiniteTransition.
 * ====================================================================== */

/** Shared float/bob wrapper so all three illustrations share one motion feel. */
@Composable
private fun Floating(
    modifier: Modifier = Modifier,
    bobDp: Float = 10f,
    periodMs: Int = 3600,
    tiltDeg: Float = 3f,
    content: @Composable () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "float")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMs), repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )
    val scale by transition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween((periodMs * 1.3f).toInt()), repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )
    Box(
        modifier = modifier.graphicsLayer {
            translationY = (t - 0.5f) * 2f * bobDp * density
            rotationZ = (t - 0.5f) * 2f * tiltDeg
            scaleX = scale
            scaleY = scale
        }
    ) { content() }
}

/** Soft lavender halo behind every illustration — grounds the floating form. */
private fun DrawScope.halo(tint: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(tint.copy(alpha = 0.20f), Color.Transparent),
            center = center,
            radius = size.minDimension * 0.52f
        ),
        radius = size.minDimension * 0.52f,
        center = center
    )
}

/* --------------------------- 1. THE BRAIN ----------------------------- */

@Composable
fun ClayBrainIllustration(modifier: Modifier = Modifier) {
    Floating(modifier = modifier.size(240.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val s = size.minDimension
            halo(FieldNeuroscience)

            fun p(x: Float, y: Float) = Offset(size.width * x, size.height * y)

            // Brain stem first so lobes sit over it.
            claySquircle(
                topLeft = p(0.44f, 0.62f),
                size = Size(s * 0.13f, s * 0.20f),
                corner = s * 0.06f,
                base = FieldNeuroscience.copy(alpha = 0.85f)
            )

            // Left hemisphere — a cluster of clay balls.
            claySphere(p(0.34f, 0.40f), s * 0.155f, FieldNeuroscience)
            claySphere(p(0.28f, 0.53f), s * 0.130f, FieldNeuroscience)
            claySphere(p(0.40f, 0.57f), s * 0.125f, FieldNeuroscience)
            claySphere(p(0.42f, 0.30f), s * 0.120f, Color(0xFFF472B6))

            // Right hemisphere in the brand purple, so the two halves read
            // as "science" and "knowledge" meeting.
            claySphere(p(0.63f, 0.41f), s * 0.150f, ScholrPurple)
            claySphere(p(0.71f, 0.52f), s * 0.125f, ScholrPurpleLight)
            claySphere(p(0.58f, 0.56f), s * 0.122f, ScholrPurpleDeep)
            claySphere(p(0.57f, 0.29f), s * 0.115f, ScholrPurpleLight)

            // Central sulcus fold.
            val fold = Path().apply {
                moveTo(size.width * 0.50f, size.height * 0.20f)
                cubicTo(
                    size.width * 0.46f, size.height * 0.34f,
                    size.width * 0.54f, size.height * 0.44f,
                    size.width * 0.50f, size.height * 0.62f
                )
            }
            clayStroke(fold, Color.White.copy(alpha = 0.75f), s * 0.030f, shadow = false)

            // Synapse sparks orbiting the brain.
            claySphere(p(0.18f, 0.24f), s * 0.030f, AcademicGreenLight)
            claySphere(p(0.84f, 0.28f), s * 0.024f, FieldChemistry)
            claySphere(p(0.80f, 0.72f), s * 0.030f, FieldPhysics)
            claySphere(p(0.20f, 0.70f), s * 0.022f, ScholrPurpleLight)
        }
    }
}

/* --------------------------- 2. THE FOLDER ---------------------------- */

@Composable
fun ClayFolderIllustration(modifier: Modifier = Modifier) {
    Floating(modifier = modifier.size(240.dp), tiltDeg = 2f, periodMs = 4200) {
        Canvas(Modifier.fillMaxSize()) {
            val s = size.minDimension
            halo(ScholrPurple)

            fun p(x: Float, y: Float) = Offset(size.width * x, size.height * y)

            // Folder tab.
            claySquircle(
                topLeft = p(0.17f, 0.27f),
                size = Size(s * 0.34f, s * 0.14f),
                corner = s * 0.055f,
                base = ScholrPurpleDeep
            )
            // Back panel.
            claySquircle(
                topLeft = p(0.15f, 0.34f),
                size = Size(s * 0.70f, s * 0.40f),
                corner = s * 0.075f,
                base = ScholrPurpleDeep
            )

            // Paper sheets peeking out, fanned.
            claySquircle(
                topLeft = p(0.26f, 0.30f),
                size = Size(s * 0.42f, s * 0.34f),
                corner = s * 0.035f,
                base = Color(0xFFEFEDF8),
                rotationDeg = -7f
            )
            claySquircle(
                topLeft = p(0.32f, 0.28f),
                size = Size(s * 0.42f, s * 0.34f),
                corner = s * 0.035f,
                base = Color(0xFFFDFCFF),
                rotationDeg = 5f
            )

            // Text ruling on the top sheet.
            translate(0f, 0f) {
                val lineColor = ScholrPurple.copy(alpha = 0.30f)
                for (i in 0 until 4) {
                    val y = size.height * (0.36f + i * 0.055f)
                    drawLine(
                        color = lineColor,
                        start = Offset(size.width * 0.38f, y),
                        end = Offset(size.width * (if (i == 3) 0.58f else 0.68f), y),
                        strokeWidth = s * 0.018f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Front panel — lighter, so the folder reads as open.
            claySquircle(
                topLeft = p(0.13f, 0.46f),
                size = Size(s * 0.74f, s * 0.30f),
                corner = s * 0.075f,
                base = ScholrPurple
            )

            // Green "saved" badge.
            claySphere(p(0.79f, 0.44f), s * 0.095f, AcademicGreen)
            val check = Path().apply {
                moveTo(size.width * 0.745f, size.height * 0.442f)
                lineTo(size.width * 0.778f, size.height * 0.475f)
                lineTo(size.width * 0.838f, size.height * 0.400f)
            }
            drawPath(
                check,
                Color.White,
                style = Stroke(width = s * 0.026f, cap = StrokeCap.Round)
            )
        }
    }
}

/* ------------------------- 3. THE MICROSCOPE -------------------------- */

@Composable
fun ClayMicroscopeIllustration(modifier: Modifier = Modifier) {
    Floating(modifier = modifier.size(240.dp), tiltDeg = 2.5f, periodMs = 3900) {
        Canvas(Modifier.fillMaxSize()) {
            val s = size.minDimension
            halo(AcademicGreen)

            fun p(x: Float, y: Float) = Offset(size.width * x, size.height * y)

            // Base plate.
            claySquircle(
                topLeft = p(0.20f, 0.76f),
                size = Size(s * 0.60f, s * 0.11f),
                corner = s * 0.055f,
                base = ScholrPurpleDeep
            )

            // Curved arm.
            val arm = Path().apply {
                moveTo(size.width * 0.63f, size.height * 0.78f)
                cubicTo(
                    size.width * 0.80f, size.height * 0.66f,
                    size.width * 0.78f, size.height * 0.40f,
                    size.width * 0.63f, size.height * 0.33f
                )
            }
            clayStroke(arm, ScholrPurple, s * 0.075f)

            // Stage with a specimen slide.
            claySquircle(
                topLeft = p(0.24f, 0.60f),
                size = Size(s * 0.40f, s * 0.075f),
                corner = s * 0.035f,
                base = ScholrPurpleLight
            )
            claySquircle(
                topLeft = p(0.28f, 0.575f),
                size = Size(s * 0.22f, s * 0.035f),
                corner = s * 0.017f,
                base = Color(0xFFB9F3DE),
                shadow = false
            )

            // Body tube, tilted.
            claySquircle(
                topLeft = p(0.40f, 0.24f),
                size = Size(s * 0.155f, s * 0.34f),
                corner = s * 0.070f,
                base = AcademicGreen,
                rotationDeg = 14f
            )
            // Eyepiece.
            claySquircle(
                topLeft = p(0.395f, 0.135f),
                size = Size(s * 0.19f, s * 0.10f),
                corner = s * 0.048f,
                base = AcademicGreenLight,
                rotationDeg = 14f
            )
            // Objective lens.
            claySphere(p(0.545f, 0.545f), s * 0.058f, Color(0xFFFDFCFF))
            drawCircle(
                color = ScholrPurpleDeep.copy(alpha = 0.55f),
                radius = s * 0.026f,
                center = p(0.545f, 0.545f)
            )

            // Focus knob.
            claySphere(p(0.70f, 0.585f), s * 0.055f, FieldChemistry)

            // Light beam glints coming off the lens.
            for (i in 0 until 3) {
                val a = -0.55f + i * 0.32f
                drawLine(
                    color = AcademicGreenLight.copy(alpha = 0.55f - i * 0.10f),
                    start = p(0.20f - i * 0.02f, 0.50f + a * 0.10f),
                    end = p(0.30f - i * 0.02f, 0.50f + a * 0.10f),
                    strokeWidth = s * 0.016f,
                    cap = StrokeCap.Round
                )
            }

            // Discovery sparks.
            claySphere(p(0.17f, 0.30f), s * 0.028f, ScholrPurpleLight)
            claySphere(p(0.83f, 0.20f), s * 0.022f, FieldNeuroscience)
        }
    }
}
