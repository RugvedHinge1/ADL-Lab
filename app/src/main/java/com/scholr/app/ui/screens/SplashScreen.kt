package com.scholr.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.scholr.app.ui.clay.claySurface
import com.scholr.app.ui.components.ScholrWordmark
import com.scholr.app.ui.illustration.TestTubeLoader
import com.scholr.app.ui.illustration.claySphere
import com.scholr.app.ui.theme.AcademicGreen
import com.scholr.app.ui.theme.AcademicGreenLight
import com.scholr.app.ui.theme.ClayBase
import com.scholr.app.ui.theme.FieldChemistry
import com.scholr.app.ui.theme.FieldNeuroscience
import com.scholr.app.ui.theme.PurpleClayBrush
import com.scholr.app.ui.theme.ScholrPurpleLight
import com.scholr.app.ui.theme.TextBody
import kotlinx.coroutines.delay

/**
 * Splash. A single scripted 2.4s beat: the mark springs in, the test-tube
 * loader fills left to right, and the status line steps through three
 * messages so the wait reads as work being done rather than dead time.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {

    val progress = remember { Animatable(0f) }
    val markScale = remember { Animatable(0.6f) }
    var statusIndex by remember { mutableStateOf(0) }

    val statuses = listOf(
        "Warming up the lab…",
        "Indexing 4.2M papers…",
        "Tuning your recommendations…"
    )

    LaunchedEffect(Unit) {
        markScale.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(Unit) {
        progress.animateTo(0.34f, tween(700, easing = FastOutSlowInEasing))
        statusIndex = 1
        progress.animateTo(0.72f, tween(800, easing = FastOutSlowInEasing))
        statusIndex = 2
        progress.animateTo(1f, tween(760, easing = FastOutSlowInEasing))
        delay(320)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ClayBase),
        contentAlignment = Alignment.Center
    ) {
        // Ambient clay confetti drifting behind the mark.
        Canvas(Modifier.fillMaxSize()) {
            val s = size.minDimension
            claySphere(Offset(size.width * 0.14f, size.height * 0.18f), s * 0.055f, ScholrPurpleLight.copy(alpha = 0.6f))
            claySphere(Offset(size.width * 0.86f, size.height * 0.24f), s * 0.035f, FieldNeuroscience.copy(alpha = 0.55f))
            claySphere(Offset(size.width * 0.80f, size.height * 0.78f), s * 0.048f, AcademicGreenLight.copy(alpha = 0.5f))
            claySphere(Offset(size.width * 0.18f, size.height * 0.82f), s * 0.030f, FieldChemistry.copy(alpha = 0.5f))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ScholrMark(
                modifier = Modifier.graphicsLayer {
                    scaleX = markScale.value
                    scaleY = markScale.value
                    alpha = markScale.value.coerceIn(0f, 1f)
                }
            )

            Spacer(Modifier.height(26.dp))

            ScholrWordmark()

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Academic discovery, made tactile",
                style = MaterialTheme.typography.bodyMedium,
                color = TextBody,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(54.dp))

            TestTubeLoader(progress = progress.value)

            Spacer(Modifier.height(16.dp))

            Text(
                text = statuses[statusIndex],
                style = MaterialTheme.typography.bodySmall,
                color = TextBody,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** The big clay app mark: an extruded purple squircle with a drawn "S". */
@Composable
fun ScholrMark(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 108.dp) {
    Box(
        modifier = modifier
            .size(size)
            .claySurface(
                cornerRadius = size * 0.30f,
                brush = PurpleClayBrush,
                darkOffset = 14.dp,
                lightOffset = (-10).dp,
                blur = 24.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            val s = Path().apply {
                moveTo(w * 0.68f, h * 0.34f)
                cubicTo(
                    w * 0.60f, h * 0.22f,
                    w * 0.30f, h * 0.24f,
                    w * 0.30f, h * 0.42f
                )
                cubicTo(
                    w * 0.30f, h * 0.58f,
                    w * 0.70f, h * 0.50f,
                    w * 0.70f, h * 0.66f
                )
                cubicTo(
                    w * 0.70f, h * 0.82f,
                    w * 0.40f, h * 0.82f,
                    w * 0.32f, h * 0.70f
                )
            }

            // shadow pass then the bright stroke — instant depth
            drawPath(
                path = s,
                color = Color(0x33241E3D),
                style = Stroke(width = w * 0.115f, cap = StrokeCap.Round)
            )
            drawPath(
                path = s,
                color = Color.White,
                style = Stroke(width = w * 0.10f, cap = StrokeCap.Round)
            )

            // green spark, top-right
            claySphere(
                center = Offset(w * 0.79f, h * 0.23f),
                radius = w * 0.075f,
                base = AcademicGreen
            )
        }
    }
}
