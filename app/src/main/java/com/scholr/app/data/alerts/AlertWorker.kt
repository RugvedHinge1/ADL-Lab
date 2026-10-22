package com.scholr.app.data.alerts

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.scholr.app.data.SampleData
import com.scholr.app.data.conferences.ConferenceRepository
import com.scholr.app.data.firebase.UserDataRepository
import com.scholr.app.data.network.GeminiRepository

/**
 * Runs in the background (even with the app closed) every few hours: refreshes
 * the alert list for the signed-in user and posts a system notification for
 * anything new.
 */
class AlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val store = AlertStore(applicationContext)
        if (!store.pushEnabled) return Result.success()
        val user = FirebaseAuth.getInstance().currentUser ?: return Result.success()

        return try {
            val interestIds = UserDataRepository().getSelectedInterests(user.uid)
            val labels = SampleData.interests.filter { it.id in interestIds }.map { it.label }

            val engine = AlertEngine(store, GeminiRepository(), ConferenceRepository(applicationContext))
            val alerts = engine.refresh(interestLabels = labels, savedTitles = emptyList())
            AlertNotifier(applicationContext, store).notifyNew(alerts)
            Result.success()
        } catch (e: Exception) {
            Log.w("AlertWorker", "alert refresh failed", e)
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_NAME = "scholr-alerts"
    }
}
