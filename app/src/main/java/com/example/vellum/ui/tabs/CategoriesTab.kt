package com.example.vellum.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vellum.theme.*
import com.example.vellum.ui.components.HorizontalDivider
import com.example.vellum.ui.components.ParchmentBackground
import com.example.vellum.ui.components.getIconForName
import com.example.vellum.ui.dialogs.AddCategoryDialog
import com.example.vellum.ui.main.MainScreenViewModel
import com.example.vellum.data.local.CategoryEntity

@Composable
fun CategoriesTab(
    viewModel: MainScreenViewModel,
    showAddCategoryDialog: Boolean,
    onDismissCategoryDialog: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val isOutlined = preferences["category_icon_style"] == "Outlined"
    var selectedType by remember { mutableStateOf("EXPENSE") }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }

    val categoryUsageCounts = remember(allTransactions) {
        allTransactions.groupBy { it.categoryId }.mapValues { it.value.size }
    }
    val filteredCategories = remember(categories, selectedType, categoryUsageCounts) {
        categories.filter { it.type == selectedType }
            .sortedByDescending { categoryUsageCounts[it.id] ?: 0 }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            // Segmented Control: EXPENSE vs INCOME
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, ParchmentDarkBrown, RoundedCornerShape(8.dp))
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (selectedType == "EXPENSE") ParchmentDarkBrown else Color.Transparent)
                        .clickable { selectedType = "EXPENSE" }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EXPENSE",
                        color = if (selectedType == "EXPENSE") ParchmentBackground else ParchmentDarkBrown,
                        fontFamily = ParchmentFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (selectedType == "INCOME") ParchmentDarkBrown else Color.Transparent)
                        .clickable { selectedType = "INCOME" }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "INCOME",
                        color = if (selectedType == "INCOME") ParchmentBackground else ParchmentDarkBrown,
                        fontFamily = ParchmentFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Categories List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredCategories) { cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                categoryToEdit = cat
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getIconForName(cat.icon, isOutlined),
                            contentDescription = cat.name,
                            tint = ParchmentDarkBrown,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = cat.name,
                            color = ParchmentDarkBrown,
                            fontFamily = ParchmentFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    HorizontalDivider(color = ParchmentLine, thickness = 0.5.dp)
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            viewModel = viewModel,
            predefinedType = selectedType,
            onDismiss = onDismissCategoryDialog
        )
    }

    if (categoryToEdit != null) {
        AddCategoryDialog(
            viewModel = viewModel,
            predefinedType = selectedType,
            categoryToEdit = categoryToEdit,
            onDismiss = { categoryToEdit = null }
        )
    }
}
