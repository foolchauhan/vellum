package com.example.vellum.ui.dialogs

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
import androidx.compose.ui.window.Dialog
import com.example.vellum.theme.*
import com.example.vellum.ui.components.*
import com.example.vellum.ui.main.MainScreenViewModel
import com.example.vellum.data.local.TransactionEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    viewModel: MainScreenViewModel,
    predefinedType: String,
    transactionToEdit: TransactionEntity? = null,
    onDismiss: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()

    var type by remember { mutableStateOf(transactionToEdit?.type ?: predefinedType) }
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
    var selectedCategory by remember(filteredCategories) {
        mutableStateOf(
            if (transactionToEdit != null && transactionToEdit.type == type) {
                categories.find { it.id == transactionToEdit.categoryId } ?: filteredCategories.firstOrNull()
            } else {
                filteredCategories.firstOrNull()
            }
        )
    }
    var selectedAccount by remember(accounts) {
        mutableStateOf(
            if (transactionToEdit != null) {
                accounts.find { it.id == transactionToEdit.accountId } ?: accounts.firstOrNull()
            } else {
                viewModel.selectedFilterAccount.value?.let { filtered ->
                    accounts.find { it.id == filtered.id }
                } ?: accounts.find { it.isDefault } ?: accounts.firstOrNull()
            }
        )
    }

    var selectedTimestamp by remember { mutableStateOf(transactionToEdit?.timestamp ?: System.currentTimeMillis()) }

    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var accountMenuExpanded by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = selectedTimestamp
    val dateString = remember(selectedTimestamp) {
        val format = SimpleDateFormat("dd MMM yyyy", Locale.US)
        format.format(Date(selectedTimestamp))
    }

    var showCustomDatePicker by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ChalkboardSlate),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .border(1.dp, ParchmentLine, RoundedCornerShape(12.dp))
        ) {
            ParchmentBackground(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                val isNestedBlurActive = showCustomDatePicker || showNoteDialog || showAddCategoryDialog
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
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
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = ParchmentDarkBrown
                            )
                        }
                        Text(
                            text = "Done",
                            color = ParchmentDarkBrown,
                            fontFamily = ParchmentFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .clickable {
                                    val parsedAmount = amount.toDoubleOrNull()
                                    if (parsedAmount == null || parsedAmount <= 0) {
                                        Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }
                                    val cat = selectedCategory ?: return@clickable
                                    val acc = selectedAccount ?: return@clickable
                                    viewModel.addTransaction(
                                        amount = parsedAmount,
                                        type = type,
                                        categoryId = cat.id,
                                        categoryName = cat.name,
                                        accountId = acc.id,
                                        accountName = acc.name,
                                        note = note,
                                        timestamp = selectedTimestamp,
                                        id = transactionToEdit?.id
                                    )
                                    onDismiss()
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
                    FormSectionHeader(title = "Transaction Details")
                    com.example.vellum.ui.components.HorizontalDivider(color = ParchmentLine, thickness = 0.5.dp)

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
                                        showAddCategoryDialog = true
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
                        TextField(
                            value = amount,
                            onValueChange = { amount = it },
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
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Section: Repeating Details
                    FormSectionHeader(title = "Repeating Details")
                    com.example.vellum.ui.components.HorizontalDivider(color = ParchmentLine, thickness = 0.5.dp)

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
                    com.example.vellum.ui.components.HorizontalDivider(color = ParchmentLine, thickness = 0.5.dp)
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
    }

    if (showNoteDialog) {
        CreateNoteDialog(
            initialNote = note,
            onDismiss = { showNoteDialog = false },
            onSetNote = { note = it }
        )
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            viewModel = viewModel,
            predefinedType = type,
            onDismiss = { showAddCategoryDialog = false }
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
}
