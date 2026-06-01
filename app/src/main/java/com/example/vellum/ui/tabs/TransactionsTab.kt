package com.example.vellum.ui.tabs

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vellum.theme.*
import com.example.vellum.ui.components.ParchmentBackground
import com.example.vellum.ui.components.TransactionRow
import com.example.vellum.ui.dialogs.FilterAccountDialog
import com.example.vellum.ui.dialogs.FilterCategoryDialog
import com.example.vellum.ui.dialogs.ShareExportDialog
import com.example.vellum.ui.main.MainScreenViewModel
import com.example.vellum.data.local.TransactionEntity
import androidx.compose.ui.graphics.Color

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun TransactionsTab(
    viewModel: MainScreenViewModel,
    showShareDialog: Boolean,
    onDismissShareDialog: () -> Unit,
    onShareClicked: () -> Unit,
    showCategoryFilterDialog: Boolean,
    onDismissCategoryFilterDialog: () -> Unit,
    onCategoryFilterClicked: () -> Unit,
    showAccountFilterDialog: Boolean,
    onDismissAccountFilterDialog: () -> Unit,
    onAccountFilterClicked: () -> Unit,
    onNavigate: (androidx.navigation3.runtime.NavKey) -> Unit
) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val showNote = preferences["show_notes"] != "Off"
    val isOutlined = preferences["category_icon_style"] == "Outlined"
    val timePeriodPref by viewModel.timePeriod.collectAsState()
    val isNavigable = timePeriodPref != "All" && timePeriodPref != "Custom"

    val currencySymbol = when (val sym = preferences["currency_symbol"]) {
        "Default" -> "₹"
        "INR" -> "₹"
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> sym ?: "₹"
    }

    var exportCsvText by remember { mutableStateOf<String?>(null) }
    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { os ->
                    os.write((exportCsvText ?: "").toByteArray())
                }
                Toast.makeText(context, "CSV saved successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving CSV: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var exportPdfBytes by remember { mutableStateOf<ByteArray?>(null) }
    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { os ->
                    os.write(exportPdfBytes ?: byteArrayOf())
                }
                Toast.makeText(context, "PDF saved successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Transaction list space
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                            .border(1.dp, ParchmentLine, RoundedCornerShape(12.dp))
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Press the '+' icon to add your first transaction",
                            color = ParchmentDarkBrown.copy(alpha = 0.8f),
                            fontFamily = ParchmentFontFamily,
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(transactions) { tx ->
                            val category = categories.find { it.id == tx.categoryId }
                            val categoryIcon = category?.icon ?: ""
                            val liveCategoryName = category?.name ?: tx.categoryName
                            val account = accounts.find { it.id == tx.accountId }
                            val liveAccountName = account?.name ?: "(Deleted Account)"
                            TransactionRow(
                                tx = tx,
                                showNote = showNote,
                                currencySymbol = currencySymbol,
                                categoryIcon = categoryIcon,
                                categoryName = liveCategoryName,
                                accountName = liveAccountName,
                                isOutlined = isOutlined,
                                onClick = {
                                    onNavigate(com.example.vellum.AddEditTransaction(transactionId = tx.id, predefinedType = tx.type))
                                }
                            ) {
                                viewModel.deleteTransaction(tx)
                                Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

            val activeCat by viewModel.selectedFilterCategory.collectAsState()
            val activeAcc by viewModel.selectedFilterAccount.collectAsState()

            // Bottom navigation & export buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Navigation arrows
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.navigatePeriod(false) },
                        enabled = isNavigable
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Prev",
                            tint = if (isNavigable) ParchmentDarkBrown else Color.Transparent
                        )
                    }
                    IconButton(
                        onClick = { viewModel.navigatePeriod(true) },
                        enabled = isNavigable
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Next",
                            tint = if (isNavigable) ParchmentDarkBrown else Color.Transparent
                        )
                    }
                }

                // Share / Filter buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onShareClicked) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export",
                            tint = ParchmentDarkBrown
                        )
                    }
                    IconButton(onClick = onCategoryFilterClicked) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Category Filter",
                            tint = if (activeCat != null) ParchmentBlueText else ParchmentDarkBrown
                        )
                    }
                    IconButton(onClick = onAccountFilterClicked) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Account Filter",
                            tint = if (activeAcc != null) ParchmentBlueText else ParchmentDarkBrown
                        )
                    }
                }
            }
        }
    }

    if (showShareDialog) {
        ShareExportDialog(
            accounts = accounts,
            onDismiss = onDismissShareDialog,
            onExportCsv = { accountFilter, start, end, label ->
                val allTxList = viewModel.allTransactions.value.filter { tx ->
                    !tx.isDeleted &&
                    (accountFilter == null || tx.accountId == accountFilter.id) &&
                    (tx.timestamp in start..end)
                }
                exportCsvText = com.example.vellum.data.ExportManager.exportToCsv(allTxList)
                val accountNameClean = (accountFilter?.name ?: "all").replace(" ", "_").lowercase()
                val labelClean = label.replace(" ", "_").lowercase()
                saveCsvLauncher.launch("vellum_${accountNameClean}_transactions_${labelClean}.csv")
            },
            onExportPdf = { accountFilter, start, end, label ->
                val allTxList = viewModel.allTransactions.value.filter { tx ->
                    !tx.isDeleted &&
                    (accountFilter == null || tx.accountId == accountFilter.id) &&
                    (tx.timestamp in start..end)
                }
                exportPdfBytes = com.example.vellum.data.ExportManager.exportToPdf(
                    context = context,
                    transactions = allTxList,
                    categories = categories,
                    accounts = accounts,
                    accountFilter = accountFilter?.name ?: "All Accounts",
                    periodLabel = label,
                    currencySymbol = currencySymbol
                )
                val accountNameClean = (accountFilter?.name ?: "all").replace(" ", "_").lowercase()
                val labelClean = label.replace(" ", "_").lowercase()
                savePdfLauncher.launch("vellum_${accountNameClean}_report_${labelClean}.pdf")
            }
        )
    }

    if (showCategoryFilterDialog) {
        FilterCategoryDialog(
            viewModel = viewModel,
            onDismiss = onDismissCategoryFilterDialog
        )
    }

    if (showAccountFilterDialog) {
        FilterAccountDialog(
            viewModel = viewModel,
            onDismiss = onDismissAccountFilterDialog
        )
    }
}
