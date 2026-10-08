package com.scholr.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.scholr.app.ui.clay.ClayButton
import com.scholr.app.ui.clay.ClayShape
import com.scholr.app.ui.clay.clayExtruded
import com.scholr.app.ui.illustration.ClayBrainIllustration
import com.scholr.app.ui.illustration.ClayFolderIllustration
import com.scholr.app.ui.illustration.ClayMicroscopeIllustration
import com.scholr.app.ui.theme.ClayBase
import com.scholr.app.ui.theme.ClayRadius
import com.scholr.app.ui.theme.GreenClayBrush
import com.scholr.app.ui.theme.PurpleClayBrush
import com.scholr.app.ui.theme.ScholrPurple
import com.scholr.app.ui.theme.SurfaceClayBrush
import com.scholr.app.ui.theme.TextBody
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private data class Slide(
    val headline: String,
    val body: String,
    val illustration: @Composable () -> Unit
)

private val slides = listOf(
    Slide(
        headline = "Research that finds you",
        body = "Scholr learns what you actually read — not what you clicked once — " +
            "and puts the next paper in front of you before you go looking.",
        illustration = { ClayBrainIllustration() }
    ),
    Slide(
        headline = "One library, everywhere",
        body = "Save, tag and group papers into collections. Your reading list " +
            "follows you from the lab bench to the bus home.",
        illustration = { ClayFolderIllustration() }
    ),
    Slide(
        headline = "Understand it in minutes",
        body = "Ask the AI analyst anything about a paper — method, limitations, " +
            "how it lands against what you've already saved.",
        illustration = { ClayMicroscopeIllustration() }
    )
)

/**
 * Three-slide onboarding carousel. The illustrations live in a swipeable
 * pager with a parallax/scale falloff, while the copy underneath swaps via
 * AnimatedContent so the two layers move at different rates.
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == slides.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClayBase)
            .systemBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        // --- skip ------------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isLast) "" else "Skip",
                style = MaterialTheme.typography.labelMedium,
                color = TextBody,
                modifier = Modifier
                    .clickable(enabled = !isLast) { onFinished() }
                    .padding(8.dp)
            )
        }

        // --- illustrations --------------------------------------------
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            pageSpacing = 8.dp
        ) { page ->
            val offset = (
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                ).absoluteValue

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val scale = 1f - (offset.coerceIn(0f, 1f) * 0.22f)
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - (offset.coerceIn(0f, 1f) * 0.6f)
                        rotationY = offset.coerceIn(0f, 1f) * -18f
                    },
                contentAlignment = Alignment.Center
            ) {
                slides[page].illustration()
            }
        }

        // --- copy ------------------------------------------------------
        AnimatedContent(
            targetState = pagerState.currentPage,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(420)) { w -> if (forward) w / 2 else -w / 2 } +
                    fadeIn(tween(340))) togetherWith
                    (slideOutHorizontally(tween(420)) { w -> if (forward) -w / 2 else w / 2 } +
                        fadeOut(tween(200)))
            },
            label = "onboardingCopy"
        ) { page ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = slides[page].headline,
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = slides[page].body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextBody,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // --- clay page indicator --------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            slides.indices.forEach { index ->
                val selected = index == pagerState.currentPage
                val width by animateDpAsState(
                    targetValue = if (selected) 30.dp else 10.dp,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
                    label = "dotWidth"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(width)
                        .height(10.dp)
                        .clayExtruded(
                            cornerRadius = 5.dp,
                            darkOffset = 3.dp,
                            lightOffset = (-2).dp,
                            blur = 6.dp
                        )
                        .clip(ClayShape(5.dp))
                        .background(if (selected) PurpleClayBrush else SurfaceClayBrush)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- primary action -------------------------------------------
        ClayButton(
            text = if (isLast) "Get started" else "Next",
            onClick = {
                if (isLast) onFinished()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            trailingIcon = if (isLast) null else Icons.Rounded.ArrowForward,
            brush = if (isLast) GreenClayBrush else PurpleClayBrush,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(28.dp))
    }
}
