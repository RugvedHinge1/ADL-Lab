package com.scholr.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scholr.app.ScholrViewModel
import com.scholr.app.data.model.FieldPalette
import com.scholr.app.data.model.Paper
import com.scholr.app.ui.clay.ClayShape
import com.scholr.app.ui.clay.claySurface
import com.scholr.app.ui.clay.clayWell
import com.scholr.app.ui.theme.ClayRadius
import com.scholr.app.ui.theme.PurpleClayBrush
import com.scholr.app.ui.theme.ScholrPurple
import com.scholr.app.ui.theme.SurfaceClayBrush
import com.scholr.app.ui.theme.TextBody
import com.scholr.app.ui.theme.TextHeading
import com.scholr.app.ui.theme.TextOnClay

/**
 * The AI Analysis panel: an 80%-height sheet that opens over the paper
 * detail. The generated summary and the conversation share one scroll, with
 * the composer pinned to the bottom — so a question the user asks lands
 * directly under the summary that prompted it.
 */
@Composable
fun AiAnalysisSheetContent(
    paper: Paper,
    vm: ScholrViewModel,
    onClose: () -> Unit
) {
    val accent = FieldPalette.colorFor(paper.field)
    val messages = vm.chatFor(paper.id)
    val listState = rememberLazyListState()
    var draft by remember { mutableStateOf("") }

    // Keep the newest message in view as the conversation grows.
    LaunchedEffect(messages.size, vm.aiThinking) {
        val last = listState.layoutInfo.totalItemsCount - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    Column(
        modifier = Modifier
            .fillMaxHeight(0.8f)
            .fillMaxWidth()
    ) {
        /* ---------------- header ---------------- */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .claySurface(cornerRadius = 14.dp, brush = PurpleClayBrush),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = TextOnClay,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("AI Analysis", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = paper.venue + " · " + paper.year,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextBody
                )
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clayWell(cornerRadius = 13.dp, offset = 3.dp, blur = 7.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Close analysis",
                    tint = TextBody,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        /* ---------------- summary + conversation ---------------- */
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "tldr") {
                SummaryBlock(
                    label = "In one line",
                    accent = accent,
                    body = paper.keyFindings.first()
                )
            }

            item(key = "what") {
                SummaryBlock(
                    label = "What they did",
                    accent = accent,
                    body = paper.abstractText
                )
            }

            item(key = "findings") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .claySurface(cornerRadius = ClayRadius.Card)
                        .padding(18.dp)
                ) {
                    Text(
                        text = "KEY FINDINGS",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                    Spacer(Modifier.height(12.dp))
                    paper.keyFindings.forEachIndexed { index, finding ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 5.dp)
                                    .size(18.dp)
                                    .clip(ClayShape(7.dp))
                                    .background(accent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (index + 1).toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent
                                )
                            }
                            Spacer(Modifier.width(11.dp))
                            Text(
                                text = finding,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextHeading
                            )
                        }
                        if (index != paper.keyFindings.lastIndex) {
                            Spacer(Modifier.height(11.dp))
                        }
                    }
                }
            }

            item(key = "divider") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "ASK ANYTHING",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextBody
                    )
                }
            }

            items(messages, key = { it.id }) { message ->
                ChatBubble(
                    text = message.text,
                    fromUser = message.fromUser,
                    accent = accent
                )
            }

            if (vm.aiThinking) {
                item(key = "typing") { TypingIndicator() }
            }

            item(key = "chat-tail") { Spacer(Modifier.height(4.dp)) }
        }

        /* ---------------- suggested prompts ---------------- */
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            items(vm.suggestedPrompts(paper), key = { it }) { prompt ->
                Box(
                    modifier = Modifier
                        .claySurface(
                            cornerRadius = ClayRadius.Pill,
                            brush = SurfaceClayBrush,
                            darkOffset = 4.dp,
                            lightOffset = (-3).dp,
                            blur = 8.dp
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { vm.askAi(paper, prompt) }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodySmall,
                        color = ScholrPurple,
                        maxLines = 1
                    )
                }
            }
        }

        /* ---------------- composer ---------------- */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clayWell(cornerRadius = ClayRadius.Button, offset = 4.dp, blur = 10.dp)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (draft.isEmpty()) {
                    Text(
                        text = "Ask about this paper…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextBody.copy(alpha = 0.75f),
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextHeading),
                    cursorBrush = SolidColor(ScholrPurple),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .claySurface(cornerRadius = ClayRadius.Button, brush = PurpleClayBrush)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        vm.askAi(paper, draft)
                        draft = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.ArrowUpward,
                    contentDescription = "Send",
                    tint = TextOnClay,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryBlock(label: String, accent: Color, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .claySurface(cornerRadius = ClayRadius.Card)
            .padding(18.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = accent
        )
        Spacer(Modifier.height(9.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = TextHeading
        )
    }
}

@Composable
private fun ChatBubble(text: String, fromUser: Boolean, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .then(
                    if (fromUser) {
                        Modifier.claySurface(
                            cornerRadius = ClayRadius.Button,
                            brush = PurpleClayBrush,
                            darkOffset = 6.dp,
                            lightOffset = (-4).dp,
                            blur = 12.dp
                        )
                    } else {
                        Modifier.claySurface(
                            cornerRadius = ClayRadius.Button,
                            brush = Brush.linearGradient(
                                listOf(Color.White, Color(0xFFF6F4FC))
                            ),
                            darkOffset = 6.dp,
                            lightOffset = (-4).dp,
                            blur = 12.dp
                        )
                    }
                )
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = if (fromUser) TextOnClay else TextHeading
            )
        }
    }
}

/** Three clay dots that bounce while the model is "thinking". */
@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")

    Row(
        modifier = Modifier
            .claySurface(
                cornerRadius = ClayRadius.Button,
                brush = SurfaceClayBrush,
                darkOffset = 5.dp,
                lightOffset = (-4).dp,
                blur = 10.dp
            )
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(3) { index ->
            val offset by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 900
                        0f at 0
                        1f at 220
                        0f at 480
                        0f at 900
                    },
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = androidx.compose.animation.core.StartOffset(index * 130)
                ),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .graphicsLayer { translationY = -offset * 6f * density }
                    .size(8.dp)
                    .clip(ClayShape(4.dp))
                    .background(ScholrPurple.copy(alpha = 0.35f + offset * 0.5f))
            )
        }
    }
}
