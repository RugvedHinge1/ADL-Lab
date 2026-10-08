package com.scholr.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scholr.app.data.model.FieldPalette
import com.scholr.app.data.model.Paper
import com.scholr.app.ui.clay.ClayShape
import com.scholr.app.ui.clay.clayExtruded
import com.scholr.app.ui.clay.clayWell
import com.scholr.app.ui.theme.ClayCardWhite
import com.scholr.app.ui.theme.ClayDepth
import com.scholr.app.ui.theme.ClayRadius
import com.scholr.app.ui.theme.ScholrPurple
import com.scholr.app.ui.theme.TextBody
import com.scholr.app.ui.theme.TextHeading

/**
 * The paper card — the single most-repeated object in the app.
 *
 * Soft white clay surface with a hairline border, a vertical field tag
 * running up the left edge, and a recessed bookmark well top-right.
 */
@Composable
fun PaperCard(
    paper: Paper,
    bookmarked: Boolean,
    onBookmarkToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showAiHint: Boolean = true
) {
    val accent = FieldPalette.colorFor(paper.field)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val depth by animateFloatAsState(
        targetValue = if (pressed) 0.45f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "cardDepth"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clayExtruded(
                cornerRadius = ClayRadius.Card,
                darkOffset = ClayDepth.DarkOffset * depth,
                lightOffset = ClayDepth.LightOffset * depth,
                blur = ClayDepth.Blur
            )
            .clip(ClayShape(ClayRadius.Card))
            .background(
                Brush.linearGradient(listOf(Color.White, ClayCardWhite))
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.14f),
                shape = ClayShape(ClayRadius.Card)
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(14.dp)
        ) {
            VerticalFieldTag(field = paper.field, accent = accent)

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = paper.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextHeading,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    ClayBookmark(
                        bookmarked = bookmarked,
                        accent = accent,
                        onClick = onBookmarkToggle
                    )
                }

                Spacer(Modifier.height(7.dp))

                Text(
                    text = paper.authors,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextBody,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "${paper.venue} · ${paper.year}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = accent
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetaPill(
                        icon = Icons.Rounded.FormatQuote,
                        label = formatCitations(paper.citations),
                        tint = TextBody
                    )
                    MetaPill(
                        icon = Icons.Rounded.Schedule,
                        label = "${paper.readMinutes} min",
                        tint = TextBody
                    )
                    if (showAiHint) {
                        MetaPill(
                            icon = Icons.Rounded.AutoAwesome,
                            label = "AI summary",
                            tint = ScholrPurple
                        )
                    }
                }
            }
        }
    }
}

/**
 * The field label rotated to run up the left edge, with an accent rail
 * behind it. requiredWidth lets the text ignore the 30dp column constraint
 * before it is rotated into place.
 */
@Composable
private fun VerticalFieldTag(field: String, accent: Color) {
    Box(
        modifier = Modifier
            .width(30.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(5.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(accent, accent.copy(alpha = 0.45f))
                    )
                )
        )
        Text(
            text = field.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(start = 6.dp)
                .requiredWidth(126.dp)
                .rotate(-90f)
        )
    }
}

/** Recessed bookmark well — the spec's "clay-inset bookmark icon". */
@Composable
fun ClayBookmark(
    bookmarked: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 38.dp
) {
    val pop by animateFloatAsState(
        targetValue = if (bookmarked) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 700f),
        label = "bookmarkPop"
    )

    Box(
        modifier = modifier
            .size(size)
            .clayWell(cornerRadius = 13.dp, offset = 3.dp, blur = 7.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (bookmarked) Icons.Rounded.Bookmark
            else Icons.Rounded.BookmarkBorder,
            contentDescription = if (bookmarked) "Remove bookmark" else "Save paper",
            tint = if (bookmarked) accent else TextBody,
            modifier = Modifier
                .size(19.dp)
                .scale(pop)
        )
    }
}

@Composable
private fun MetaPill(icon: ImageVector, label: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = tint,
            maxLines = 1
        )
    }
}

internal fun formatCitations(count: Int): String = when {
    count >= 1000 -> String.format("%.1fk", count / 1000f)
    else -> count.toString()
}
