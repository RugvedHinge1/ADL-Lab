package com.scholr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.scholr.app.data.db.ScholrDatabase
import com.scholr.app.data.firebase.AuthRepository
import com.scholr.app.data.firebase.ContentRepository
import com.scholr.app.data.firebase.UserDataRepository
import com.scholr.app.data.network.GeminiRepository
import com.scholr.app.data.network.PaperApiRepository
import com.scholr.app.data.repository.ScholrRepository
import com.scholr.app.ui.theme.ScholrTheme

class MainActivity : ComponentActivity() {

    private val db by lazy { ScholrDatabase.getInstance(applicationContext) }
    private val repo by lazy {
        ScholrRepository(
            db = db,
            auth = AuthRepository(),
            content = ContentRepository(),
            userData = UserDataRepository(),
            paperApi = PaperApiRepository(),
            gemini = GeminiRepository()
        )
    }
    private val viewModel: ScholrViewModel by viewModels { ScholrViewModel.factory(repo) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            ScholrTheme {
                ScholrApp(vm = viewModel)
            }
        }
    }
}