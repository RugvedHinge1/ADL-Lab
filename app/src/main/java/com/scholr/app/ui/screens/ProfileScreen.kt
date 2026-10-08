package com.scholr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.scholr.app.ScholrViewModel
import com.scholr.app.ui.clay.ClayButton
import com.scholr.app.ui.clay.ClayIconButton
import com.scholr.app.ui.clay.ClayPanel
import com.scholr.app.ui.clay.claySurface
import com.scholr.app.ui.theme.ClayBase
import com.scholr.app.ui.theme.ClaySpacing
import com.scholr.app.ui.theme.FieldMedicine
import com.scholr.app.ui.theme.PurpleClayBrush
import com.scholr.app.ui.theme.SurfaceClayBrush
import com.scholr.app.ui.theme.TextBody
import com.scholr.app.ui.theme.TextHeading
import com.scholr.app.ui.theme.TextOnClay

/**
 * Who-you-are + sign out. Deliberately small — a bio/settings page can grow
 * here later, but the one job this screen must not fail at is making sign
 * out an actual, visible, labelled action.
 */
@Composable
fun ProfileScreen(
    vm: ScholrViewModel,
    onBack: () -> Unit,
    onSignedOut: () -> Unit
) {
    val user = vm.currentUser
    val email = user?.email ?: "No email on file"
    val initial = vm.userName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ClayBase)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = ClaySpacing.ListPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClayIconButton(
                    icon = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                    tint = TextHeading
                )
                Spacer(Modifier.width(14.dp))
                Text("Profile", style = MaterialTheme.typography.headlineSmall)
            }

            Spacer(Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .claySurface(cornerRadius = 44.dp, brush = PurpleClayBrush),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.displaySmall,
                    color = TextOnClay
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = vm.userName.ifBlank { "Scholr user" },
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = TextBody,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            ClayPanel(modifier = Modifier.fillMaxWidth(), brush = SurfaceClayBrush) {
                Column {
                    ProfileStatRow("Bookmarked papers", vm.bookmarkedIds.size.toString())
                    Spacer(Modifier.height(14.dp))
                    ProfileStatRow("Collections", vm.collections.size.toString())
                }
            }

            Spacer(Modifier.height(28.dp))

            ClayButton(
                text = "Sign out",
                onClick = {
                    vm.signOut()
                    onSignedOut()
                },
                brush = SurfaceClayBrush,
                contentColor = FieldMedicine,
                leadingIcon = Icons.Rounded.Logout,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(36.dp))
        }
    }
}

@Composable
private fun ProfileStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextBody)
        Text(value, style = MaterialTheme.typography.labelLarge, color = TextHeading)
    }
}
