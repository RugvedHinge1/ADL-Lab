package com.scholr.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scholr.app.ScholrViewModel
import com.scholr.app.data.model.Paper
import com.scholr.app.data.model.PaperCollection
import com.scholr.app.data.scan.ScanCodec
import com.scholr.app.ui.clay.ClayButton
import com.scholr.app.ui.clay.ClayTextField
import com.scholr.app.ui.clay.SheetShape
import com.scholr.app.ui.clay.ClayShape
import com.scholr.app.ui.clay.claySurface
import com.scholr.app.ui.clay.clayWell
import com.scholr.app.ui.components.PaperCard
import com.scholr.app.ui.components.SectionHeader
import com.scholr.app.ui.theme.ClayBase
import com.scholr.app.ui.theme.ClayRadius
import com.scholr.app.ui.theme.ClaySpacing
import com.scholr.app.ui.theme.PurpleClayBrush
import com.scholr.app.ui.theme.ScholrPurple
import com.scholr.app.ui.theme.TextBody
import com.scholr.app.ui.theme.TextHeading
import com.scholr.app.ui.theme.TextOnClay

private enum class LibraryTab(val label: String) { Saved("Saved"), Collections("Collections") }

@Composable
fun LibraryScreen(
    vm: ScholrViewModel,
    innerPadding: PaddingValues,
    onPaperClick: (String) -> Unit
) {
    var tab by remember { mutableStateOf(LibraryTab.Saved) }
    val saved = vm.bookmarkedPapers
    var qrCollection by remember { mutableStateOf<PaperCollection?>(null) }
    var creating by remember { mutableStateOf(false) }

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
                Text("Library", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${saved.size} saved · ${vm.collections.size} collections",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextBody
                )
            }
        }

        item(key = "segmented") {
            ClaySegmentedControl(
                options = LibraryTab.entries.map { it.label },
                selectedIndex = LibraryTab.entries.indexOf(tab),
                onSelect = { tab = LibraryTab.entries[it] }
            )
        }

        when (tab) {
            LibraryTab.Saved -> {
                if (saved.isEmpty()) {
                    item(key = "empty") { EmptyLibraryState() }
                } else {
                    items(saved, key = { it.id }) { paper ->
                        PaperCard(
                            paper = paper,
                            bookmarked = true,
                            onBookmarkToggle = { vm.toggleBookmark(paper.id) },
                            onClick = { onPaperClick(paper.id) }
                        )
                    }
                }
            }

            LibraryTab.Collections -> {
                item(key = "collections-header") {
                    SectionHeader(
                        title = "Your collections",
                        action = "New",
                        onActionClick = { creating = true }
                    )
                }
                if (vm.collections.isEmpty()) {
                    item(key = "no-collections") {
                        Text(
                            text = "No collections yet. Make one from your saved papers, or scan a " +
                                "collection QR code from the Home screen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextBody
                        )
                    }
                }
                items(vm.collections, key = { it.id }) { collection ->
                    CollectionCard(collection = collection, onClick = { qrCollection = collection })
                }
                item(key = "recent-header") {
                    Spacer(Modifier.height(4.dp))
                    SectionHeader(title = "Recently added")
                }
                item(key = "recent-row") {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(saved, key = { "mini-${it.id}" }) { paper ->
                            MiniPaperTile(
                                title = paper.title,
                                field = paper.field,
                                onClick = { onPaperClick(paper.id) }
                            )
                        }
                    }
                }
            }
        }

        item(key = "tail") { Spacer(Modifier.height(8.dp)) }
    }

    qrCollection?.let { collection ->
        LaunchedEffect(collection.id) { vm.ensurePapersLoaded(collection.paperIds) }
        val shared = collection.paperIds.take(ScanCodec.MAX_PAPERS_PER_QR)
        QrShareSheet(
            title = "${collection.emoji}  ${collection.name}",
            subtitle = if (collection.paperIds.size == 1) "1 paper" else "${collection.paperIds.size} papers",
            payload = ScanCodec.collectionPayload(collection.name, collection.emoji, collection.paperIds),
            shareCaption = "Scholr collection: ${collection.name}",
            hint = if (collection.paperIds.size > shared.size)
                "Scan this in Scholr to import the collection. Only the first ${shared.size} papers fit in one code."
            else "Scan this in Scholr (Home, then Scan) to import the whole collection.",
            onDismiss = { qrCollection = null },
            extra = {
                shared.mapNotNull { vm.paperFor(it) }.forEach { paper ->
                    PaperRow(paper, onClick = { qrCollection = null; onPaperClick(paper.id) })
                    Spacer(Modifier.height(10.dp))
                }
            }
        )
    }

    if (creating) {
        NewCollectionSheet(
            saved = saved,
            onDismiss = { creating = false },
            onCreate = { name, ids ->
                vm.createCollection(name, ids)
                creating = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewCollectionSheet(
    saved: List<Paper>,
    onDismiss: () -> Unit,
    onCreate: (String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf(setOf<String>()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ClayBase,
        shape = SheetShape,
        dragHandle = { ClayDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp)
        ) {
            Text("New collection", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(14.dp))
            ClayTextField(value = name, onValueChange = { name = it }, label = "Name", placeholder = "e.g. Diffusion models")
            Spacer(Modifier.height(16.dp))
            Text("Pick from your saved papers", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(10.dp))
            if (saved.isEmpty()) {
                Text(
                    "Nothing saved yet - bookmark a few papers first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextBody
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false).heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(saved, key = { it.id }) { paper ->
                        val on = paper.id in picked
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .claySurface(cornerRadius = ClayRadius.Pill)
                                .clickable { picked = if (on) picked - paper.id else picked + paper.id }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                paper.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(10.dp))
                            Icon(
                                if (on) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = if (on) "Selected" else "Not selected",
                                tint = if (on) ScholrPurple else TextBody
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            ClayButton(
                text = "Create collection",
                onClick = { onCreate(name, saved.filter { it.id in picked }.map { it.id }) },
                enabled = name.isNotBlank() && picked.isNotEmpty(),
                brush = PurpleClayBrush,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Two-up segmented control: a recessed track with an extruded purple thumb
 * that slides between the options.
 */
@Composable
fun ClaySegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clayWell(cornerRadius = ClayRadius.Button, offset = 4.dp, blur = 10.dp)
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val weight by animateFloatAsState(
                targetValue = if (selected) 1.25f else 1f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
                label = "segWeight"
            )
            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxSize()
                    .then(
                        if (selected) Modifier
                            .claySurface(
                                cornerRadius = ClayRadius.Pill,
                                brush = PurpleClayBrush,
                                darkOffset = 5.dp,
                                lightOffset = (-4).dp,
                                blur = 10.dp
                            )
                        else Modifier
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) TextOnClay else TextBody,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CollectionCard(collection: PaperCollection, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .claySurface(cornerRadius = ClayRadius.Card)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(ClayShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFEDEBF8), Color(0xFFDFDBF2))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(collection.emoji, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = collection.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (collection.paperIds.size == 1) "1 paper" else "${collection.paperIds.size} papers",
                style = MaterialTheme.typography.bodySmall,
                color = TextBody
            )
        }
        Icon(
            Icons.Rounded.QrCode2,
            contentDescription = "Show QR code",
            tint = ScholrPurple,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun MiniPaperTile(title: String, field: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(168.dp)
            .height(126.dp)
            .claySurface(cornerRadius = ClayRadius.Tile)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = TextHeading,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = field,
            style = MaterialTheme.typography.labelSmall,
            color = com.scholr.app.data.model.FieldPalette.colorFor(field)
        )
    }
}

@Composable
private fun EmptyLibraryState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .claySurface(cornerRadius = 34.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.BookmarkBorder,
                contentDescription = null,
                tint = TextBody,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("Nothing saved yet", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tap the bookmark on any paper and it will land here, " +
                "ready to sort into a collection.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextBody,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp)
        )
    }
}
