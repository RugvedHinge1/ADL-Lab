package com.scholr.app.ui.clay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.scholr.app.ui.theme.ClayDepth
import com.scholr.app.ui.theme.ClayRadius
import com.scholr.app.ui.theme.PurpleClayBrush
import com.scholr.app.ui.theme.ScholrPurple
import com.scholr.app.ui.theme.SurfaceClayBrush
import com.scholr.app.ui.theme.TextBody
import com.scholr.app.ui.theme.TextHeading
import com.scholr.app.ui.theme.TextOnClay

/* ========================================================================
 *  Reusable clay controls. Each one animates from extruded to inset on
 *  press, which is the single gesture that makes claymorphism feel physical
 *  rather than just decorative.
 * ====================================================================== */

/**
 * Primary action button. Purple clay by default; pass [GreenClayBrush] for
 * the Home-flavoured variant.
 */
@Composable
fun ClayButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    brush: Brush = PurpleClayBrush,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    contentColor: Color = TextOnClay,
    cornerRadius: Dp = ClayRadius.Button,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val depth by animateFloatAsState(
        targetValue = if (pressed) 0.30f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 900f),
        label = "clayButtonDepth"
    )

    Box(
        modifier = modifier
            .heightIn(min = 58.dp)
            .clayExtruded(
                cornerRadius = cornerRadius,
                darkOffset = ClayDepth.DarkOffset * depth,
                lightOffset = ClayDepth.LightOffset * depth,
                blur = ClayDepth.Blur * (0.45f + 0.55f * depth)
            )
            .clip(ClayShape(cornerRadius))
            .background(brush)
            .then(
                if (pressed) Modifier.clayInset(
                    cornerRadius = cornerRadius,
                    offset = 3.dp,
                    blur = 9.dp
                ) else Modifier
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 26.dp, vertical = 17.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, null, tint = contentColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
            if (trailingIcon != null) {
                Spacer(Modifier.width(10.dp))
                Icon(trailingIcon, null, tint = contentColor, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** Quiet secondary button — the surface itself, extruded, with dark text. */
@Composable
fun ClayGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    cornerRadius: Dp = ClayRadius.Button
) {
    ClayButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        brush = SurfaceClayBrush,
        leadingIcon = leadingIcon,
        contentColor = TextHeading,
        cornerRadius = cornerRadius
    )
}

/**
 * Square-ish clay icon button — used for the header's profile / bell and for
 * back arrows. Set [inset] to render it as a recessed well instead (that is
 * the treatment the spec asks for on the bookmark control).
 */
@Composable
fun ClayIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = 46.dp,
    iconSize: Dp = 21.dp,
    tint: Color = TextHeading,
    cornerRadius: Dp = ClayRadius.Icon,
    inset: Boolean = false,
    background: Brush = SurfaceClayBrush
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val depth by animateFloatAsState(
        targetValue = if (pressed) 0.25f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 900f),
        label = "clayIconDepth"
    )

    val base = if (inset || pressed) {
        Modifier.clayWell(cornerRadius = cornerRadius, offset = 3.dp, blur = 8.dp)
    } else {
        Modifier
            .clayExtruded(
                cornerRadius = cornerRadius,
                darkOffset = ClayDepth.DarkOffsetSmall * depth,
                lightOffset = ClayDepth.LightOffsetSmall * depth,
                blur = ClayDepth.BlurSmall
            )
            .clip(ClayShape(cornerRadius))
            .background(background)
    }

    Box(
        modifier = modifier
            .size(size)
            .then(base)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Filter chip for the horizontal quick-filter row on Home.
 * Selected chips flip from surface clay to purple clay.
 */
@Composable
fun ClayChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    selectedBrush: Brush = PurpleClayBrush
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val depth by animateFloatAsState(
        targetValue = if (pressed) 0.3f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 1000f),
        label = "clayChipDepth"
    )

    Box(
        modifier = modifier
            .height(42.dp)
            .clayExtruded(
                cornerRadius = ClayRadius.Pill,
                darkOffset = ClayDepth.DarkOffsetSmall * depth,
                lightOffset = ClayDepth.LightOffsetSmall * depth,
                blur = ClayDepth.BlurSmall
            )
            .clip(ClayShape(ClayRadius.Pill))
            .background(if (selected) selectedBrush else SurfaceClayBrush)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon, null,
                    tint = if (selected) TextOnClay else ScholrPurple,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(7.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) TextOnClay else TextBody
            )
        }
    }
}

/**
 * Recessed search field. The spec explicitly puts search bars in the inset
 * treatment, so this is a [clayWell] rather than an extruded surface.
 */
@Composable
fun ClaySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search papers, authors, topics…",
    onSearch: () -> Unit = {},
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clayWell(cornerRadius = ClayRadius.Button, offset = 4.dp, blur = 11.dp)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = TextBody,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.width(12.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextBody.copy(alpha = 0.75f),
                    maxLines = 1
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.merge(
                    MaterialTheme.typography.bodyMedium.copy(
                        color = TextHeading,
                        fontWeight = FontWeight.Medium
                    )
                ),
                cursorBrush = SolidColor(ScholrPurple),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

/**
 * Generic raised panel. Anything that needs to sit "on top of" the base
 * surface without being a paper card goes through here.
 */
@Composable
fun ClayPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = ClayRadius.Card,
    brush: Brush = SurfaceClayBrush,
    contentPadding: Dp = 18.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .claySurface(cornerRadius = cornerRadius, brush = brush)
            .padding(contentPadding)
    ) {
        content()
    }
}
