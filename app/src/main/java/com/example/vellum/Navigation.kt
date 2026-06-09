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
import com.example.vellum.ui.main.AddEditTransactionScreen
import com.example.vellum.ui.main.AddEditCategoryScreen
import com.example.vellum.ui.main.AddEditAccountScreen
import com.example.vellum.ui.components.ParchmentBackground

@Composable
fun MainNavigation(
    repository: DataRepository,
    initialQuickAddCategory: String? = null,
    onQuickAddHandled: () -> Unit = {}
) {
    val backStack = rememberNavBackStack(Main)
    val sharedViewModel: MainScreenViewModel = viewModel { MainScreenViewModel(repository) }

    // Handle Quick Add intent trigger
    androidx.compose.runtime.LaunchedEffect(initialQuickAddCategory) {
        initialQuickAddCategory?.let { category ->
            backStack.add(AddEditTransaction(preselectedCategoryName = category))
            onQuickAddHandled()
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && account.email != null) {
            sharedViewModel.signIn(
                account.email!!,
                account.displayName,
                account.photoUrl?.toString(),
                isRestore = true
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
            entry<AddEditTransaction> { key ->
                AddEditTransactionScreen(
                    viewModel = sharedViewModel,
                    predefinedType = key.predefinedType,
                    transactionId = key.transactionId,
                    preselectedCategoryName = key.preselectedCategoryName,
                    onBack = { backStack.removeLastOrNull() },
                    onNavigate = { navKey -> backStack.add(navKey) },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<AddEditCategory> { key ->
                AddEditCategoryScreen(
                    viewModel = sharedViewModel,
                    predefinedType = key.predefinedType,
                    categoryId = key.categoryId,
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<AddEditAccount> { key ->
                AddEditAccountScreen(
                    viewModel = sharedViewModel,
                    accountId = key.accountId,
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
        }
    )
}
