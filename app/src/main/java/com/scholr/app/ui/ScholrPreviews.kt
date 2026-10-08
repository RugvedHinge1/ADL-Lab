package com.scholr.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.scholr.app.data.SampleData
import com.scholr.app.navigation.TopLevelDestination
import com.scholr.app.ui.clay.ClayButton
import com.scholr.app.ui.clay.ClayChip
import com.scholr.app.ui.clay.ClayGhostButton
import com.scholr.app.ui.clay.ClaySearchField
import com.scholr.app.ui.components.PaperCard
import com.scholr.app.ui.components.ScholrBottomBar
import com.scholr.app.ui.components.ScholrHeader
import com.scholr.app.ui.illustration.ClayBrainIllustration
import com.scholr.app.ui.illustration.ClayFolderIllustration
import com.scholr.app.ui.illustration.ClayMicroscopeIllustration
import com.scholr.app.ui.illustration.TestTubeLoader
import com.scholr.app.ui.theme.ClayBase
import com.scholr.app.ui.theme.GreenClayBrush
import com.scholr.app.ui.theme.ScholrTheme

/* ------------------------------------------------------------------------
 *  Design-system previews. Open any of these in Android Studio's split view
 *  to tune the clay without running the app.
 *
 *  Heads-up: the interactive preview renders shadows, but the *static*
 *  preview renderer sometimes drops framework shadow layers. If a preview
 *  looks flat, hit "Start Interactive Preview" or run on a device — the
 *  device rendering is the truth.
 * ---------------------------------------------------------------------- */

@Preview(name = "Clay controls", showBackground = true, backgroundColor = 0xFFF0EEF6)
@Composable
private fun ClayControlsPreview() {
    ScholrTheme {
        Column(
            modifier = Modifier
                .background(ClayBase)
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ClayButton(text = "Primary action", onClick = {}, modifier = Modifier.fillMaxWidth())
            ClayButton(
                text = "Home flavour",
                onClick = {},
                brush = GreenClayBrush,
                modifier = Modifier.fillMaxWidth()
            )
            ClayGhostButton(text = "Secondary", onClick = {}, modifier = Modifier.fillMaxWidth())
            ClaySearchField(value = "", onValueChange = {})
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ClayChip(label = "For you", selected = true, onClick = {})
                ClayChip(label = "Trending", selected = false, onClick = {})
            }
            TestTubeLoader(progress = 0.62f)
        }
    }
}

@Preview(name = "Header + nav", showBackground = true, backgroundColor = 0xFFF0EEF6)
@Composable
private fun ChromePreview() {
    ScholrTheme {
        Column(
            modifier = Modifier
                .background(ClayBase)
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ScholrHeader(unreadCount = 3)
            ScholrBottomBar(current = TopLevelDestination.Home, onSelect = {})
            ScholrBottomBar(current = TopLevelDestination.Library, onSelect = {})
        }
    }
}

@Preview(name = "Paper card", showBackground = true, backgroundColor = 0xFFF0EEF6, heightDp = 460)
@Composable
private fun PaperCardPreview() {
    ScholrTheme {
        Column(
            modifier = Modifier
                .background(ClayBase)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            var saved by remember { mutableStateOf(false) }
            PaperCard(
                paper = SampleData.papers[0],
                bookmarked = saved,
                onBookmarkToggle = { saved = !saved },
                onClick = {}
            )
            PaperCard(
                paper = SampleData.papers[1],
                bookmarked = true,
                onBookmarkToggle = {},
                onClick = {}
            )
        }
    }
}

@Preview(name = "Illustrations", showBackground = true, backgroundColor = 0xFFF0EEF6, heightDp = 800)
@Composable
private fun IllustrationPreview() {
    ScholrTheme {
        Column(
            modifier = Modifier
                .background(ClayBase)
                .fillMaxSize()
                .padding(PaddingValues(20.dp)),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ClayBrainIllustration()
            ClayFolderIllustration()
            ClayMicroscopeIllustration()
        }
    }
}
