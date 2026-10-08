package com.scholr.app.ui.screens

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
import androidx.compose.runtime.Composable
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
import com.scholr.app.data.SampleData
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

private val fieldTabs = listOf("All", "Computer Science", "Neuroscience", "Chemistry", "Physics", "Climate Science")

@Composable
fun ConferencesScreen(
    innerPadding: PaddingValues,
    onConferenceClick: (String) -> Unit = {}
) {
    var tab by remember { mutableStateOf(fieldTabs.first()) }

    val list = remember(tab) {
        if (tab == "All") SampleData.conferences
        else SampleData.conferences.filter { it.field == tab }
    }
    val urgent = SampleData.conferences.filter { it.daysLeft <= 14 }.sortedBy { it.daysLeft }

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
                    "Deadlines, venues and calls for papers in your fields.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextBody
                )
            }
        }

        if (urgent.isNotEmpty()) {
            item(key = "urgent") { DeadlineAlert(urgent.first()) }
        }

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

        item(key = "section") {
            SectionHeader(title = "Upcoming", action = "Calendar")
        }

        items(list, key = { it.id }) { conference ->
            ConferenceCard(
                conference = conference,
                onClick = { onConferenceClick(conference.id) }
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
private fun ConferenceCard(conference: Conference, onClick: () -> Unit) {
    val accent = FieldPalette.colorFor(conference.field)
    val urgencyColor = when {
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
                        text = conference.location,
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
                    text = "${conference.daysLeft}d left",
                    style = MaterialTheme.typography.labelMedium,
                    color = urgencyColor
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = conference.tier,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
        }
    }
}
