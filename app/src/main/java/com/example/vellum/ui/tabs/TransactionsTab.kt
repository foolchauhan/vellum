package com.example.vellum.ui.tabs

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import java.util.Calendar

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

    val activeCat by viewModel.selectedFilterCategory.collectAsState()
    val activeAcc by viewModel.selectedFilterAccount.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val filteredTransactions = remember(transactions, searchQuery) {
        if (searchQuery.isEmpty()) {
            transactions
        } else {
            val query = searchQuery.lowercase(java.util.Locale.US).trim()
            
            // 1. Amount parser (e.g. "over 50", "under 100", "> 20")
            var minAmount = 0.0
            var maxAmount = Double.MAX_VALUE
            
            val overRegex = Regex("(?:over|>|greater than)\\s*(\\d+(?:\\.\\d+)?)")
            val underRegex = Regex("(?:under|<|less than)\\s*(\\d+(?:\\.\\d+)?)")
            
            overRegex.find(query)?.let { match ->
                minAmount = match.groupValues[1].toDoubleOrNull() ?: 0.0
            }
            underRegex.find(query)?.let { match ->
                maxAmount = match.groupValues[1].toDoubleOrNull() ?: Double.MAX_VALUE
            }

            // 2. Date parser (e.g. "today", "yesterday", "week")
            var startTimestamp = 0L
            var endTimestamp = Long.MAX_VALUE
            val now = System.currentTimeMillis()
            
            if (query.contains("today")) {
                startTimestamp = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                endTimestamp = startTimestamp + 24 * 60 * 60 * 1000L - 1
            } else if (query.contains("yesterday")) {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                startTimestamp = cal.timeInMillis
                endTimestamp = startTimestamp + 24 * 60 * 60 * 1000L - 1
            } else if (query.contains("week") || query.contains("last 7 days")) {
                startTimestamp = now - 7 * 24 * 60 * 60 * 1000L
            } else if (query.contains("month") || query.contains("last 30 days")) {
                startTimestamp = now - 30 * 24 * 60 * 60 * 1000L
            } else if (query.contains("year")) {
                startTimestamp = now - 365 * 24 * 60 * 60 * 1000L
            }

            // Clean query text for semantic matching
            val textQuery = query
                .replace(overRegex, "")
                .replace(underRegex, "")
                .replace("today", "")
                .replace("yesterday", "")
                .replace("last week", "")
                .replace("last month", "")
                .replace("last year", "")
                .replace("week", "")
                .replace("month", "")
                .replace("year", "")
                .trim()

            transactions.filter { tx ->
                val splitsList = try {
                    if (tx.splits.isNotEmpty()) kotlinx.serialization.json.Json.decodeFromString<List<com.example.vellum.data.local.TransactionSplit>>(tx.splits) else emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
                val amount = if (splitsList.isNotEmpty()) splitsList.sumOf { it.amount } else tx.amount
                
                val amountMatch = amount in minAmount..maxAmount
                
                val timeMatch = if (startTimestamp > 0 || endTimestamp < Long.MAX_VALUE) {
                    tx.timestamp in startTimestamp..endTimestamp
                } else {
                    true
                }

                val textMatch = if (textQuery.isNotEmpty()) {
                    tx.categoryName.lowercase(java.util.Locale.US).contains(textQuery) ||
                    tx.accountName.lowercase(java.util.Locale.US).contains(textQuery) ||
                    tx.note.lowercase(java.util.Locale.US).contains(textQuery) ||
                    tx.type.lowercase(java.util.Locale.US).contains(textQuery) ||
                    com.example.vellum.data.SemanticMatcher.isSemanticMatch(textQuery, tx.categoryName) ||
                    com.example.vellum.data.SemanticMatcher.isSemanticMatch(textQuery, tx.note)
                } else {
                    true
                }

                amountMatch && timeMatch && textMatch
            }
        }
    }

    val aiAnswer = remember(filteredTransactions, searchQuery, currencySymbol) {
        if (searchQuery.isBlank()) return@remember null
        
        val query = searchQuery.lowercase(java.util.Locale.US).trim()
        
        val isQuestion = query.contains("?") || 
                         query.contains("how much") || 
                         query.contains("how many") || 
                         query.contains("total") || 
                         query.contains("sum") || 
                         query.contains("average") || 
                         query.contains("avg") || 
                         query.contains("count") || 
                         query.contains("spent") || 
                         query.contains("spend") || 
                         query.contains("earned") || 
                         query.contains("earn") || 
                         query.contains("balance")
                         
        if (!isQuestion) return@remember null
        
        val matchedCount = filteredTransactions.size
        val totalSpent = filteredTransactions.filter { it.type == "EXPENSE" && !it.isDeleted }.sumOf { it.amount }
        val totalEarned = filteredTransactions.filter { it.type == "INCOME" && !it.isDeleted }.sumOf { it.amount }
        
        when {
            query.contains("balance") -> {
                val net = totalEarned - totalSpent
                val netSign = if (net >= 0) "+" else ""
                "Net Balance for these transactions is $netSign$currencySymbol${String.format(java.util.Locale.US, "%.2f", net)} (Total Income: $currencySymbol${String.format(java.util.Locale.US, "%.2f", totalEarned)}, Total Expense: $currencySymbol${String.format(java.util.Locale.US, "%.2f", totalSpent)})."
            }
            query.contains("average") || query.contains("avg") -> {
                val totalAmount = filteredTransactions.filter { !it.isDeleted }.sumOf { it.amount }
                val avg = if (matchedCount > 0) totalAmount / matchedCount else 0.0
                "Average transaction amount is $currencySymbol${String.format(java.util.Locale.US, "%.2f", avg)} across $matchedCount matched transaction(s)."
            }
            query.contains("count") || query.contains("how many") || query.contains("number of") -> {
                "Found $matchedCount transaction(s) matching your query."
            }
            query.contains("spent") || query.contains("spend") || query.contains("expense") -> {
                if (matchedCount == 0) {
                    "You spent ${currencySymbol}0.00 (No matching transactions found)."
                } else {
                    "Total spending in this search is $currencySymbol${String.format(java.util.Locale.US, "%.2f", totalSpent)} (across $matchedCount transaction(s))."
                }
            }
            query.contains("earned") || query.contains("earn") || query.contains("income") -> {
                if (matchedCount == 0) {
                    "You earned ${currencySymbol}0.00 (No matching transactions found)."
                } else {
                    "Total earnings in this search are $currencySymbol${String.format(java.util.Locale.US, "%.2f", totalEarned)} (across $matchedCount transaction(s))."
                }
            }
            else -> {
                val totalAmount = filteredTransactions.filter { !it.isDeleted }.sumOf { it.amount }
                if (totalEarned > 0 && totalSpent > 0) {
                    "Total income: $currencySymbol${String.format(java.util.Locale.US, "%.2f", totalEarned)}, Total expense: $currencySymbol${String.format(java.util.Locale.US, "%.2f", totalSpent)} (across $matchedCount transaction(s))."
                } else {
                    "Total sum for matched transactions is $currencySymbol${String.format(java.util.Locale.US, "%.2f", totalAmount)} ($matchedCount transaction(s))."
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Natural Language Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .border(1.dp, ParchmentLine, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔍",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search (e.g., 'Food over 50')",
                            color = ParchmentDarkBrown.copy(alpha = 0.5f),
                            fontFamily = ParchmentFontFamily,
                            fontSize = 16.sp
                        )
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = ParchmentDarkBrown,
                            fontFamily = ParchmentFontFamily,
                            fontSize = 16.sp
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    Text(
                        text = "✕",
                        color = ParchmentDarkBrown.copy(alpha = 0.6f),
                        fontFamily = ParchmentFontFamily,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable { searchQuery = "" }
                            .padding(start = 8.dp)
                    )
                }
            }

            aiAnswer?.let { answer ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = ParchmentBlueText.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.dp, ParchmentBlueText.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "💡",
                            fontSize = 20.sp
                        )
                        Column {
                            Text(
                                text = "Chalkboard AI Answer",
                                fontFamily = ParchmentFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ParchmentBlueText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = answer,
                                fontFamily = ParchmentFontFamily,
                                fontSize = 14.sp,
                                color = ParchmentDarkBrown
                            )
                        }
                    }
                }
            }

            if (activeCat != null || activeAcc != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filters:",
                        color = ParchmentDarkBrown.copy(alpha = 0.6f),
                        fontFamily = ParchmentFontFamily,
                        fontSize = 14.sp
                    )
                    activeCat?.let { cat ->
                        Row(
                            modifier = Modifier
                                .border(1.dp, ParchmentLine, RoundedCornerShape(16.dp))
                                .clickable { viewModel.setFilterCategory(null) }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Category: ${cat.name}",
                                color = ParchmentDarkBrown,
                                fontFamily = ParchmentFontFamily,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "✕",
                                color = ParchmentDarkBrown.copy(alpha = 0.6f),
                                fontFamily = ParchmentFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    activeAcc?.let { acc ->
                        Row(
                            modifier = Modifier
                                .border(1.dp, ParchmentLine, RoundedCornerShape(16.dp))
                                .clickable { viewModel.setFilterAccount(null) }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Account: ${acc.name}",
                                color = ParchmentDarkBrown,
                                fontFamily = ParchmentFontFamily,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "✕",
                                color = ParchmentDarkBrown.copy(alpha = 0.6f),
                                fontFamily = ParchmentFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

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
                } else if (filteredTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                            .border(1.dp, ParchmentLine, RoundedCornerShape(12.dp))
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "No matching transactions found on the blackboard.",
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
                        items(filteredTransactions) { tx ->
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
