package com.example.vellum.ui.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.vellum.AddEditCategory
import com.example.vellum.theme.*
import com.example.vellum.ui.components.*
import com.example.vellum.ui.components.HorizontalDivider
import com.example.vellum.data.local.TransactionEntity
import com.example.vellum.data.local.TransactionSplit
import com.example.vellum.data.local.CategoryEntity
import com.example.vellum.data.local.AccountEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import com.example.vellum.ui.dialogs.ChalkboardDatePickerDialog
import com.example.vellum.ui.dialogs.CreateNoteDialog
import java.text.SimpleDateFormat
import java.util.*
import android.net.Uri
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    viewModel: MainScreenViewModel,
    predefinedType: String,
    transactionId: String? = null,
    preselectedCategoryName: String? = null,
    onBack: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val currencySymbol = remember(preferences) {
        when (val sym = preferences["currency_symbol"]) {
            "Default" -> "₹"
            "INR" -> "₹"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> sym ?: "₹"
        }
    }

    val transactionToEdit = remember(transactionId, allTransactions) {
        transactionId?.let { id -> allTransactions.find { it.id == id } }
    }

    var type by remember {
        mutableStateOf(
            transactionToEdit?.type 
                ?: preselectedCategoryName?.let { name ->
                    categories.find { it.name.equals(name, ignoreCase = true) }?.type
                }
                ?: predefinedType
        )
    }
    var amount by remember { mutableStateOf(transactionToEdit?.amount?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var note by remember { mutableStateOf(transactionToEdit?.note ?: "") }
    var repeat by remember { mutableStateOf(false) }

    val categoryUsageCounts = remember(allTransactions) {
        allTransactions.groupBy { it.categoryId }.mapValues { it.value.size }
    }
    val filteredCategories = remember(categories, type, categoryUsageCounts) {
        categories.filter { it.type == type }
            .sortedByDescending { categoryUsageCounts[it.id] ?: 0 }
    }

    var isSplitMode by remember { mutableStateOf(transactionToEdit?.splits?.isNotEmpty() == true) }
    var splitsList by remember {
        mutableStateOf<List<TransactionSplit>>(
            if (transactionToEdit?.splits?.isNotEmpty() == true) {
                try {
                    Json.decodeFromString<List<TransactionSplit>>(transactionToEdit.splits)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        )
    }

    var splitCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var splitAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var splitAmount by remember { mutableStateOf("") }
    var splitNote by remember { mutableStateOf("") }

    var splitCategoryMenuExpanded by remember { mutableStateOf(false) }
    var splitAccountMenuExpanded by remember { mutableStateOf(false) }

    var isCategoryManuallySelected by remember { mutableStateOf(false) }

    var selectedCategory by remember {
        mutableStateOf(
            if (transactionToEdit != null) {
                categories.find { it.id == transactionToEdit.categoryId }
            } else if (preselectedCategoryName != null) {
                categories.find { it.name.equals(preselectedCategoryName, ignoreCase = true) }
            } else {
                null
            }
        )
    }
    var selectedAccount by remember {
        mutableStateOf(
            if (transactionToEdit != null) {
                accounts.find { it.id == transactionToEdit.accountId } ?: accounts.firstOrNull()
            } else {
                val activeFilterAcc = viewModel.selectedFilterAccount.value
                if (activeFilterAcc != null) {
                    accounts.find { it.id == activeFilterAcc.id }
                } else {
                    // Fall back to most used account
                    allTransactions
                        .filter { !it.isDeleted }
                        .groupBy { it.accountId }
                        .maxByOrNull { it.value.size }
                        ?.key
                        ?.let { accountId -> accounts.find { it.id == accountId } }
                } ?: accounts.find { it.isDefault } ?: accounts.firstOrNull()
            }
        )
    }

    LaunchedEffect(categories, accounts, type) {
        if (selectedCategory == null || selectedCategory?.type != type) {
            val lastTx = allTransactions
                .filter { !it.isDeleted && it.type == type }
                .maxByOrNull { it.timestamp }
            val autoCat = lastTx?.let { tx ->
                categories.find { it.id == tx.categoryId }
            } ?: filteredCategories.firstOrNull()
            selectedCategory = autoCat
            isCategoryManuallySelected = false
        }
        if (splitCategory == null || splitCategory?.type != type) {
            splitCategory = filteredCategories.firstOrNull()
        }
        if (splitAccount == null) {
            splitAccount = accounts.find { it.isDefault } ?: accounts.firstOrNull()
        }
    }

    var selectedTimestamp by remember { mutableStateOf(transactionToEdit?.timestamp ?: System.currentTimeMillis()) }

    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var accountMenuExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = selectedTimestamp
    val dateString = remember(selectedTimestamp) {
        val format = SimpleDateFormat("dd MMM yyyy", Locale.US)
        format.format(Date(selectedTimestamp))
    }

    var showCustomDatePicker by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var activeHelpTip by remember { mutableStateOf<String?>(null) }

    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    var showScanOptionsDialog by remember { mutableStateOf(false) }

    fun processReceiptImage(imageUri: Uri) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val lines = visionText.textBlocks.flatMap { block -> block.lines.map { line -> line.text } }
                    val priceRegex = Regex("""\b\d+\.\d{2}\b""")
                    val prices = lines.flatMap { line ->
                        priceRegex.findAll(line).map { match -> match.value.toDoubleOrNull() ?: 0.0 }
                    }.toList()
                    if (prices.isNotEmpty()) {
                        val maxPrice = prices.maxOrNull() ?: 0.0
                        if (maxPrice > 0.0) {
                            amount = maxPrice.toString()
                        }
                    }
                    val fullText = lines.joinToString(" ")
                    var matchedCategory: CategoryEntity? = null
                    for (cat in filteredCategories) {
                        if (com.example.vellum.data.SemanticMatcher.isSemanticMatch(cat.name, fullText, 0.3f)) {
                            matchedCategory = cat
                            break
                        }
                    }

                    val merchantLine = lines.firstOrNull { line ->
                        line.length > 2 && line.any { c -> c.isLetter() } &&
                        !line.contains("receipt", ignoreCase = true) &&
                        !line.contains("welcome", ignoreCase = true)
                    }
                    if (merchantLine != null) {
                        note = merchantLine.trim()
                        if (matchedCategory == null) {
                            for (cat in filteredCategories) {
                                if (com.example.vellum.data.SemanticMatcher.isSemanticMatch(merchantLine, cat.name, 0.3f)) {
                                    matchedCategory = cat
                                    break
                                }
                            }
                        }
                    }
                    if (matchedCategory != null) {
                        selectedCategory = matchedCategory
                    }
                    Toast.makeText(context, "Receipt processed successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error scanning receipt: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempImageUri?.let { processReceiptImage(it) }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { processReceiptImage(it) }
    }

    LaunchedEffect(note) {
        if (note.isNotEmpty() && !isSplitMode && !isCategoryManuallySelected) {
            val matched = filteredCategories.firstOrNull { cat ->
                com.example.vellum.data.SemanticMatcher.isSemanticMatch(note, cat.name, 0.3f) ||
                com.example.vellum.data.SemanticMatcher.isSemanticMatch(cat.name, note, 0.3f)
            }
            if (matched != null) {
                selectedCategory = matched
            } else {
                val noteLower = note.lowercase()
                val predictedCat = when {
                    noteLower.contains("eat") || noteLower.contains("food") || noteLower.contains("restaurant") || 
                    noteLower.contains("mcdonald") || noteLower.contains("pizza") || noteLower.contains("starbucks") || 
                    noteLower.contains("cafe") || noteLower.contains("dinner") || noteLower.contains("lunch") -> {
                        filteredCategories.find { it.name.lowercase().contains("eat") || it.name.lowercase().contains("restaurant") }
                    }
                    noteLower.contains("grocery") || noteLower.contains("supermarket") || noteLower.contains("walmart") || 
                    noteLower.contains("target") || noteLower.contains("milk") || noteLower.contains("store") -> {
                        filteredCategories.find { it.name.lowercase().contains("groc") || it.name.lowercase().contains("shop") }
                    }
                    noteLower.contains("uber") || noteLower.contains("taxi") || noteLower.contains("flight") || 
                    noteLower.contains("cab") || noteLower.contains("train") || noteLower.contains("travel") || 
                    noteLower.contains("trip") -> {
                        filteredCategories.find { it.name.lowercase().contains("travel") || it.name.lowercase().contains("transit") }
                    }
                    noteLower.contains("movie") || noteLower.contains("netflix") || noteLower.contains("game") || 
                    noteLower.contains("tv") || noteLower.contains("cinema") || noteLower.contains("concert") -> {
                        filteredCategories.find { it.name.lowercase().contains("ent") || it.name.lowercase().contains("show") }
                    }
                    noteLower.contains("shirt") || noteLower.contains("pant") || noteLower.contains("clothes") || 
                    noteLower.contains("shoes") || noteLower.contains("dress") || noteLower.contains("mall") -> {
                        filteredCategories.find { it.name.lowercase().contains("clothes") || it.name.lowercase().contains("apparel") }
                    }
                    noteLower.contains("salary") || noteLower.contains("wage") || noteLower.contains("paycheck") || 
                    noteLower.contains("income") -> {
                        filteredCategories.find { it.name.lowercase().contains("salary") || it.name.lowercase().contains("wage") }
                    }
                    noteLower.contains("health") || noteLower.contains("med") || noteLower.contains("doctor") || 
                    noteLower.contains("pharmacy") || noteLower.contains("hospital") -> {
                        filteredCategories.find { it.name.lowercase().contains("health") || it.name.lowercase().contains("med") }
                    }
                    else -> null
                }
                if (predictedCat != null) {
                    selectedCategory = predictedCat
                }
            }
        }
    }

    // Auto-select newly created categories upon return
    var previousCategoryIds by remember { mutableStateOf(categories.map { it.id }.toSet()) }
    LaunchedEffect(categories) {
        val newCats = categories.filter { it.id !in previousCategoryIds }
        if (newCats.isNotEmpty()) {
            selectedCategory = newCats.first()
            isCategoryManuallySelected = true
        }
        previousCategoryIds = categories.map { it.id }.toSet()
    }

    val isNestedBlurActive = showCustomDatePicker || showNoteDialog || activeHelpTip != null
    
    Box(modifier = Modifier.fillMaxSize()) {
        ParchmentBackground(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .blur(if (isNestedBlurActive) 10.dp else 0.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Back",
                            tint = ParchmentDarkBrown
                        )
                    }
                    Text(
                        text = if (transactionToEdit != null) "Edit Transaction" else "Add Transaction",
                        color = ParchmentDarkBrown,
                        fontFamily = ParchmentFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Done",
                        color = ParchmentDarkBrown,
                        fontFamily = ParchmentFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .clickable {
                                if (isSplitMode && splitsList.isEmpty()) {
                                    Toast.makeText(context, "Please add at least one split entry", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }
                                val finalAmount = if (isSplitMode) splitsList.sumOf { it.amount } else (amount.toDoubleOrNull() ?: 0.0)
                                if (finalAmount <= 0) {
                                    Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }
                                val cat = if (isSplitMode) {
                                    categories.find { it.id == splitsList.first().categoryId } ?: selectedCategory
                                } else selectedCategory
                                val acc = if (isSplitMode) {
                                    accounts.find { it.id == splitsList.first().accountId } ?: selectedAccount
                                } else selectedAccount

                                val primaryCat = cat ?: return@clickable
                                val primaryAcc = acc ?: return@clickable

                                val splitsJson = if (isSplitMode) Json.encodeToString(splitsList) else ""

                                viewModel.addTransaction(
                                    amount = finalAmount,
                                    type = type,
                                    categoryId = primaryCat.id,
                                    categoryName = primaryCat.name,
                                    accountId = primaryAcc.id,
                                    accountName = primaryAcc.name,
                                    note = if (isSplitMode) splitsList.first().note else note,
                                    timestamp = selectedTimestamp,
                                    id = transactionToEdit?.id,
                                    splits = splitsJson
                                )
                                onBack()
                            }
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Expense / Income Toggle
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(220.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, ParchmentDarkBrown, RoundedCornerShape(8.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (type == "EXPENSE") ParchmentDarkBrown else Color.Transparent)
                            .clickable { type = "EXPENSE" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "EXPENSE",
                            color = if (type == "EXPENSE") ParchmentBackground else ParchmentDarkBrown,
                            fontFamily = ParchmentFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (type == "INCOME") ParchmentDarkBrown else Color.Transparent)
                            .clickable { type = "INCOME" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "INCOME",
                            color = if (type == "INCOME") ParchmentBackground else ParchmentDarkBrown,
                            fontFamily = ParchmentFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section: Transaction Details
                FormSectionHeader(
                    title = "Transaction Details",
                    onHelpClick = {
                        activeHelpTip = "Enter the date, category, account, and total amount of your transaction. You can also scan a receipt using the 'Scan' button to auto-fill these fields based on the text on the receipt."
                    }
                )
                HorizontalDivider(color = ParchmentLine, thickness = 0.5.dp)

                // Date Row
                FormRow(label = "Date") {
                    Text(
                        text = dateString,
                        color = ParchmentDarkBrown,
                        fontFamily = ParchmentFontFamily,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCustomDatePicker = true }
                    )
                }

                // Category Row
                FormRow(label = "Category") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = selectedCategory?.name ?: "Not Selected",
                            color = if (selectedCategory != null) ParchmentDarkBrown else Color.Gray,
                            fontFamily = ParchmentFontFamily,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { categoryMenuExpanded = true }
                        )
                        DropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false },
                            modifier = Modifier.background(ParchmentBackground)
                        ) {
                            filteredCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name, color = ParchmentDarkBrown, fontFamily = ParchmentFontFamily) },
                                    onClick = {
                                        selectedCategory = cat
                                        isCategoryManuallySelected = true
                                        categoryMenuExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "+ Add Category...",
                                        color = ParchmentBlueText,
                                        fontFamily = ParchmentFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick = {
                                    categoryMenuExpanded = false
                                    onNavigate(AddEditCategory(predefinedType = type))
                                }
                            )
                        }
                    }
                }

                // Account Row
                FormRow(label = "Account") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = selectedAccount?.name ?: "Not Selected",
                            color = if (selectedAccount != null) ParchmentDarkBrown else Color.Gray,
                            fontFamily = ParchmentFontFamily,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { accountMenuExpanded = true }
                        )
                        DropdownMenu(
                            expanded = accountMenuExpanded,
                            onDismissRequest = { accountMenuExpanded = false },
                            modifier = Modifier.background(ParchmentBackground)
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(acc.name, color = ParchmentDarkBrown, fontFamily = ParchmentFontFamily) },
                                    onClick = {
                                        selectedAccount = acc
                                        accountMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Amount Row
                FormRow(label = "Amount") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = if (isSplitMode) splitsList.sumOf { it.amount }.let { if (it == 0.0) "" else it.toString() } else amount,
                            onValueChange = { if (!isSplitMode) amount = it },
                            readOnly = isSplitMode,
                            placeholder = { Text("Amount", color = Color.Gray, fontFamily = ParchmentFontFamily) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = ParchmentDarkBrown,
                                fontFamily = ParchmentFontFamily,
                                fontSize = 16.sp
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        if (!isSplitMode) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { showScanOptionsDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = ParchmentDarkBrown),
                                modifier = Modifier.border(1.dp, ParchmentDarkBrown, RoundedCornerShape(8.dp)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Scan",
                                    fontFamily = ParchmentFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                FormSectionHeader(
                    title = "Split Details",
                    onHelpClick = {
                        activeHelpTip = "Enable Split Mode to divide this transaction among multiple categories and accounts. Add each split entry with its category, account, and amount. The total transaction amount will be computed as the sum of all split amounts."
                    }
                )
                HorizontalDivider(color = ParchmentLine, thickness = 0.5.dp)

                FormRow(label = "Split Transaction") {
                    Switch(
                        checked = isSplitMode,
                        onCheckedChange = { isSplitMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ParchmentDarkBrown,
                            checkedTrackColor = ParchmentLine
                        )
                    )
                }

                if (isSplitMode) {
                    // Display existing splits list
                    splitsList.forEachIndexed { index, split ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 16.dp)
                                .border(0.5.dp, ParchmentLine, RoundedCornerShape(4.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${split.categoryName} (${split.accountName})",
                                    fontFamily = ParchmentFontFamily,
                                    fontSize = 14.sp,
                                    color = ParchmentDarkBrown,
                                    fontWeight = FontWeight.Medium
                                )
                                if (split.note.isNotEmpty()) {
                                    Text(
                                        text = split.note,
                                        fontFamily = ParchmentFontFamily,
                                        fontSize = 12.sp,
                                        color = ParchmentDarkBrown.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = String.format(Locale.US, "%s%.2f", currencySymbol, split.amount),
                                    fontFamily = ParchmentFontFamily,
                                    fontSize = 14.sp,
                                    color = ParchmentDarkBrown,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        splitsList = splitsList.toMutableList().apply { removeAt(index) }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Split",
                                        tint = Color.Red.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Add Split Entry",
                        fontFamily = ParchmentFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ParchmentDarkBrown,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Add split Category selector
                    FormRow(label = "Split Category") {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = splitCategory?.name ?: "Not Selected",
                                color = if (splitCategory != null) ParchmentDarkBrown else Color.Gray,
                                fontFamily = ParchmentFontFamily,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { splitCategoryMenuExpanded = true }
                            )
                            DropdownMenu(
                                expanded = splitCategoryMenuExpanded,
                                onDismissRequest = { splitCategoryMenuExpanded = false },
                                modifier = Modifier.background(ParchmentBackground)
                            ) {
                                filteredCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name, color = ParchmentDarkBrown, fontFamily = ParchmentFontFamily) },
                                        onClick = {
                                            splitCategory = cat
                                            splitCategoryMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Add split Account selector
                    FormRow(label = "Split Account") {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = splitAccount?.name ?: "Not Selected",
                                color = if (splitAccount != null) ParchmentDarkBrown else Color.Gray,
                                fontFamily = ParchmentFontFamily,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { splitAccountMenuExpanded = true }
                            )
                            DropdownMenu(
                                expanded = splitAccountMenuExpanded,
                                onDismissRequest = { splitAccountMenuExpanded = false },
                                modifier = Modifier.background(ParchmentBackground)
                            ) {
                                accounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text(acc.name, color = ParchmentDarkBrown, fontFamily = ParchmentFontFamily) },
                                        onClick = {
                                            splitAccount = acc
                                            splitAccountMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Add split Amount field
                    FormRow(label = "Split Amount") {
                        TextField(
                            value = splitAmount,
                            onValueChange = { splitAmount = it },
                            placeholder = { Text("Split Amount", color = Color.Gray, fontFamily = ParchmentFontFamily) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = ParchmentDarkBrown,
                                fontFamily = ParchmentFontFamily,
                                fontSize = 16.sp
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Add split Note field
                    FormRow(label = "Split Note") {
                        TextField(
                            value = splitNote,
                            onValueChange = { splitNote = it },
                            placeholder = { Text("Split Note (optional)", color = Color.Gray, fontFamily = ParchmentFontFamily) },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = ParchmentDarkBrown,
                                fontFamily = ParchmentFontFamily,
                                fontSize = 16.sp
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = {
                            val parsed = splitAmount.toDoubleOrNull()
                            if (parsed == null || parsed <= 0) {
                                Toast.makeText(context, "Please enter split amount", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val cat = splitCategory ?: return@Button
                            val acc = splitAccount ?: return@Button
                            val splitObj = TransactionSplit(
                                categoryId = cat.id,
                                categoryName = cat.name,
                                accountId = acc.id,
                                accountName = acc.name,
                                amount = parsed,
                                note = splitNote.trim()
                            )
                            splitsList = splitsList + splitObj
                            splitAmount = ""
                            splitNote = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = ParchmentDarkBrown),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .border(1.dp, ParchmentDarkBrown, RoundedCornerShape(8.dp))
                    ) {
                        Text(text = "+ Add Split Row", fontFamily = ParchmentFontFamily, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section: Repeating Details
                FormSectionHeader(
                    title = "Repeating Details",
                    onHelpClick = {
                        activeHelpTip = "Turn on the Repeat toggle to mark this transaction as a recurring template that can be quickly added or monitored in your sheets."
                    }
                )
                HorizontalDivider(color = ParchmentLine, thickness = 0.5.dp)

                // Repeat Row
                FormRow(label = "Repeat") {
                    Switch(
                        checked = repeat,
                        onCheckedChange = { repeat = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ParchmentDarkBrown,
                            checkedTrackColor = ParchmentLine
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Note
                HorizontalDivider(color = ParchmentLine, thickness = 0.5.dp)
                FormRow(label = "Note") {
                    Text(
                        text = if (note.isEmpty()) "No Note Entered" else note,
                        color = if (note.isEmpty()) Color.Gray else ParchmentDarkBrown,
                        fontFamily = ParchmentFontFamily,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNoteDialog = true }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }
    }

    if (showNoteDialog) {
        CreateNoteDialog(
            initialNote = note,
            onDismiss = { showNoteDialog = false },
            onSetNote = { note = it }
        )
    }

    if (showCustomDatePicker) {
        ChalkboardDatePickerDialog(
            initialTimestamp = selectedTimestamp,
            onDismiss = { showCustomDatePicker = false },
            onDateSelected = { timestamp ->
                selectedTimestamp = timestamp
                showCustomDatePicker = false
            }
        )
    }

    if (showScanOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showScanOptionsDialog = false },
            title = {
                Text(
                    text = "Scan Receipt",
                    fontFamily = ParchmentTitleFontFamily,
                    color = ParchmentDarkBrown,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Choose receipt source:",
                    fontFamily = ParchmentFontFamily,
                    color = ParchmentDarkBrown
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showScanOptionsDialog = false
                        val file = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
                        val authority = "${context.packageName}.fileprovider"
                        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
                        tempImageUri = uri
                        takePictureLauncher.launch(uri)
                    }
                ) {
                    Text("Camera", fontFamily = ParchmentFontFamily, color = ParchmentBlueText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showScanOptionsDialog = false
                        pickImageLauncher.launch("image/*")
                    }
                ) {
                    Text("Gallery", fontFamily = ParchmentFontFamily, color = ParchmentBlueText, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = ParchmentBackground
        )
    }

    if (activeHelpTip != null) {
        AlertDialog(
            onDismissRequest = { activeHelpTip = null },
            title = {
                Text(
                    text = "Help Tip",
                    fontFamily = ParchmentTitleFontFamily,
                    color = ParchmentDarkBrown,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = activeHelpTip ?: "",
                    fontFamily = ParchmentFontFamily,
                    color = ParchmentDarkBrown,
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { activeHelpTip = null }) {
                    Text("Got it", fontFamily = ParchmentFontFamily, color = ParchmentBlueText, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = ParchmentBackground
        )
    }
}
