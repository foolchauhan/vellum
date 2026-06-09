package com.example.vellum.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.vellum.ui.main.MainScreenViewModel

@Composable
fun FilterTransactionsDialog(viewModel: MainScreenViewModel, onDismiss: () -> Unit) {
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val activeCat by viewModel.selectedFilterCategory.collectAsState()
    val activeAcc by viewModel.selectedFilterAccount.collectAsState()

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    var selectedCat by remember { mutableStateOf(activeCat) }
    var selectedAcc by remember { mutableStateOf(activeAcc) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ParchmentBackground),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, ParchmentLine, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Filter Transactions",
                    fontFamily = ParchmentFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ParchmentDarkBrown
                )

                // Category selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Category Filter", fontFamily = ParchmentFontFamily, fontSize = 12.sp, color = ParchmentDarkBrown.copy(alpha = 0.7f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ParchmentLine, RoundedCornerShape(4.dp))
                            .clickable { categoryDropdownExpanded = true }
                            .padding(12.dp)
                    ) {
                        Text(
                            text = selectedCat?.name ?: "All Categories",
                            color = ParchmentDarkBrown,
                            fontFamily = ParchmentFontFamily
                        )

                        DropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false },
                            modifier = Modifier.background(ParchmentBackground)
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Categories", color = ParchmentDarkBrown, fontFamily = ParchmentFontFamily) },
                                onClick = {
                                    selectedCat = null
                                    categoryDropdownExpanded = false
                                }
                            )
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name, color = ParchmentDarkBrown, fontFamily = ParchmentFontFamily) },
                                    onClick = {
                                        selectedCat = cat
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Account selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Account Filter", fontFamily = ParchmentFontFamily, fontSize = 12.sp, color = ParchmentDarkBrown.copy(alpha = 0.7f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ParchmentLine, RoundedCornerShape(4.dp))
                            .clickable { accountDropdownExpanded = true }
                            .padding(12.dp)
                    ) {
                        Text(
                            text = selectedAcc?.name ?: "All Accounts",
                            color = ParchmentDarkBrown,
                            fontFamily = ParchmentFontFamily
                        )

                        DropdownMenu(
                            expanded = accountDropdownExpanded,
                            onDismissRequest = { accountDropdownExpanded = false },
                            modifier = Modifier.background(ParchmentBackground)
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Accounts", color = ParchmentDarkBrown, fontFamily = ParchmentFontFamily) },
                                onClick = {
                                    selectedAcc = null
                                    accountDropdownExpanded = false
                                }
                            )
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(acc.name, color = ParchmentDarkBrown, fontFamily = ParchmentFontFamily) },
                                    onClick = {
                                        selectedAcc = acc
                                        accountDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Actions: Reset, Cancel, Apply
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        viewModel.clearFilters()
                        onDismiss()
                    }) {
                        Text(
                            text = "Reset",
                            color = ParchmentDarkBrown.copy(alpha = 0.7f),
                            fontFamily = ParchmentFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "Cancel",
                                color = ParchmentDarkBrown.copy(alpha = 0.7f),
                                fontFamily = ParchmentFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.setFilterCategory(selectedCat)
                                viewModel.setFilterAccount(selectedAcc)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ParchmentDarkBrown)
                        ) {
                            Text(
                                text = "Apply",
                                color = ParchmentBackground,
                                fontFamily = ParchmentFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SwitchAccountDialog(viewModel: MainScreenViewModel, onDismiss: () -> Unit) {
    val accounts by viewModel.accounts.collectAsState()
    val activeAccount by viewModel.selectedFilterAccount.collectAsState()

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
                    text = "Switch Account",
                    fontFamily = ParchmentFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ParchmentDarkBrown,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Option: All Accounts
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.setFilterAccount(null)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (activeAccount == null),
                        onClick = {
                            viewModel.setFilterAccount(null)
                            onDismiss()
                        },
                        colors = RadioButtonDefaults.colors(selectedColor = ParchmentDarkBrown)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "All Accounts",
                        fontFamily = ParchmentFontFamily,
                        color = ParchmentDarkBrown,
                        fontSize = 16.sp
                    )
                }

                // Options: specific accounts
                accounts.forEach { acc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setFilterAccount(acc)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (activeAccount?.id == acc.id),
                            onClick = {
                                viewModel.setFilterAccount(acc)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = ParchmentDarkBrown)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = acc.name,
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
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }
            }
        }
    }
}

@Composable
fun FilterCategoryDialog(viewModel: MainScreenViewModel, onDismiss: () -> Unit) {
    val categories by viewModel.categories.collectAsState()
    val activeCat by viewModel.selectedFilterCategory.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ParchmentBackground),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, ParchmentLine, RoundedCornerShape(12.dp))
        ) {
            ParchmentBackground(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .wrapContentHeight()
                ) {
                    Text(
                        text = "Filter by Category",
                        fontFamily = ParchmentFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ParchmentDarkBrown,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setFilterCategory(null)
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (activeCat == null),
                                    onClick = {
                                        viewModel.setFilterCategory(null)
                                        onDismiss()
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = ParchmentDarkBrown)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "All Categories",
                                    fontFamily = ParchmentFontFamily,
                                    color = ParchmentDarkBrown,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        items(categories) { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setFilterCategory(cat)
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (activeCat?.id == cat.id),
                                    onClick = {
                                        viewModel.setFilterCategory(cat)
                                        onDismiss()
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = ParchmentDarkBrown)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = cat.name,
                                    fontFamily = ParchmentFontFamily,
                                    color = ParchmentDarkBrown,
                                    fontSize = 16.sp
                                )
                            }
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
fun FilterAccountDialog(viewModel: MainScreenViewModel, onDismiss: () -> Unit) {
    SwitchAccountDialog(viewModel = viewModel, onDismiss = onDismiss)
}
