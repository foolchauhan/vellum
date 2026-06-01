package com.example.vellum.ui.dialogs

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.vellum.theme.*
import com.example.vellum.ui.components.ParchmentBackground
import com.example.vellum.ui.components.HorizontalDivider
import com.example.vellum.ui.components.getIconForName
import com.example.vellum.ui.main.MainScreenViewModel
import com.example.vellum.data.local.AccountEntity

@Composable
fun SettingsSelectionDialog(
    title: String,
    options: List<String>,
    selectedValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
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
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = title,
                        fontFamily = ParchmentFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ParchmentDarkBrown,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(option) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedValue == option),
                                onClick = { onSelect(option) },
                                colors = RadioButtonDefaults.colors(selectedColor = ParchmentDarkBrown)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = option,
                                fontFamily = ParchmentFontFamily,
                                color = ParchmentDarkBrown,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
}

@Composable
fun ShowSpendingDialog(viewModel: MainScreenViewModel, onDismiss: () -> Unit) {
    val preferences by viewModel.preferences.collectAsState()
    val selectedPeriod = preferences["time_period"] ?: "Monthly"
    val options = listOf("Daily", "Weekly", "Monthly", "Yearly", "All", "Last 6 Months", "Last 1 Year", "Custom")

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var tempStartDate by remember { mutableStateOf(System.currentTimeMillis()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ParchmentBackground),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, ParchmentLine, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Show Spending",
                    fontFamily = ParchmentFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ParchmentDarkBrown,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (option == "Custom") {
                                    tempStartDate = System.currentTimeMillis()
                                    showStartPicker = true
                                } else {
                                    viewModel.updatePreference("time_period", option)
                                    viewModel.resetPeriodOffset()
                                    onDismiss()
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedPeriod == option),
                            onClick = {
                                if (option == "Custom") {
                                    tempStartDate = System.currentTimeMillis()
                                    showStartPicker = true
                                } else {
                                    viewModel.updatePreference("time_period", option)
                                    viewModel.resetPeriodOffset()
                                    onDismiss()
                                }
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = ParchmentDarkBrown)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = option,
                            fontFamily = ParchmentFontFamily,
                            color = ParchmentDarkBrown,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                onDismiss()
            }
        )
    }
}

@Composable
fun AccountCarryOverSettingsDialog(
    viewModel: MainScreenViewModel,
    onDismiss: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val isOutlined = preferences["category_icon_style"] == "Outlined"

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
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Carry Over Settings",
                        fontFamily = ParchmentFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ParchmentDarkBrown,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        accounts.forEach { acc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val iconName = if (acc.shareCode != null) "shared" else (acc.icon.takeIf { it.isNotEmpty() } ?: "personal")
                                    Icon(
                                        imageVector = getIconForName(iconName, isOutlined),
                                        contentDescription = acc.name,
                                        tint = ParchmentDarkBrown,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = acc.name,
                                        fontFamily = ParchmentFontFamily,
                                        color = ParchmentDarkBrown,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Switch(
                                    checked = acc.carryOver,
                                    onCheckedChange = { isChecked ->
                                        viewModel.updateAccountCarryOver(acc, isChecked)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = ParchmentDarkBrown,
                                        checkedTrackColor = ParchmentLine
                                    )
                                )
                            }
                            HorizontalDivider(color = ParchmentLine.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = "DONE",
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
}

