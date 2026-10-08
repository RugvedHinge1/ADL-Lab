package com.scholr.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scholr.app.ScholrViewModel
import com.scholr.app.data.SampleData
import com.scholr.app.ui.clay.ClayIconButton
import com.scholr.app.ui.clay.ClaySearchField
import com.scholr.app.ui.clay.claySurface
import com.scholr.app.ui.components.PaperCard
import com.scholr.app.ui.components.SectionHeader
import com.scholr.app.ui.theme.ClayBase
import com.scholr.app.ui.theme.ClayRadius
import com.scholr.app.ui.theme.ClaySpacing
import com.scholr.app.ui.theme.ScholrPurple
import com.scholr.app.ui.theme.TextBody
import com.scholr.app.ui.theme.TextHeading

@Composable
fun SearchScreen(
    vm: ScholrViewModel,
    innerPadding: PaddingValues,
    onPaperClick: (String) -> Unit
) {
    val query = vm.searchQuery
    val results = vm.searchResults

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
                Text("Search", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Titles, authors, venues, methods — all of it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextBody
                )
            }
        }

        item(key = "field") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ClaySearchField(
                    value = query,
                    onValueChange = { vm.searchQuery = it },
                    modifier = Modifier.weight(1f),
                    trailing = {
                        if (query.isNotEmpty()) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                tint = TextBody,
                                modifier = Modifier
                                    .size(19.dp)
                                    .clickable { vm.searchQuery = "" }
                            )
                        }
                    }
                )
                Spacer(Modifier.width(12.dp))
                ClayIconButton(
                    icon = Icons.Rounded.Tune,
                    contentDescription = "Filters",
                    onClick = { },
                    size = 56.dp,
                    tint = ScholrPurple,
                    cornerRadius = ClayRadius.Button
                )
            }
        }

        if (query.isBlank()) {
            item(key = "recent-header") {
                SectionHeader(title = "Recent", action = "Clear")
            }
            items(SampleData.recentSearches, key = { "recent-$it" }) { term ->
                SuggestionRow(
                    text = term,
                    leading = Icons.Rounded.History,
                    onClick = { vm.searchQuery = term }
                )
            }

            item(key = "topics-header") {
                Spacer(Modifier.height(4.dp))
                SectionHeader(title = "Trending in your fields")
            }
            items(SampleData.trendingTopics, key = { "topic-$it" }) { topic ->
                SuggestionRow(
                    text = topic,
                    leading = Icons.Rounded.NorthEast,
                    onClick = { vm.searchQuery = topic }
                )
            }
        } else if (vm.searchLoading) {
            item(key = "loading") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ScholrPurple)
                }
            }
        } else if (vm.searchError != null) {
            item(key = "search-error") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = vm.searchError.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (results.isEmpty()) {
            item(key = "empty") { EmptySearchState(query) }
        } else {
            item(key = "count") {
                Text(
                    text = "${results.size} result${if (results.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextBody
                )
            }
            items(results, key = { it.id }) { paper ->
                PaperCard(
                    paper = paper,
                    bookmarked = vm.isBookmarked(paper.id),
                    onBookmarkToggle = { vm.toggleBookmark(paper.id) },
                    onClick = { onPaperClick(paper.id) }
                )
            }
        }

        item(key = "tail") { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun SuggestionRow(
    text: String,
    leading: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .claySurface(cornerRadius = ClayRadius.Pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(leading, null, tint = TextBody, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextHeading,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptySearchState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .claySurface(cornerRadius = 34.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.SearchOff,
                contentDescription = null,
                tint = TextBody,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Nothing for “$query”",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Try a broader term, an author surname, or one of the " +
                "trending topics below.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextBody,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
