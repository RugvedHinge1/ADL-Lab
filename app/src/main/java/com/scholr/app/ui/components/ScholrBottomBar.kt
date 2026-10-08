package com.scholr.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.scholr.app.navigation.TopLevelDestination
import com.scholr.app.ui.clay.ClayShape
import com.scholr.app.ui.clay.clayExtruded
import com.scholr.app.ui.clay.claySurface
import com.scholr.app.ui.theme.ClayRadius
import com.scholr.app.ui.theme.GreenClayBrush
import com.scholr.app.ui.theme.PurpleClayBrush
import com.scholr.app.ui.theme.TextBody
import com.scholr.app.ui.theme.TextOnClay

/**
 * The fixed bottom navigation bar.
 *
 * Inactive items are bare icons sitting on the bar's clay. The active item
 * extrudes into a filled pill that grows to reveal its label — green for the
 * Home anchor, purple for everything else.
 */
@Composable
fun ScholrBottomBar(
    current: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(74.dp)
            .claySurface(cornerRadius = ClayRadius.Card)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TopLevelDestination.entries.forEach { dest ->
            NavItem(
                destination = dest,
                selected = dest == current,
                onClick = { onSelect(dest) }
            )
        }
    }
}

@Composable
private fun NavItem(
    destination: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val brush = if (destination.isAnchor) GreenClayBrush else PurpleClayBrush

    val iconTint by animateColorAsState(
        targetValue = if (selected) TextOnClay else TextBody,
        label = "navTint"
    )
    val lift by animateDpAsState(
        targetValue = if (selected) 6.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 550f),
        label = "navLift"
    )
    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .height(52.dp)
            .then(
                if (selected) Modifier
                    .clayExtruded(
                        cornerRadius = ClayRadius.Pill,
                        darkOffset = lift,
                        lightOffset = -lift * 0.75f,
                        blur = 12.dp
                    )
                    .clip(ClayShape(ClayRadius.Pill))
                    .background(brush)
                else Modifier.clip(ClayShape(ClayRadius.Pill))
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = if (selected) 16.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = destination.label,
            tint = iconTint,
            modifier = Modifier.size(23.dp)
        )
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = destination.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextOnClay
                )
            }
        }
    }
}

/** A soft rounded divider used to visually seat the bar against content. */
@Composable
fun ClayBarSpacer(modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(1.dp))
}
