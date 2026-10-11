package com.scholr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.ui.platform.LocalContext
import com.scholr.app.data.geo.openDirections
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scholr.app.data.geo.LatLng
import com.scholr.app.data.geo.NearbyPlace
import com.scholr.app.data.model.Conference
import com.scholr.app.data.model.FieldPalette
import com.scholr.app.ui.clay.ClayChip
import com.scholr.app.ui.clay.ClayShape
import com.scholr.app.ui.clay.claySurface
import com.scholr.app.ui.components.NearbyMap
import com.scholr.app.ui.theme.ClayBase
import com.scholr.app.ui.theme.ClayCardWhite
import com.scholr.app.ui.theme.ClayRadius
import com.scholr.app.ui.theme.ClaySpacing
import com.scholr.app.ui.theme.TextBody
import com.scholr.app.ui.theme.TextHeading

internal fun formatKm(km: Double): String =
    if (km < 10) "%.1f km".format(km) else "%,d km".format(km.toInt())

/** List / Map switch plus a "use my location" action, shown above the list. */
@Composable
internal fun ViewToggleRow(
    onMap: () -> Unit,
    locating: Boolean,
    hasLocation: Boolean,
    onLocate: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ClayChip(label = "List", selected = true, onClick = {}) }
        item { ClayChip(label = "Map", selected = false, onClick = onMap) }
        item {
            ClayChip(
                label = when {
                    locating -> "Locating…"
                    hasLocation -> "Nearest first"
                    else -> "Use my location"
                },
                selected = hasLocation,
                onClick = onLocate,
                leadingIcon = Icons.Rounded.MyLocation
            )
        }
    }
}

/** Full-screen map of conferences (and optionally libraries) with a distance-sorted card strip. */
@Composable
internal fun ConferenceMapView(
    innerPadding: PaddingValues,
    conferences: List<Conference>,
    distances: Map<String, Double?>,
    libraries: List<NearbyPlace>,
    user: LatLng?,
    locating: Boolean,
    locationDenied: Boolean,
    showLibraries: Boolean,
    librariesError: String?,
    focusId: String?,
    onFocus: (String?) -> Unit,
    onShowList: () -> Unit,
    onToggleLibraries: () -> Unit,
    onLocate: () -> Unit,
    onConferenceClick: (String) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClayBase)
            .padding(
                top = innerPadding.calculateTopPadding() + ClaySpacing.ListPadding,
                bottom = innerPadding.calculateBottomPadding()
            )
    ) {
        Column(modifier = Modifier.padding(horizontal = ClaySpacing.ListPadding)) {
            Text("Conferences", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                text = when {
                    locationDenied -> "Location is off — allow it to see what is near you."
                    user != null -> "Sorted by distance from you."
                    else -> "Tap “Use my location” to find what is nearby."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextBody
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = ClaySpacing.ListPadding, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { ClayChip(label = "List", selected = false, onClick = onShowList) }
            item { ClayChip(label = "Map", selected = true, onClick = {}) }
            item {
                ClayChip(
                    label = when {
                        locating -> "Locating…"
                        user != null -> "Located"
                        else -> "Use my location"
                    },
                    selected = user != null,
                    onClick = onLocate,
                    leadingIcon = Icons.Rounded.MyLocation
                )
            }
            item {
                ClayChip(
                    label = librariesError ?: "Libraries",
                    selected = showLibraries && librariesError == null,
                    onClick = onToggleLibraries
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = ClaySpacing.ListPadding)
                .clip(ClayShape(ClayRadius.Card))
        ) {
            NearbyMap(
                conferences = conferences,
                libraries = libraries,
                user = user,
                focusConferenceId = focusId,
                onConferenceTap = onFocus,
                modifier = Modifier.fillMaxSize()
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = ClaySpacing.ListPadding, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(conferences, key = { it.id }) { c ->
                val accent = FieldPalette.colorFor(c.field)
                val focused = c.id == focusId
                Column(
                    modifier = Modifier
                        .width(220.dp)
                        .claySurface(
                            cornerRadius = ClayRadius.Card,
                            brush = Brush.linearGradient(listOf(Color.White, ClayCardWhite))
                        )
                        .border(
                            if (focused) 2.dp else 1.dp,
                            accent.copy(alpha = if (focused) 0.7f else 0.14f),
                            ClayShape(ClayRadius.Card)
                        )
                        .clickable { if (focused) onConferenceClick(c.id) else onFocus(c.id) }
                        .padding(14.dp)
                ) {
                    Text(c.acronym, style = MaterialTheme.typography.titleMedium, color = accent, maxLines = 1)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = c.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextBody,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = listOfNotNull(distances[c.id]?.let(::formatKm), when {
                            c.daysLeft == com.scholr.app.data.conferences.CLOSED -> "Closed"
                            c.daysLeft < 0 -> "Deadline TBA"
                            else -> "${c.daysLeft}d left"
                        })
                            .joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextHeading
                    )
                    Spacer(Modifier.height(10.dp))
                    ClayChip(
                        label = "Directions",
                        selected = false,
                        onClick = { openDirections(context, c.acronym, c.lat, c.lng, c.location) },
                        leadingIcon = Icons.Rounded.Directions
                    )
                }
            }
        }
    }
}
