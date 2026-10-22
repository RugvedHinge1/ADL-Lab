package com.scholr.app.data.alerts

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.scholr.app.MainActivity
import com.scholr.app.R

/** Posts real Android notifications for alerts the user hasn't been told about yet. */
class AlertNotifier(context: Context, private val store: AlertStore) {

    private val appContext = context.applicationContext

    fun canPost(): Boolean {
        if (!store.pushEnabled) return false
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(appContext).areNotificationsEnabled()
    }

    /** Shows at most [limit] not-yet-notified alerts and remembers them so they never repeat. */
    fun notifyNew(alerts: List<AppAlert>, limit: Int = 3) {
        val fresh = alerts.filter { it.id !in store.notifiedIds }
        if (fresh.isEmpty()) return
        if (!canPost()) return // leave them un-notified so they fire once permission is granted

        ensureChannel()
        val manager = NotificationManagerCompat.from(appContext)
        val shown = fresh.take(limit)
        shown.forEach { alert ->
            val pending = openAlertsIntent(alert.id.hashCode())
            val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(alert.title)
                .setContentText(alert.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(alert.body))
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setGroup(GROUP_KEY)
                .setContentIntent(pending)
                .build()
            @Suppress("MissingPermission") // canPost() checked it above
            manager.notify(alert.id.hashCode(), notification)
        }
        // Our own group summary, so tapping the collapsed stack also lands on the Alerts screen
        // (the system's auto-generated summary would just launch the app's front door).
        val summary = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Scholr alerts")
            .setContentText("${shown.size} new")
            .setStyle(
                NotificationCompat.InboxStyle().also { inbox ->
                    shown.forEach { inbox.addLine("${it.title} - ${it.body}") }
                    inbox.setSummaryText("${shown.size} new alerts")
                }
            )
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setContentIntent(openAlertsIntent(SUMMARY_ID))
            .build()
        @Suppress("MissingPermission")
        manager.notify(SUMMARY_ID, summary)

        // Only what was actually shown counts as handled; the rest follows on the next check.
        store.notifiedIds = store.notifiedIds + shown.map { it.id }
    }

    private fun openAlertsIntent(requestCode: Int): PendingIntent {
        val open = Intent(appContext, MainActivity::class.java).apply {
            putExtra(EXTRA_OPEN_ALERTS, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            appContext, requestCode, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val channel = NotificationChannel(CHANNEL_ID, "Research alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Conference deadlines and AI-written research nudges"
        }
        appContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "research_alerts"
        const val EXTRA_OPEN_ALERTS = "open_alerts"
        private const val GROUP_KEY = "scholr_alerts_group"
        private const val SUMMARY_ID = 7001
    }
}
