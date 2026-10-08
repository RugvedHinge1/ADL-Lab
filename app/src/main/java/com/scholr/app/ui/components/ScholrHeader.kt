package com.scholr.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.scholr.app.ui.clay.ClayIconButton
import com.scholr.app.ui.clay.claySurface
import com.scholr.app.ui.clay.claySurfaceSolid
import com.scholr.app.ui.theme.AcademicGreen
import com.scholr.app.ui.theme.ClayRadius
import com.scholr.app.ui.theme.FieldMedicine
import com.scholr.app.ui.theme.LogoStyle
import com.scholr.app.ui.theme.PurpleClayBrush
import com.scholr.app.ui.theme.ScholrPurple
import com.scholr.app.ui.theme.TextHeading

/**
 * The Scholr wordmark. The "o" is replaced by an extruded purple clay dot
 * with a green orbiting accent, so the logo carries the design language even
 * at 26sp.
 */
@Composable
fun ScholrWordmark(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "logo")
    val bob by transition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "logoBob"
    )

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text("Sch", style = LogoStyle)
        Box(
            modifier = Modifier
                .padding(horizontal = 1.dp)
                .graphicsLayer { translationY = bob * density }
                .size(17.dp)
                .claySurface(
                    cornerRadius = 9.dp,
                    brush = PurpleClayBrush,
                    darkOffset = 3.dp,
                    lightOffset = (-2).dp,
                    blur = 6.dp
                ),
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                Modifier
                    .padding(2.5.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(AcademicGreen)
            )
        }
        Text("lr", style = LogoStyle)
    }
}

/**
 * Centre-aligned header bar: profile on the left, wordmark centred, bell on
 * the right — all sitting on one soft claymorphic bar.
 */
@Composable
fun ScholrHeader(
    modifier: Modifier = Modifier,
    unreadCount: Int = 0,
    onProfileClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .claySurface(cornerRadius = ClayRadius.Card)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ClayIconButton(
                icon = Icons.Rounded.PersonOutline,
                contentDescription = "Profile and menu",
                onClick = onProfileClick,
                tint = ScholrPurple
            )

            ScholrWordmark()

            Box(contentAlignment = Alignment.TopEnd) {
                ClayIconButton(
                    icon = Icons.Rounded.NotificationsNone,
                    contentDescription = "Notifications",
                    onClick = onNotificationsClick,
                    tint = TextHeading
                )
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .offset(x = 3.dp, y = (-3).dp)
                            .size(18.dp)
                            .claySurfaceSolid(
                                cornerRadius = 9.dp,
                                color = FieldMedicine,
                                darkOffset = 3.dp,
                                lightOffset = (-2).dp,
                                blur = 5.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/** Section title + optional trailing action, used up and down the app. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onActionClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        if (action != null) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = action,
                style = MaterialTheme.typography.labelMedium,
                color = ScholrPurple,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
