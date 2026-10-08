package com.scholr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scholr.app.ScholrViewModel
import com.scholr.app.data.model.FieldPalette
import com.scholr.app.ui.clay.ClayButton
import com.scholr.app.ui.clay.ClayIconButton
import com.scholr.app.ui.clay.ClayShape
import com.scholr.app.ui.clay.SheetShape
import com.scholr.app.ui.clay.claySurface
import com.scholr.app.ui.components.ClayBookmark
import com.scholr.app.ui.components.formatCitations
import com.scholr.app.ui.theme.ClayBase
import com.scholr.app.ui.theme.ClayCardWhite
import com.scholr.app.ui.theme.ClayRadius
import com.scholr.app.ui.theme.ClaySpacing
import com.scholr.app.ui.theme.PurpleClayBrush
import com.scholr.app.ui.theme.ScholrPurple
import com.scholr.app.ui.theme.SurfaceClayBrush
import com.scholr.app.ui.theme.TextBody
import com.scholr.app.ui.theme.TextHeading
import kotlinx.coroutines.launch

/**
 * Paper detail. Scrolling content with a floating clay top bar and a pinned
 * action dock; tapping "AI Summary" slides the analysis sheet up over 80% of
 * the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperDetailScreen(
    paperId: String,
    vm: ScholrViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(paperId) { vm.ensurePaperLoaded(paperId) }
    val paper = vm.paperFor(paperId)
    if (paper == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ClayBase)
        ) {
            CircularProgressIndicator(
                color = ScholrPurple,
                modifier = Modifier.align(Alignment.Center)
            )
            ClayIconButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
                tint = TextHeading,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(horizontal = ClaySpacing.ListPadding, vertical = 12.dp)
            )
        }
        return
    }

    val accent = FieldPalette.colorFor(paper.field)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var sheetOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ClayBase)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = ClaySpacing.ListPadding,
                end = ClaySpacing.ListPadding,
                top = 88.dp,
                bottom = 124.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "hero") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .claySurface(
                            cornerRadius = ClayRadius.Card,
                            brush = Brush.linearGradient(listOf(Color.White, ClayCardWhite))
                        )
                        .border(1.dp, accent.copy(alpha = 0.14f), ClayShape(ClayRadius.Card))
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(ClayShape(ClayRadius.Pill))
                                .background(accent.copy(alpha = 0.13f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = paper.field.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        ClayBookmark(
                            bookmarked = vm.isBookmarked(paper.id),
                            accent = accent,
                            onClick = { vm.toggleBookmark(paper.id) }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(text = paper.title, style = MaterialTheme.typography.headlineMedium)

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = paper.authors,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextBody
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "${paper.venue} · ${paper.year}",
                        style = MaterialTheme.typography.titleSmall,
                        color = accent
                    )
                }
            }

            item(key = "stats") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        icon = Icons.Rounded.FormatQuote,
                        value = formatCitations(paper.citations),
                        label = "Citations",
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        icon = Icons.Rounded.Schedule,
                        value = "${paper.readMinutes}m",
                        label = "Read time",
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        icon = Icons.Rounded.WorkspacePremium,
                        value = if (paper.openAccess) "Open" else "Paywall",
                        label = "Access",
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "abstract") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .claySurface(cornerRadius = ClayRadius.Card)
                        .padding(20.dp)
                ) {
                    Text(
                        text = "ABSTRACT",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextBody
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = paper.abstractText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextHeading
                    )
                }
            }

            item(key = "findings") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .claySurface(cornerRadius = ClayRadius.Card)
                        .padding(20.dp)
                ) {
                    Text(
                        text = "KEY FINDINGS",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                    Spacer(Modifier.height(12.dp))
                    paper.keyFindings.forEach { finding ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 7.dp)
                                    .size(8.dp)
                                    .clip(ClayShape(4.dp))
                                    .background(accent)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = finding,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextHeading,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }

            item(key = "tags") {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    paper.tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .claySurface(
                                    cornerRadius = ClayRadius.Pill,
                                    brush = SurfaceClayBrush,
                                    darkOffset = 4.dp,
                                    lightOffset = (-3).dp,
                                    blur = 8.dp
                                )
                                .padding(horizontal = 13.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextBody,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        /* ---------------- floating top bar ---------------- */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = ClaySpacing.ListPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ClayIconButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
                tint = TextHeading
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ClayIconButton(
                    icon = Icons.Rounded.Download,
                    contentDescription = "Download PDF",
                    onClick = { },
                    tint = TextHeading
                )
                ClayIconButton(
                    icon = Icons.Rounded.Share,
                    contentDescription = "Share",
                    onClick = { },
                    tint = TextHeading
                )
            }
        }

        /* ---------------- pinned action dock ---------------- */
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = ClaySpacing.ListPadding, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClayIconButton(
                icon = Icons.Rounded.PictureAsPdf,
                contentDescription = "Read full text",
                onClick = { },
                size = 58.dp,
                cornerRadius = ClayRadius.Button,
                tint = ScholrPurple
            )
            ClayButton(
                text = "AI Summary",
                onClick = { sheetOpen = true },
                leadingIcon = Icons.Rounded.AutoAwesome,
                brush = PurpleClayBrush,
                modifier = Modifier.weight(1f)
            )
        }
    }

    /* ---------------- the 80% AI analysis sheet ---------------- */
    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState,
            containerColor = ClayBase,
            shape = SheetShape,
            dragHandle = { ClayDragHandle() }
        ) {
            AiAnalysisSheetContent(
                paper = paper,
                vm = vm,
                onClose = {
                    scope.launch { sheetState.hide() }
                        .invokeOnCompletion { sheetOpen = false }
                }
            )
        }
    }
}

/** A clay pebble drag handle for the bottom sheet. */
@Composable
fun ClayDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(46.dp)
                .height(6.dp)
                .clip(ClayShape(3.dp))
                .background(TextBody.copy(alpha = 0.28f))
        )
    }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .claySurface(cornerRadius = ClayRadius.Tile)
            .padding(vertical = 16.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = TextHeading,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextBody,
            maxLines = 1
        )
    }
}
