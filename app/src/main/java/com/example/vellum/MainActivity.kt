package com.example.vellum

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vellum.data.DefaultDataRepository
import com.example.vellum.data.local.VellumDatabase
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
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock

class MainActivity : androidx.fragment.app.FragmentActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private var onSignInResult: ((email: String, displayName: String?, photoUrl: String?) -> Unit)? = null

    private val quickAddCategoryState = mutableStateOf<String?>(null)

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
        } catch (e: ApiException) {
            Log.e("MainActivity", "Google Sign-In failed: statusCode=${e.statusCode}, message=${e.message}")
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

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleQuickAddIntent(intent)
    }

    private fun handleQuickAddIntent(intent: android.content.Intent?) {
        val category = intent?.getStringExtra("quick_add_category")
        if (category != null) {
            quickAddCategoryState.value = category
            intent.removeExtra("quick_add_category")
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e("MainActivity", "Biometric error: $errString")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Vellum Lock")
            .setSubtitle("Unlock to access your chalkboard finances")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.vellum.data.ReminderScheduler.createNotificationChannel(this)

        // Handle quick-add intent if clicked when app was closed
        handleQuickAddIntent(intent)

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
            
            var isUnlocked by remember { mutableStateOf(false) }

            LaunchedEffect(preferences) {
                if (preferences["biometric_lock"] == "On" && !isUnlocked) {
                    showBiometricPrompt {
                        isUnlocked = true
                    }
                }
            }

            val themePref = preferences["theme"] ?: when (preferences["dark_theme"]) {
                "On" -> "Dark"
                "Off" -> "Light"
                "System" -> "System"
                else -> "System"
            }
            val handwritingPref = preferences["handwriting_style"] ?: "Default"

            val allTransactions by repository.getTransactionsFlow().collectAsState(initial = emptyList())
            LaunchedEffect(allTransactions, themePref) {
                ChalkboardWidgetProvider.triggerUpdate(applicationContext)
                QuickAddWidgetProvider.triggerUpdate(applicationContext)
                BudgetProgressWidgetProvider.triggerUpdate(applicationContext)
                WeeklyTrendWidgetProvider.triggerUpdate(applicationContext)
                RecentTransactionsWidgetProvider.triggerUpdate(applicationContext)
            }

            VellumTheme(theme = themePref, handwritingStyle = handwritingPref) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isLocked = preferences["biometric_lock"] == "On" && !isUnlocked
                    if (isLocked) {
                        ChalkboardLockScreen(onUnlockClick = {
                            showBiometricPrompt {
                                isUnlocked = true
                            }
                        })
                    } else {
                        val quickAddCategory by quickAddCategoryState
                        MainNavigation(
                            repository = repository,
                            initialQuickAddCategory = quickAddCategory,
                            onQuickAddHandled = {
                                quickAddCategoryState.value = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChalkboardLockScreen(onUnlockClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.vellum.theme.ChalkboardSlate),
        contentAlignment = Alignment.Center
    ) {
        com.example.vellum.ui.components.ChalkboardBackground(modifier = Modifier.fillMaxSize()) {}
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = com.example.vellum.theme.ChalkWhite,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Vellum is Locked",
                color = com.example.vellum.theme.ChalkWhite,
                fontFamily = com.example.vellum.theme.ParchmentTitleFontFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Please authenticate to access your chalkboard finances",
                color = com.example.vellum.theme.ChalkGray,
                fontFamily = com.example.vellum.theme.ParchmentFontFamily,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onUnlockClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.example.vellum.theme.ChalkBlue,
                    contentColor = com.example.vellum.theme.ChalkboardSlate
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(48.dp)
            ) {
                Text(
                    text = "Unlock",
                    fontFamily = com.example.vellum.theme.ParchmentFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
