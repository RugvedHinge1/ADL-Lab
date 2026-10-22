package com.scholr.app

import android.content.Intent
import android.os.Bundle
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.scholr.app.data.alerts.AlertEngine
import com.scholr.app.data.alerts.AlertNotifier
import com.scholr.app.data.alerts.AlertStore
import com.scholr.app.data.alerts.AlertWorker
import java.util.concurrent.TimeUnit
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.scholr.app.data.db.ScholrDatabase
import com.scholr.app.data.firebase.AuthRepository
import com.scholr.app.data.conferences.ConferenceRepository
import com.scholr.app.data.geo.PlaceGeocoder
import com.scholr.app.data.firebase.UserDataRepository
import com.scholr.app.data.network.GeminiRepository
import com.scholr.app.data.network.PaperApiRepository
import com.scholr.app.data.repository.ScholrRepository
import com.scholr.app.ui.theme.ScholrTheme

class MainActivity : ComponentActivity() {

    private val db by lazy { ScholrDatabase.getInstance(applicationContext) }
    private val alertStore by lazy { AlertStore(applicationContext) }
    private val gemini by lazy { GeminiRepository() }
    private val conferenceSource by lazy { ConferenceRepository(applicationContext) }
    private val repo by lazy {
        ScholrRepository(
            db = db,
            auth = AuthRepository(),
            conferenceSource = conferenceSource,
            geocoder = PlaceGeocoder(applicationContext),
            userData = UserDataRepository(),
            paperApi = PaperApiRepository(),
            gemini = gemini,
            alertStore = alertStore,
            alertEngine = AlertEngine(alertStore, gemini, conferenceSource),
            notifier = AlertNotifier(applicationContext, alertStore)
        )
    }
    private val viewModel: ScholrViewModel by viewModels { ScholrViewModel.factory(repo) }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** A tapped alert notification carries a flag asking us to open the alerts screen. */
    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(AlertNotifier.EXTRA_OPEN_ALERTS, false) == true) {
            viewModel.requestOpenAlerts()
            intent.removeExtra(AlertNotifier.EXTRA_OPEN_ALERTS)
        }
    }

    /** Checks for new deadlines / AI alerts every few hours, even when the app is closed. */
    private fun scheduleAlertWork() {
        val request = PeriodicWorkRequestBuilder<AlertWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniquePeriodicWork(AlertWorker.UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        scheduleAlertWork()
        handleIntent(intent)

        setContent {
            ScholrTheme {
                ScholrApp(vm = viewModel)
            }
        }
    }
}