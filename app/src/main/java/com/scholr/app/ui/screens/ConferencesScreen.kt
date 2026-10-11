package com.scholr.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.MyLocation
import com.scholr.app.data.geo.openDirections
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.scholr.app.data.geo.LatLng
import com.scholr.app.data.geo.LibrariesRepository
import com.scholr.app.data.geo.NearbyPlace
import com.scholr.app.data.geo.currentLocation
import com.scholr.app.data.geo.hasLocationPermission
import com.scholr.app.data.geo.haversineKm
import com.scholr.app.ui.components.NearbyMap
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scholr.app.ScholrViewModel
import androidx.compose.ui.platform.LocalUriHandler
import com.scholr.app.data.model.Conference
import com.scholr.app.data.model.FieldPalette
import com.scholr.app.ui.clay.ClayChip
import com.scholr.app.ui.clay.ClayShape
import com.scholr.app.ui.clay.clayWell
import com.scholr.app.ui.clay.claySurface
import com.scholr.app.ui.components.SectionHeader
import com.scholr.app.ui.theme.AcademicGreen
import com.scholr.app.ui.theme.ClayBase
import com.scholr.app.ui.theme.ClayCardWhite
import com.scholr.app.ui.theme.ClayRadius
import com.scholr.app.ui.theme.ClaySpacing
import com.scholr.app.ui.theme.FieldChemistry
import com.scholr.app.ui.theme.FieldMedicine
import com.scholr.app.ui.theme.TextBody
import com.scholr.app.ui.theme.TextHeading
import com.scholr.app.ui.theme.TextOnClay

private val radiusOptions = listOf<Int?>(null, 250, 1000, 3000)

@Composable
fun ConferencesScreen(
    vm: ScholrViewModel,
    innerPadding: PaddingValues
) {
    var tab by remember { mutableStateOf("All") }
    var radiusKm by remember { mutableStateOf<Int?>(null) }
    var showMap by remember { mutableStateOf(false) }
    var showLibraries by remember { mutableStateOf(false) }
    var focusId by remember { mutableStateOf<String?>(null) }
    var locating by remember { mutableStateOf(false) }
    var locationDenied by remember { mutableStateOf(false) }
    var libraries by remember { mutableStateOf<List<NearbyPlace>>(emptyList()) }
    var librariesError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val user = vm.userLocation
    val all = vm.conferences

    fun locate() {
        scope.launch {
            locating = true
            val here = currentLocation(context)
            locationDenied = here == null
            vm.updateUserLocation(here)
            locating = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) locate() else locationDenied = true
    }

    fun requestLocation() {
        if (hasLocationPermission(context)) locate()
        else permissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    LaunchedEffect(Unit) {
        vm.loadConferences()
        // Picked up automatically if permission was granted on a previous visit.
        if (hasLocationPermission(context) && vm.userLocation == null) locate()
    }

    LaunchedEffect(showLibraries, user) {
        val origin = user
        if (!showLibraries || origin == null) return@LaunchedEffect
        librariesError = null
        libraries = try {
            LibrariesRepository.nearby(origin)
        } catch (e: Exception) {
            librariesError = "Libraries unavailable"
            emptyList()
        }
    }

    val fieldTabs = remember(all) {
        listOf("All") + all.groupingBy { it.field }.eachCount()
            .entries.sortedByDescending { it.value }.map { it.key }
    }

    val distances = remember(all, user) {
        val origin = user
        all.associate { c ->
            c.id to if (origin != null && c.hasLocation) haversineKm(origin, LatLng(c.lat, c.lng)) else null
        }
    }

    val list = remember(all, tab, user, radiusKm) {
        var l = all.filter { tab == "All" || it.field == tab }
        val r = radiusKm
        if (user != null && r != null) l = l.filter { c -> distances[c.id]?.let { it <= r } == true }
        // With a known position, nearest conference first.
        if (user != null) {
            l.sortedWith(compareBy({ distances[it.id] ?: Double.MAX_VALUE }, { it.startEpochDay }))
        } else l
    }
    val urgent = all.filter { it.daysLeft in 0..14 }.sortedBy { it.daysLeft }
    val pendingVenues = vm.pendingVenueCount

    val openLink: (String) -> Unit = { id ->
        all.firstOrNull { it.id == id }?.link?.takeIf { it.isNotBlank() }?.let { runCatching { uriHandler.openUri(it) } }
    }

    if (showMap) {
        ConferenceMapView(
            innerPadding = innerPadding,
            conferences = list.filter { it.hasLocation },
            distances = distances,
            libraries = if (showLibraries) libraries else emptyList(),
            user = user,
            locating = locating,
            locationDenied = locationDenied,
            showLibraries = showLibraries,
            librariesError = librariesError,
            focusId = focusId,
            onFocus = { focusId = it },
            onShowList = { showMap = false },
            onToggleLibraries = {
                if (user == null) requestLocation()
                showLibraries = !showLibraries
            },
            onLocate = ::requestLocation,
            onConferenceClick = openLink
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ClayBase),
        contentPadding = PaddingValues(
            start = ClaySpacing.ListPadding,
            end = ClaySpacing.ListPadding,
            top = innerPadding.calculateTopPadding() + ClaySpacing.ListPadding,
            bottom = innerPadding.calculateBottomPadding() + ClaySpacing.ListPadding
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "title") {
            Column {
                Spacer(Modifier.height(6.dp))
                Text("Conferences", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when {
                        locationDenied -> "Location is off \u2014 allow it to see what is near you."
                        user != null && pendingVenues > 0 -> "Locating venues near you\u2026 ($pendingVenues to go)"
                        user != null -> "Live listings, nearest first."
                        else -> "Live deadlines, venues and calls for papers."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextBody
                )
            }
        }

        item(key = "viewToggle") {
            ViewToggleRow(
                onMap = { showMap = true },
                locating = locating,
                hasLocation = user != null,
                onLocate = ::requestLocation
            )
        }

        if (vm.conferencesLoading && all.isEmpty()) {
            item(key = "loading") {
                Text(
                    "Loading live conference listings\u2026",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextBody
                )
            }
        }

        vm.conferencesError?.let { message ->
            item(key = "error") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = TextBody)
                    ClayChip(label = "Try again", selected = false, onClick = { vm.loadConferences(forceRefresh = true) })
                }
            }
        }

        if (urgent.isNotEmpty()) {
            item(key = "urgent") { DeadlineAlert(urgent.first()) }
        }

        if (all.isNotEmpty()) {
            item(key = "tabs") {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(fieldTabs, key = { it }) { name ->
                        ClayChip(
                            label = name,
                            selected = tab == name,
                            onClick = { tab = name }
                        )
                    }
                }
            }
        }

        if (user != null) {
            item(key = "radius") {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(radiusOptions, key = { it ?: 0 }) { r ->
                        ClayChip(
                            label = if (r == null) "Anywhere" else "Within ${"%,d".format(r)} km",
                            selected = radiusKm == r,
                            onClick = { radiusKm = r }
                        )
                    }
                }
            }
        }

        if (all.isNotEmpty()) {
            item(key = "section") {
                SectionHeader(title = if (user != null) "Near you" else "Upcoming")
            }
        }

        if (all.isNotEmpty() && list.isEmpty()) {
            item(key = "empty") {
                Text(
                    when {
                        radiusKm != null && pendingVenues > 0 ->
                            "Still locating venues \u2014 results will appear here."
                        radiusKm != null ->
                            "No listed conferences within ${"%,d".format(radiusKm)} km of you \u2014 try a wider range."
                        else -> "Nothing matches these filters."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextBody
                )
            }
        }

        items(list, key = { it.id }) { conference ->
            ConferenceCard(
                conference = conference,
                distanceKm = distances[conference.id],
                onClick = { openLink(conference.id) }
            )
        }

        item(key = "tail") { Spacer(Modifier.height(8.dp)) }
    }
}

/** A red-accented alert for the nearest deadline. */
@Composable
private fun DeadlineAlert(conference: Conference) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .claySurface(
                cornerRadius = ClayRadius.Card,
                brush = Brush.linearGradient(
                    listOf(Color(0xFFFFF1F2), Color(0xFFFFE4E6))
                )
            )
            .border(1.dp, FieldMedicine.copy(alpha = 0.20f), ClayShape(ClayRadius.Card))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(ClayShape(15.dp))
                .background(FieldMedicine.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = FieldMedicine,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${conference.acronym} closes in ${conference.daysLeft} days",
                style = MaterialTheme.typography.titleSmall,
                color = TextHeading
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = conference.deadline,
                style = MaterialTheme.typography.bodySmall,
                color = TextBody
            )
        }
    }
}

@Composable
private fun ConferenceCard(conference: Conference, distanceKm: Double? = null, onClick: () -> Unit) {
    val accent = FieldPalette.colorFor(conference.field)
    val urgencyColor = when {
        conference.daysLeft < 0 -> TextBody
        conference.daysLeft <= 7 -> FieldMedicine
        conference.daysLeft <= 21 -> FieldChemistry
        else -> AcademicGreen
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .claySurface(
                cornerRadius = ClayRadius.Card,
                brush = Brush.linearGradient(listOf(Color.White, ClayCardWhite))
            )
            .border(1.dp, accent.copy(alpha = 0.14f), ClayShape(ClayRadius.Card))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            // Acronym badge in field colour.
            Box(
                modifier = Modifier
                    .size(width = 66.dp, height = 66.dp)
                    .clip(ClayShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(accent, accent.copy(alpha = 0.72f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = conference.acronym.take(6),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextOnClay,
                    maxLines = 1
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conference.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Place, null,
                        tint = TextBody, modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (distanceKm != null) "${conference.location} · ${formatKm(distanceKm)}"
                        else conference.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextBody,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.CalendarMonth, null,
                        tint = TextBody, modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = conference.dates,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextBody,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clayWell(cornerRadius = ClayRadius.Pill, offset = 3.dp, blur = 8.dp)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = conference.deadline,
                style = MaterialTheme.typography.bodySmall,
                color = TextBody,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(ClayShape(4.dp))
                        .background(urgencyColor)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when {
                        conference.daysLeft == com.scholr.app.data.conferences.CLOSED -> "Closed"
                        conference.daysLeft < 0 -> "Deadline TBA"
                        else -> "${conference.daysLeft}d left"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = urgencyColor
                )
                if (conference.tier.isNotBlank()) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = conference.tier,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
            }
        }

        if (conference.hasPlaceName) {
            val context = LocalContext.current
            Spacer(Modifier.height(12.dp))
            ClayChip(
                label = "Directions in Google Maps",
                selected = false,
                onClick = {
                    openDirections(
                        context, conference.acronym,
                        conference.lat.takeIf { conference.hasLocation },
                        conference.lng.takeIf { conference.hasLocation },
                        conference.location
                    )
                },
                leadingIcon = Icons.Rounded.Directions
            )
        }
    }
}
