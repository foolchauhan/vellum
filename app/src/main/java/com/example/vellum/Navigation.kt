package com.example.vellum

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.vellum.data.DataRepository
import com.example.vellum.ui.main.MainScreen
import com.example.vellum.ui.main.MainScreenViewModel
import com.example.vellum.ui.main.SettingsScreen
import com.example.vellum.ui.components.ParchmentBackground

@Composable
fun MainNavigation(repository: DataRepository) {
    val backStack = rememberNavBackStack(Main)
    val sharedViewModel: MainScreenViewModel = viewModel { MainScreenViewModel(repository) }

    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && account.email != null) {
            sharedViewModel.signIn(
                account.email!!,
                account.displayName,
                account.photoUrl?.toString()
            )
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Main> {
                MainScreen(
                    onItemClick = { navKey -> backStack.add(navKey) },
                    viewModel = sharedViewModel,
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<Settings> {
                ParchmentBackground(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        viewModel = sharedViewModel,
                        onBackClicked = { backStack.removeLastOrNull() },
                        modifier = Modifier.safeDrawingPadding()
                    )
                }
            }
        }
    )
}
