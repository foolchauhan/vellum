package com.example.vellum

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.vellum.data.DefaultDataRepository
import com.example.vellum.data.local.VellumDatabase
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.vellum.theme.VellumTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private var onSignInResult: ((email: String, displayName: String?, photoUrl: String?) -> Unit)? = null

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null && account.email != null) {
                onSignInResult?.invoke(
                    account.email!!,
                    account.displayName,
                    account.photoUrl?.toString()
                )
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Google Sign-In failed", e)
        }
    }

    fun triggerGoogleSignIn(onResult: (email: String, displayName: String?, photoUrl: String?) -> Unit) {
        this.onSignInResult = onResult
        signInLauncher.launch(googleSignInClient.signInIntent)
    }

    fun triggerGoogleSignOut(onCompleted: () -> Unit) {
        googleSignInClient.signOut().addOnCompleteListener {
            onCompleted()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.vellum.data.ReminderScheduler.createNotificationChannel(this)

        // Initialize Google Sign-In Client options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestIdToken("688785211099-5v9rfbti9mnoenhfc49us4mk1321nade.apps.googleusercontent.com")
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize Local SQLite Database and Repository
        val database = VellumDatabase.getInstance(applicationContext)
        val repository = DefaultDataRepository(database)

        // Fresh debug install clean slate
        if (BuildConfig.DEBUG) {
            val prefs = getSharedPreferences("vellum_debug_prefs", MODE_PRIVATE)
            if (!prefs.getBoolean("first_run_debug", false)) {
                googleSignInClient.signOut()
                CoroutineScope(Dispatchers.IO).launch {
                    database.clearAllTables()
                    VellumDatabase.prepopulateDatabase(database)
                    prefs.edit().putBoolean("first_run_debug", true).apply()
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            val preferencesList by repository.getPreferencesFlow().collectAsState(initial = emptyList())
            val preferences = remember(preferencesList) {
                preferencesList.associate { it.key to it.value }
            }
            // Backward compatibility fallback:
            val themePref = preferences["theme"] ?: when (preferences["dark_theme"]) {
                "On" -> "Dark"
                "Off" -> "Light"
                "System" -> "System"
                else -> "System"
            }
            VellumTheme(theme = themePref) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(repository)
                }
            }
        }
    }
}
