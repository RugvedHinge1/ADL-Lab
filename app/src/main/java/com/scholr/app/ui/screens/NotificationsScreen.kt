package com.scholr.app.ui.screens

import android.Manifest
import android.os.Build
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.scholr.app.ScholrViewModel
import com.scholr.app.data.alerts.AppAlert
import com.scholr.app.ui.clay.ClayButton
import com.scholr.app.ui.clay.ClayIconButton
import com.scholr.app.ui.clay.ClayPanel
import com.scholr.app.ui.clay.claySurface
import com.scholr.app.ui.theme.AcademicGreen
import com.scholr.app.ui.theme.ClayBase
import com.scholr.app.ui.theme.ClayRadius
import com.scholr.app.ui.theme.ClaySpacing
import com.scholr.app.ui.theme.GreenClayBrush
import com.scholr.app.ui.theme.PurpleClayBrush
import com.scholr.app.ui.theme.ScholrPurple
import com.scholr.app.ui.theme.TextBody
import com.scholr.app.ui.theme.TextHeading
import com.scholr.app.ui.theme.TextOnClay

/**
 * The alerts inbox: AI-written nudges and live conference-deadline reminders,
 * plus the switch for real system notifications.
 */
@Composable
fun NotificationsScreen(
    vm: ScholrViewModel,
    onBack: () -> Unit,
    onOpenPaper: (String) -> Unit,
    onOpenConferences: () -> Unit
) {
    val context = LocalContext.current
    var osAllowed by remember { mutableStateOf(notificationsAllowed(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        osAllowed = granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
        vm.onNotificationPermissionAsked(granted)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClayBase)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ClaySpacing.ListPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClayIconButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
                tint = TextHeading
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Alerts", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = if (vm.unreadAlertCount > 0) "${vm.unreadAlertCount} unread" else "You're all caught up",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextBody
                )
            }
            ClayIconButton(
                icon = Icons.Rounded.DoneAll,
                contentDescription = "Mark all as read",
                onClick = { vm.markAllAlertsRead() },
                tint = ScholrPurple
            )
            Spacer(Modifier.width(10.dp))
            ClayIconButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = "Refresh alerts",
                onClick = { vm.loadAlerts(force = true) },
                tint = TextHeading
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = ClaySpacing.ListPadding,
                end = ClaySpacing.ListPadding,
                top = 4.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "push") {
                PushSettingsPanel(
                    enabled = vm.pushEnabled,
                    osAllowed = osAllowed,
                    onToggle = { vm.updatePushEnabled(it) },
                    onAllow = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }

            if (vm.alerts.isEmpty()) {
                item(key = "empty") {
                    EmptyAlerts(loading = vm.alertsLoading)
                }
            } else {
                items(vm.alerts, key = { it.id }) { alert ->
                    AlertCard(
                        alert = alert,
                        unread = !vm.isAlertRead(alert.id),
                        onClick = {
                            vm.markAlertRead(alert.id)
                            when (alert.refType) {
                                AppAlert.REF_PAPER -> onOpenPaper(alert.refId)
                                AppAlert.REF_CONFERENCE -> onOpenConferences()
                            }
                        }
                    )
                }
                if (vm.alertsLoading) {
                    item(key = "loading") {
                        Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ScholrPurple, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun notificationsAllowed(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        android.content.pm.PackageManager.PERMISSION_GRANTED
    ) return false
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
}

@Composable
private fun PushSettingsPanel(
    enabled: Boolean,
    osAllowed: Boolean,
    onToggle: (Boolean) -> Unit,
    onAllow: () -> Unit
) {
    ClayPanel(modifier = Modifier.fillMaxWidth(), contentPadding = 16.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Push notifications", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Get a heads-up for conference deadlines and AI research nudges, even when Scholr is closed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextBody
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = AcademicGreen,
                        checkedThumbColor = Color.White
                    )
                )
            }
            if (enabled && !osAllowed) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Notifications are blocked for Scholr on this phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextBody
                )
                Spacer(Modifier.height(10.dp))
                ClayButton(
                    text = "Allow notifications",
                    onClick = onAllow,
                    brush = GreenClayBrush,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AlertCard(alert: AppAlert, unread: Boolean, onClick: () -> Unit) {
    val isAi = alert.kind == AppAlert.KIND_AI
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .claySurface(cornerRadius = ClayRadius.Card)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .claySurface(
                    cornerRadius = 16.dp,
                    brush = if (isAi) PurpleClayBrush else GreenClayBrush
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isAi) Icons.Rounded.AutoAwesome else Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = TextOnClay,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    alert.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextHeading,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (unread) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(ScholrPurple)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(alert.body, style = MaterialTheme.typography.bodySmall, color = TextBody)
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isAi) "AI insight · " + timeAgo(alert.createdAt) else "Conference deadline",
                style = MaterialTheme.typography.labelSmall,
                color = if (isAi) ScholrPurple else AcademicGreen
            )
        }
    }
}

private fun timeAgo(createdAt: Long): String {
    val age = System.currentTimeMillis() - createdAt
    return if (age < DateUtils.MINUTE_IN_MILLIS) "just now"
    else DateUtils.getRelativeTimeSpanString(createdAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
}

@Composable
private fun EmptyAlerts(loading: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (loading) {
            CircularProgressIndicator(color = ScholrPurple)
            Spacer(Modifier.height(16.dp))
            Text("Writing your alerts…", style = MaterialTheme.typography.bodyMedium, color = TextBody)
        } else {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .claySurface(cornerRadius = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.NotificationsNone, null, tint = TextBody, modifier = Modifier.size(38.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text("No alerts yet", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Pick a few research interests and save some papers — Scholr will flag deadlines and " +
                    "ideas that fit them. Tap refresh to check now.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextBody,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}
