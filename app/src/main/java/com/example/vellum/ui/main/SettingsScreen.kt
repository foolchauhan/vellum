package com.example.vellum.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vellum.theme.*
import com.example.vellum.ui.components.HorizontalDivider
import com.example.vellum.ui.components.ParchmentBackground
import com.example.vellum.ui.components.getSettingsIconForName
import com.example.vellum.ui.dialogs.SettingsSelectionDialog
import com.example.vellum.ui.dialogs.ChalkboardDatePickerDialog
import com.example.vellum.ui.dialogs.AccountCarryOverSettingsDialog
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun SettingsScreen(
    viewModel: MainScreenViewModel,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferences by viewModel.preferences.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    var activeDialogOption by remember { mutableStateOf<String?>(null) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var tempStartDate by remember { mutableStateOf(System.currentTimeMillis()) }

    val createTemplateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    val templateText = "Date,Type,Amount,Category,Account,Note\n2026-05-30,EXPENSE,15.50,Food,Personal,Lunch at cafe\n2026-05-30,INCOME,1500.00,Salary,Personal,Monthly paycheck\n"
                    outputStream.write(templateText.toByteArray())
                }
                android.widget.Toast.makeText(context, "Template saved successfully", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Error saving template: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val csvText = context.contentResolver.openInputStream(it)?.use { inputStream ->
                    inputStream.bufferedReader().use { reader -> reader.readText() }
                }
                if (csvText != null) {
                    viewModel.bulkUploadTransactions(csvText) { success, failure ->
                        android.widget.Toast.makeText(
                            context,
                            "Uploaded: $success success, $failure failed",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    android.widget.Toast.makeText(context, "Could not read file", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val dialogData = when(activeDialogOption) {
        "time_period" -> Pair("Time Period", listOf("Daily", "Weekly", "Monthly", "Yearly", "All", "Last 6 Months", "Last 1 Year", "Custom"))
        "budget_mode" -> Pair("Budget Mode", listOf("On", "Off"))
        "hide_future" -> Pair("Hide Future Transactions", listOf("On", "Off"))
        "dropbox_sync" -> Pair("Dropbox Sync", listOf("On", "Off"))
        "theme" -> Pair("Theme", listOf("Light", "Dark", "System"))
        "show_notes" -> Pair("Show Transaction Note", listOf("On", "Off"))
        "currency_symbol" -> Pair("Currency Symbol", listOf("Default", "$", "₹", "€", "£"))
        "summary_font" -> Pair("Summary Font", listOf("Chalk", "Default"))
        "category_icon_style" -> Pair("Category Icon Style", listOf("Filled", "Outlined"))
        "tabs_position" -> Pair("Tabs Position", listOf("Top", "Bottom"))
        "reminders" -> Pair("Reminders", listOf("Off", "Daily", "Every Week", "Monthly"))
        "auto_backup" -> Pair("Auto Backup", listOf("On", "Off"))
        "passcode" -> Pair("Passcode", listOf("On", "Off"))
        else -> null
    }

    val columnModifier = if (activeDialogOption != null) {
        Modifier.fillMaxSize().blur(10.dp)
    } else {
        Modifier.fillMaxSize()
    }

    Box(modifier = modifier) {
        Column(modifier = columnModifier) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClicked) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = ParchmentDarkBrown
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings",
                    color = ParchmentDarkBrown,
                    fontFamily = ParchmentFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Profile Card at the top
                item {
                    GoogleProfileCard(viewModel = viewModel)
                }

                // Spending Section
                item { SettingsSectionHeader("Spending") }
                item {
                    SettingsRow(
                        title = "Time Period",
                        value = preferences["time_period"] ?: "Monthly",
                        iconName = "timeperiod"
                    ) {
                        activeDialogOption = "time_period"
                    }
                }
                item {
                    SettingsRow(
                        title = "Budget Mode",
                        value = preferences["budget_mode"] ?: "Off",
                        iconName = "budgetmode"
                    ) {
                        activeDialogOption = "budget_mode"
                    }
                }
                item {
                    val enabledCount = accounts.count { it.carryOver }
                    SettingsRow(
                        title = "Carry Over",
                        value = if (enabledCount > 0) "$enabledCount Enabled" else "Off",
                        iconName = "carryover"
                    ) {
                        activeDialogOption = "carry_over"
                    }
                }
                item {
                    SettingsRow(
                        title = "Hide Future Transactions",
                        value = preferences["hide_future"] ?: "Off",
                        iconName = "hidefuture"
                    ) {
                        activeDialogOption = "hide_future"
                    }
                }



                // User Interface Section
                item { SettingsSectionHeader("User Interface") }
                item {
                    SettingsRow(
                        title = "Theme",
                        value = preferences["theme"] ?: "System",
                        iconName = "darktheme"
                    ) {
                        activeDialogOption = "theme"
                    }
                }
                item {
                    SettingsRow(
                        title = "Show Transaction Note",
                        value = preferences["show_notes"] ?: "On",
                        iconName = "shownote"
                    ) {
                        activeDialogOption = "show_notes"
                    }
                }
                item {
                    SettingsRow(
                        title = "Currency Symbol",
                        value = preferences["currency_symbol"] ?: "Default",
                        iconName = "currencysymbol"
                    ) {
                        activeDialogOption = "currency_symbol"
                    }
                }
                item {
                    SettingsRow(
                        title = "Summary Font",
                        value = preferences["summary_font"] ?: "Chalk",
                        iconName = "summaryfont"
                    ) {
                        activeDialogOption = "summary_font"
                    }
                }
                item {
                    SettingsRow(
                        title = "Category Icon Style",
                        value = preferences["category_icon_style"] ?: "Filled",
                        iconName = "categoryiconstyle"
                    ) {
                        activeDialogOption = "category_icon_style"
                    }
                }
                item {
                    SettingsRow(
                        title = "Tabs Position",
                        value = preferences["tabs_position"] ?: "Top",
                        iconName = "tabsposition"
                    ) {
                        activeDialogOption = "tabs_position"
                    }
                }

                // General Section
                item { SettingsSectionHeader("General") }
                item {
                    SettingsRow(
                        title = "Reminders",
                        value = preferences["reminders"] ?: "Every Week",
                        iconName = "reminders"
                    ) {
                        activeDialogOption = "reminders"
                    }
                }
                item {
                    SettingsRow(
                        title = "Auto Backup",
                        value = preferences["auto_backup"] ?: "Off",
                        iconName = "autobackup"
                    ) {
                        activeDialogOption = "auto_backup"
                    }
                }
                item {
                    SettingsRow(
                        title = "Passcode",
                        value = preferences["passcode"] ?: "Off",
                        iconName = "passcode"
                    ) {
                        activeDialogOption = "passcode"
                    }
                }

                // Advanced Options Section
                item { SettingsSectionHeader("Advanced Options") }
                item {
                    SettingsRow(
                        title = "Download CSV Template",
                        value = "Download",
                        iconName = "download"
                    ) {
                        createTemplateLauncher.launch("vellum_template.csv")
                    }
                }
                item {
                    SettingsRow(
                        title = "Bulk Upload CSV",
                        value = "Upload",
                        iconName = "upload"
                    ) {
                        getContentLauncher.launch("*/*")
                    }
                }
            }
        }
    }

    dialogData?.let { (title, optionsList) ->
        SettingsSelectionDialog(
            title = title,
            options = optionsList,
            selectedValue = preferences[activeDialogOption!!] ?: when(activeDialogOption) {
                "show_notes" -> "On"
                "currency_symbol" -> "Default"
                "summary_font" -> "Chalk"
                "category_icon_style" -> "Filled"
                "tabs_position" -> "Top"
                "reminders" -> "Every Week"
                "theme" -> "System"
                else -> "Off"
            },
            onDismiss = { activeDialogOption = null },
            onSelect = { newValue ->
                if (newValue == "Custom") {
                    tempStartDate = System.currentTimeMillis()
                    showStartPicker = true
                } else {
                    viewModel.updatePreference(activeDialogOption!!, newValue)
                    if (activeDialogOption == "time_period") {
                        viewModel.resetPeriodOffset()
                    }
                }
                activeDialogOption = null
            }
        )
    }

    if (activeDialogOption == "carry_over") {
        AccountCarryOverSettingsDialog(
            viewModel = viewModel,
            onDismiss = { activeDialogOption = null }
        )
    }

    if (showStartPicker) {
        ChalkboardDatePickerDialog(
            initialTimestamp = tempStartDate,
            onDismiss = { showStartPicker = false },
            onDateSelected = { startMs ->
                tempStartDate = startMs
                showStartPicker = false
                showEndPicker = true
            }
        )
    }

    if (showEndPicker) {
        ChalkboardDatePickerDialog(
            initialTimestamp = tempStartDate,
            onDismiss = { showEndPicker = false },
            onDateSelected = { endMs ->
                showEndPicker = false
                viewModel.updatePreference("custom_start_date", tempStartDate.toString())
                viewModel.updatePreference("custom_end_date", endMs.toString())
                viewModel.updatePreference("time_period", "Custom")
                viewModel.resetPeriodOffset()
            }
        )
    }


}

@Composable
fun SettingsSectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SettingsSectionHeader)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            color = ParchmentDarkBrown.copy(alpha = 0.7f),
            fontFamily = ParchmentFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
fun SettingsRow(title: String, value: String, iconName: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = getSettingsIconForName(iconName),
                    contentDescription = title,
                    tint = ParchmentDarkBrown
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    color = ParchmentDarkBrown,
                    fontFamily = ParchmentFontFamily,
                    fontSize = 16.sp
                )
            }
            Text(
                text = value,
                color = ParchmentBlueText,
                fontFamily = ParchmentFontFamily,
                fontSize = 16.sp
            )
        }
        HorizontalDivider(color = ParchmentLine, thickness = 0.5.dp)
    }
}
