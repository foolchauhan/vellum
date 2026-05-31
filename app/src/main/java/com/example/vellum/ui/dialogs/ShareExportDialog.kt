package com.example.vellum.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.vellum.data.local.AccountEntity
import com.example.vellum.theme.*
import com.example.vellum.ui.components.ParchmentBackground
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ShareExportDialog(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onExportCsv: (accountFilter: AccountEntity?, startTimestamp: Long, endTimestamp: Long, periodLabel: String) -> Unit,
    onExportPdf: (accountFilter: AccountEntity?, startTimestamp: Long, endTimestamp: Long, periodLabel: String) -> Unit
) {
    var selectedAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var selectedPeriodType by remember { mutableStateOf("Monthly") }

    // Date millisecond for Daily/Weekly
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    // Month & Year selection for Monthly
    val currentCal = Calendar.getInstance()
    var selectedMonth by remember { mutableStateOf(currentCal.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(currentCal.get(Calendar.YEAR)) }

    // Year selection for Yearly
    var selectedYearOnly by remember { mutableStateOf(currentCal.get(Calendar.YEAR)) }

    var accountMenuExpanded by remember { mutableStateOf(false) }
    var periodTypeMenuExpanded by remember { mutableStateOf(false) }
    var monthMenuExpanded by remember { mutableStateOf(false) }
    var yearMenuExpanded by remember { mutableStateOf(false) }
    var yearOnlyMenuExpanded by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }

    val monthsList = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val yearsList = (2024..2030).map { it.toString() }

    val dateLabel = remember(selectedDateMillis) {
        SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(selectedDateMillis))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ChalkboardSlate),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, ParchmentLine, RoundedCornerShape(12.dp))
        ) {
            ParchmentBackground(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Export Transactions",
                        fontFamily = ParchmentFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = ParchmentDarkBrown,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // 1. Account Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Account Filter",
                            fontFamily = ParchmentFontFamily,
                            fontSize = 12.sp,
                            color = ParchmentDarkBrown.copy(alpha = 0.7f)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ParchmentLine, RoundedCornerShape(6.dp))
                                .clickable { accountMenuExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = selectedAccount?.name ?: "All Accounts",
                                color = ParchmentDarkBrown,
                                fontFamily = ParchmentFontFamily,
                                fontSize = 14.sp
                            )
                            DropdownMenu(
                                expanded = accountMenuExpanded,
                                onDismissRequest = { accountMenuExpanded = false },
                                modifier = Modifier.background(ParchmentBackground)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Accounts", color = ParchmentDarkBrown, fontFamily = ParchmentFontFamily) },
                                    onClick = {
                                        selectedAccount = null
                                        accountMenuExpanded = false
                                    }
                                )
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

                    // 2. Period Type Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Period Type",
                            fontFamily = ParchmentFontFamily,
                            fontSize = 12.sp,
                            color = ParchmentDarkBrown.copy(alpha = 0.7f)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ParchmentLine, RoundedCornerShape(6.dp))
                                .clickable { periodTypeMenuExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = selectedPeriodType,
                                color = ParchmentDarkBrown,
                                fontFamily = ParchmentFontFamily,
                                fontSize = 14.sp
                            )
                            DropdownMenu(
                                expanded = periodTypeMenuExpanded,
                                onDismissRequest = { periodTypeMenuExpanded = false },
                                modifier = Modifier.background(ParchmentBackground)
                            ) {
                                listOf("All Time", "Daily", "Weekly", "Monthly", "Yearly").forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type, color = ParchmentDarkBrown, fontFamily = ParchmentFontFamily) },
                                        onClick = {
                                            selectedPeriodType = type
                                            periodTypeMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Conditional Specific Period Selectors
                    when (selectedPeriodType) {
                        "Daily", "Weekly" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (selectedPeriodType == "Daily") "Select Date" else "Select Week (contains date)",
                                    fontFamily = ParchmentFontFamily,
                                    fontSize = 12.sp,
                                    color = ParchmentDarkBrown.copy(alpha = 0.7f)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, ParchmentLine, RoundedCornerShape(6.dp))
                                        .clickable { showDatePicker = true }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = dateLabel,
                                        color = ParchmentDarkBrown,
                                        fontFamily = ParchmentFontFamily,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                        "Monthly" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.weight(1.5f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Month",
                                        fontFamily = ParchmentFontFamily,
                                        fontSize = 12.sp,
                                        color = ParchmentDarkBrown.copy(alpha = 0.7f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, ParchmentLine, RoundedCornerShape(6.dp))
                                            .clickable { monthMenuExpanded = true }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = monthsList[selectedMonth],
                                            color = ParchmentDarkBrown,
                                            fontFamily = ParchmentFontFamily,
                                            fontSize = 14.sp
                                        )
                                        DropdownMenu(
                                            expanded = monthMenuExpanded,
                                            onDismissRequest = { monthMenuExpanded = false },
                                            modifier = Modifier.background(ParchmentBackground)
                                        ) {
                                            monthsList.forEachIndexed { index, mName ->
                                                DropdownMenuItem(
                                                    text = { Text(mName, color = ParchmentDarkBrown, fontFamily = ParchmentFontFamily) },
                                                    onClick = {
                                                        selectedMonth = index
                                                        monthMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Year",
                                        fontFamily = ParchmentFontFamily,
                                        fontSize = 12.sp,
                                        color = ParchmentDarkBrown.copy(alpha = 0.7f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, ParchmentLine, RoundedCornerShape(6.dp))
                                            .clickable { yearMenuExpanded = true }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = selectedYear.toString(),
                                            color = ParchmentDarkBrown,
                                            fontFamily = ParchmentFontFamily,
                                            fontSize = 14.sp
                                        )
                                        DropdownMenu(
                                            expanded = yearMenuExpanded,
                                            onDismissRequest = { yearMenuExpanded = false },
                                            modifier = Modifier.background(ParchmentBackground)
                                        ) {
                                            yearsList.forEach { yStr ->
                                                DropdownMenuItem(
                                                    text = { Text(yStr, color = ParchmentDarkBrown, fontFamily = ParchmentFontFamily) },
                                                    onClick = {
                                                        selectedYear = yStr.toInt()
                                                        yearMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "Yearly" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Select Year",
                                    fontFamily = ParchmentFontFamily,
                                    fontSize = 12.sp,
                                    color = ParchmentDarkBrown.copy(alpha = 0.7f)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, ParchmentLine, RoundedCornerShape(6.dp))
                                        .clickable { yearOnlyMenuExpanded = true }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = selectedYearOnly.toString(),
                                        color = ParchmentDarkBrown,
                                        fontFamily = ParchmentFontFamily,
                                        fontSize = 14.sp
                                    )
                                    DropdownMenu(
                                        expanded = yearOnlyMenuExpanded,
                                        onDismissRequest = { yearOnlyMenuExpanded = false },
                                        modifier = Modifier.background(ParchmentBackground)
                                    ) {
                                        yearsList.forEach { yStr ->
                                            DropdownMenuItem(
                                                text = { Text(yStr, color = ParchmentDarkBrown, fontFamily = ParchmentFontFamily) },
                                                onClick = {
                                                    selectedYearOnly = yStr.toInt()
                                                    yearOnlyMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4. Export Buttons
                    val (start, end, label) = remember(
                        selectedPeriodType, selectedDateMillis,
                        selectedMonth, selectedYear, selectedYearOnly
                    ) {
                        val cal = Calendar.getInstance()
                        when (selectedPeriodType) {
                            "All Time" -> {
                                Triple(0L, Long.MAX_VALUE, "All Time")
                            }
                            "Daily" -> {
                                cal.timeInMillis = selectedDateMillis
                                cal.set(Calendar.HOUR_OF_DAY, 0)
                                cal.set(Calendar.MINUTE, 0)
                                cal.set(Calendar.SECOND, 0)
                                cal.set(Calendar.MILLISECOND, 0)
                                val startVal = cal.timeInMillis
                                cal.set(Calendar.HOUR_OF_DAY, 23)
                                cal.set(Calendar.MINUTE, 59)
                                cal.set(Calendar.SECOND, 59)
                                cal.set(Calendar.MILLISECOND, 999)
                                val endVal = cal.timeInMillis
                                val dateLbl = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(selectedDateMillis))
                                Triple(startVal, endVal, dateLbl)
                            }
                            "Weekly" -> {
                                cal.timeInMillis = selectedDateMillis
                                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                                cal.set(Calendar.HOUR_OF_DAY, 0)
                                cal.set(Calendar.MINUTE, 0)
                                cal.set(Calendar.SECOND, 0)
                                cal.set(Calendar.MILLISECOND, 0)
                                val startVal = cal.timeInMillis
                                cal.add(Calendar.DAY_OF_WEEK, 6)
                                cal.set(Calendar.HOUR_OF_DAY, 23)
                                cal.set(Calendar.MINUTE, 59)
                                cal.set(Calendar.SECOND, 59)
                                cal.set(Calendar.MILLISECOND, 999)
                                val endVal = cal.timeInMillis

                                val format = SimpleDateFormat("MMM d", Locale.US)
                                val startStr = format.format(Date(startVal))
                                val endStr = format.format(Date(endVal))
                                Triple(startVal, endVal, "Week of $startStr - $endStr")
                            }
                            "Yearly" -> {
                                cal.set(Calendar.YEAR, selectedYearOnly)
                                cal.set(Calendar.DAY_OF_YEAR, 1)
                                cal.set(Calendar.HOUR_OF_DAY, 0)
                                cal.set(Calendar.MINUTE, 0)
                                cal.set(Calendar.SECOND, 0)
                                cal.set(Calendar.MILLISECOND, 0)
                                val startVal = cal.timeInMillis
                                cal.set(Calendar.MONTH, Calendar.DECEMBER)
                                cal.set(Calendar.DAY_OF_MONTH, 31)
                                cal.set(Calendar.HOUR_OF_DAY, 23)
                                cal.set(Calendar.MINUTE, 59)
                                cal.set(Calendar.SECOND, 59)
                                cal.set(Calendar.MILLISECOND, 999)
                                val endVal = cal.timeInMillis
                                Triple(startVal, endVal, "Year $selectedYearOnly")
                            }
                            else -> { // Monthly
                                cal.set(Calendar.YEAR, selectedYear)
                                cal.set(Calendar.MONTH, selectedMonth)
                                cal.set(Calendar.DAY_OF_MONTH, 1)
                                cal.set(Calendar.HOUR_OF_DAY, 0)
                                cal.set(Calendar.MINUTE, 0)
                                cal.set(Calendar.SECOND, 0)
                                cal.set(Calendar.MILLISECOND, 0)
                                val startVal = cal.timeInMillis
                                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                                cal.set(Calendar.HOUR_OF_DAY, 23)
                                cal.set(Calendar.MINUTE, 59)
                                cal.set(Calendar.SECOND, 59)
                                cal.set(Calendar.MILLISECOND, 999)
                                val endVal = cal.timeInMillis
                                Triple(startVal, endVal, "${monthsList[selectedMonth]} $selectedYear")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onExportPdf(selectedAccount, start, end, label)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF",
                            tint = ParchmentDarkBrown
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Export as PDF Report",
                            fontFamily = ParchmentFontFamily,
                            color = ParchmentDarkBrown,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onExportCsv(selectedAccount, start, end, label)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "CSV",
                            tint = ParchmentDarkBrown
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Export as CSV Data",
                            fontFamily = ParchmentFontFamily,
                            color = ParchmentDarkBrown,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = "CANCEL",
                            color = ParchmentDarkBrown,
                            fontFamily = ParchmentFontFamily,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onDismiss() }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        ChalkboardDatePickerDialog(
            initialTimestamp = selectedDateMillis,
            onDismiss = { showDatePicker = false },
            onDateSelected = { timestamp ->
                selectedDateMillis = timestamp
                showDatePicker = false
            }
        )
    }
}
