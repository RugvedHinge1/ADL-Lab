package com.scholr.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scholr.app.ScholrViewModel
import com.scholr.app.data.SampleData
import com.scholr.app.data.model.FieldPalette
import com.scholr.app.ui.clay.ClayButton
import com.scholr.app.ui.clay.ClayChip
import com.scholr.app.ui.clay.ClayPanel
import com.scholr.app.ui.clay.ClayShape
import com.scholr.app.ui.clay.claySurface
import com.scholr.app.ui.components.PaperCard
import com.scholr.app.ui.components.ScholrHeader
import com.scholr.app.ui.components.SectionHeader
import com.scholr.app.ui.theme.AcademicGreen
import com.scholr.app.ui.theme.ClayBase
import com.scholr.app.ui.theme.ClayRadius
import com.scholr.app.ui.theme.ClaySpacing
import com.scholr.app.ui.theme.FieldMedicine
import com.scholr.app.ui.theme.GreenClayBrush
import com.scholr.app.ui.theme.PurpleClayBrush
import com.scholr.app.ui.theme.ScholrPurple
import com.scholr.app.ui.theme.SurfaceClayBrush
import com.scholr.app.ui.theme.TextBody
import com.scholr.app.ui.theme.TextHeading
import com.scholr.app.ui.theme.TextOnClay

/**
 * The discovery feed.
 *
 * Note the LazyColumn contentPadding — 16dp on every side. Clay shadows are
 * drawn *outside* each card's layout bounds, so without this the list would
 * shave them off at the viewport edges and the cards would look flat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: ScholrViewModel,
    innerPadding: PaddingValues,
    onPaperClick: (String) -> Unit,
    onProfileClick: () -> Unit = {}
) {
    val feed = vm.feed

    PullToRefreshBox(
        isRefreshing = vm.papersLoading,
        onRefresh = { vm.refreshFeed() },
        modifier = Modifier.fillMaxSize()
    ) {
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
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "header") {
            ScholrHeader(
                unreadCount = vm.unreadNotifications,
                onProfileClick = onProfileClick,
                onNotificationsClick = { vm.clearNotifications() }
            )
        }

        item(key = "greeting") {
            Column {
                Text(
                    text = "Hello, ${vm.userName}",
                    style = MaterialTheme.typography.displayMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${feed.size} papers matched your fields this week.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextBody
                )
            }
        }

        item(key = "filters") {
            // contentPadding (not padding) so the first and last chip can still
            // cast their shadows past the scroll edge.
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(SampleData.quickFilters, key = { it }) { filter ->
                    ClayChip(
                        label = filter,
                        selected = vm.activeFilter == filter,
                        onClick = { vm.setFilter(filter) },
                        leadingIcon = when (filter) {
                            "For you" -> Icons.Rounded.AutoAwesome
                            "Trending" -> Icons.Rounded.TrendingUp
                            "Highly cited" -> Icons.Rounded.LocalFireDepartment
                            else -> null
                        }
                    )
                }
            }
        }

        item(key = "streak") { ReadingStreakPanel() }

        item(key = "section") {
            SectionHeader(
                title = if (vm.activeFilter == "For you") "Recommended for you"
                else vm.activeFilter,
                action = "See all"
            )
        }

        if (vm.papersError != null && feed.isEmpty()) {
            item(key = "papers-error") {
                FeedErrorState(message = vm.papersError.orEmpty(), onRetry = { vm.refreshFeed() })
            }
        } else if (vm.papersLoading && feed.isEmpty()) {
            item(key = "papers-loading") { FeedLoadingState() }
        } else {
            items(feed, key = { it.id }) { paper ->
                PaperCard(
                    paper = paper,
                    bookmarked = vm.isBookmarked(paper.id),
                    onBookmarkToggle = { vm.toggleBookmark(paper.id) },
                    onClick = { onPaperClick(paper.id) }
                )
            }
        }

        item(key = "trending-header") {
            Spacer(Modifier.height(4.dp))
            SectionHeader(title = "Trending topics")
        }

        item(key = "trending") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SampleData.trendingTopics.forEachIndexed { index, topic ->
                    TrendingRow(rank = index + 1, topic = topic)
                }
            }
        }

        item(key = "tail") { Spacer(Modifier.height(8.dp)) }
    }
    }
}

@Composable
private fun FeedLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = ScholrPurple)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Finding papers that match your interests…",
            style = MaterialTheme.typography.bodyMedium,
            color = TextBody
        )
    }
}

@Composable
private fun FeedErrorState(message: String, onRetry: () -> Unit) {
    ClayPanel(modifier = Modifier.fillMaxWidth(), brush = SurfaceClayBrush) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = FieldMedicine
            )
            Spacer(Modifier.height(14.dp))
            ClayButton(
                text = "Retry",
                onClick = onRetry,
                brush = PurpleClayBrush,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** A small "your reading week" strip — seven clay bars and a streak count. */
@Composable
private fun ReadingStreakPanel() {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    val values = listOf(0.35f, 0.62f, 0.48f, 0.85f, 0.72f, 0.20f, 0.55f)

    ClayPanel(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Reading streak", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "6 days · 2h 14m this week",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextBody
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .claySurface(cornerRadius = 15.dp, brush = GreenClayBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "6",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextOnClay
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEachIndexed { index, day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height((10 + values[index] * 42).dp)
                                .clip(ClayShape(7.dp))
                                .background(
                                    if (index == 3) GreenClayBrush else PurpleClayBrush
                                )
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextBody
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendingRow(rank: Int, topic: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .claySurface(cornerRadius = ClayRadius.Pill)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(ClayShape(11.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            FieldPalette.colorFor("Computer Science").copy(alpha = 0.18f),
                            FieldPalette.colorFor("Biology").copy(alpha = 0.18f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = ScholrPurple
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = topic,
            style = MaterialTheme.typography.titleSmall,
            color = TextHeading,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Rounded.TrendingUp,
            contentDescription = null,
            tint = AcademicGreen,
            modifier = Modifier.size(17.dp)
        )
    }
}
