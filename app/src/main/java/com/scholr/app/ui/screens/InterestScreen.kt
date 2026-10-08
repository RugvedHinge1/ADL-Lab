package com.scholr.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scholr.app.data.SampleData
import com.scholr.app.data.model.Interest
import com.scholr.app.ui.clay.ClayButton
import com.scholr.app.ui.clay.ClayShape
import com.scholr.app.ui.clay.clayExtruded
import com.scholr.app.ui.theme.ClayBase
import com.scholr.app.ui.theme.ClayDepth
import com.scholr.app.ui.theme.ClayRadius
import com.scholr.app.ui.theme.PurpleClayBrush
import com.scholr.app.ui.theme.ScholrPurple
import com.scholr.app.ui.theme.SurfaceClayBrush
import com.scholr.app.ui.theme.TextBody
import com.scholr.app.ui.theme.TextHeading
import com.scholr.app.ui.theme.TextOnClay

/**
 * Interest selection. Tiles are the clearest demonstration of the design
 * language in the whole app: picking one literally pushes it further out of
 * the surface — the shadow offsets and blur grow, and the face flips to
 * purple clay.
 */
@Composable
fun InterestScreen(
    selected: List<String>,
    onToggle: (String) -> Unit,
    canContinue: Boolean,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClayBase)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(24.dp))
            Text("What are you into?", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Pick at least three. You can change these any time from " +
                    "your profile — the feed adapts as you read.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextBody
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${selected.size} selected",
                style = MaterialTheme.typography.labelMedium,
                color = if (canContinue) ScholrPurple else TextBody
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(SampleData.interests, key = { it.id }) { interest ->
                InterestTile(
                    interest = interest,
                    selected = selected.contains(interest.id),
                    onClick = { onToggle(interest.id) }
                )
            }
        }

        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)) {
            ClayButton(
                text = if (canContinue) "Start discovering"
                else "Pick ${3 - selected.size} more",
                onClick = onContinue,
                enabled = canContinue,
                trailingIcon = if (canContinue) Icons.Rounded.ArrowForward else null,
                brush = if (canContinue) PurpleClayBrush else SurfaceClayBrush,
                contentColor = if (canContinue) TextOnClay else TextBody,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun InterestTile(
    interest: Interest,
    selected: Boolean,
    onClick: () -> Unit
) {
    // Selected tiles extrude further: bigger offsets, wider blur.
    val darkOffset by animateDpAsState(
        targetValue = if (selected) ClayDepth.DarkOffsetDeep else ClayDepth.DarkOffsetSmall,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
        label = "tileDark"
    )
    val lightOffset by animateDpAsState(
        targetValue = if (selected) ClayDepth.LightOffsetDeep else ClayDepth.LightOffsetSmall,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
        label = "tileLight"
    )
    val blur by animateDpAsState(
        targetValue = if (selected) ClayDepth.BlurDeep else ClayDepth.BlurSmall,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 420f),
        label = "tileBlur"
    )
    val lift by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f),
        label = "tileLift"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) TextOnClay else TextHeading,
        label = "tileLabel"
    )

    Box(
        modifier = Modifier
            .aspectRatio(0.92f)
            .scale(lift)
            .clayExtruded(
                cornerRadius = ClayRadius.Tile,
                darkOffset = darkOffset,
                lightOffset = lightOffset,
                blur = blur
            )
            .clip(ClayShape(ClayRadius.Tile))
            .background(if (selected) PurpleClayBrush else SurfaceClayBrush)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = interest.emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text = interest.label,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        AnimatedVisibility(
            visible = selected,
            enter = scaleIn(spring(dampingRatio = 0.45f)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Box(
                modifier = Modifier
                    .padding(7.dp)
                    .size(20.dp)
                    .clip(ClayShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                androidx.compose.ui.graphics.Color.White,
                                androidx.compose.ui.graphics.Color(0xFFE7E4F5)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = ScholrPurple,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
