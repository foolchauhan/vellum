package com.example.vellum.ui.dialogs

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.vellum.theme.*
import com.example.vellum.ui.components.*
import com.example.vellum.ui.components.HorizontalDivider
import com.example.vellum.ui.main.MainScreenViewModel
import com.example.vellum.data.local.CategoryEntity
import com.example.vellum.ui.components.EXPENSE_ICONS
import com.example.vellum.ui.components.INCOME_ICONS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryDialog(
    viewModel: MainScreenViewModel,
    predefinedType: String,
    categoryToEdit: CategoryEntity? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val isOutlined = preferences["category_icon_style"] == "Outlined"
    var name by remember { mutableStateOf(categoryToEdit?.name ?: "") }
    var type by remember { mutableStateOf(categoryToEdit?.type ?: predefinedType) }
    var selectedIconName by remember { mutableStateOf(categoryToEdit?.icon ?: "general") }
    var selectedColorHex by remember { mutableStateOf<String?>(categoryToEdit?.chartColor) }

    var showIconPickerDialog by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }

    val colorsPalette = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7",
        "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
        "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
        "#FFEB3B", "#FFC107", "#FF9800", "#FF5722",
        "#795548", "#9E9E9E", "#607D8B"
    )

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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
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
                                    if (name.trim().isEmpty()) {
                                        return@clickable
                                    }
                                    val isDuplicate = categories.any {
                                        it.id != categoryToEdit?.id && it.name.trim().lowercase() == name.trim().lowercase() && it.type == type
                                    }
                                    if (isDuplicate) {
                                        Toast.makeText(context, "Category already exists", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }
                                    viewModel.addCategory(
                                        name = name.trim(),
                                        type = type,
                                        icon = selectedIconName,
                                        chartColor = selectedColorHex ?: "#4E3C30",
                                        id = categoryToEdit?.id,
                                        isDefault = categoryToEdit?.isDefault ?: false
                                    )
                                    onDismiss()
                                }
                                .padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section Header
                    FormSectionHeader(title = "Category Details")
                    HorizontalDivider(color = ParchmentLine, thickness = 0.5.dp)

                    // Name Row
                    FormRow(label = "Name") {
                        TextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("Name", color = Color.Gray, fontFamily = ParchmentFontFamily) },
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

                    // Icon Row
                    FormRow(label = "Icon") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showIconPickerDialog = true }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = getIconForName(selectedIconName, isOutlined),
                                    contentDescription = selectedIconName,
                                    tint = ParchmentDarkBrown,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedIconName.replaceFirstChar { it.uppercase() },
                                    color = ParchmentDarkBrown,
                                    fontFamily = ParchmentFontFamily,
                                    fontSize = 16.sp
                                )
                            }
                            IconButton(onClick = { selectedIconName = "general" }) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Clear",
                                    tint = ParchmentDarkBrown.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    // Chart Color Row
                    FormRow(label = "Chart Colour") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showColorPickerDialog = true }
                        ) {
                            Text(
                                text = if (selectedColorHex != null) "Selected Color" else "Not Entered",
                                color = if (selectedColorHex != null) Color(android.graphics.Color.parseColor(selectedColorHex)) else Color.Gray,
                                fontFamily = ParchmentFontFamily,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (selectedColorHex != null) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(android.graphics.Color.parseColor(selectedColorHex)))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { selectedColorHex = null }) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = "Clear",
                                        tint = ParchmentDarkBrown.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    if (categoryToEdit != null) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                viewModel.deleteCategory(categoryToEdit)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Delete Category",
                                fontFamily = ParchmentFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showIconPickerDialog) {
        val iconsList = if (type == "EXPENSE") EXPENSE_ICONS else INCOME_ICONS
        Dialog(onDismissRequest = { showIconPickerDialog = false }) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ParchmentBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, ParchmentLine, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select Icon",
                        color = ParchmentDarkBrown,
                        fontFamily = ParchmentFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    val rows = iconsList.chunked(4)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rows.forEach { rowItems ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowItems.forEach { iconName ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .border(
                                                1.dp,
                                                if (selectedIconName == iconName) ParchmentDarkBrown else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedIconName = iconName
                                                showIconPickerDialog = false
                                            }
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = getIconForName(iconName, isOutlined),
                                                contentDescription = iconName,
                                                tint = ParchmentDarkBrown,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = iconName,
                                                fontSize = 9.sp,
                                                color = ParchmentDarkBrown,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { showIconPickerDialog = false }) {
                        Text("CANCEL", color = ParchmentDarkBrown, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showColorPickerDialog) {
        Dialog(onDismissRequest = { showColorPickerDialog = false }) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ParchmentBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, ParchmentLine, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select Chart Color",
                        color = ParchmentDarkBrown,
                        fontFamily = ParchmentFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    val chunkedColors = colorsPalette.chunked(4)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        chunkedColors.forEach { colorRow ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                colorRow.forEach { colorHex ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(android.graphics.Color.parseColor(colorHex)))
                                            .border(
                                                2.dp,
                                                if (selectedColorHex == colorHex) ParchmentDarkBrown else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedColorHex = colorHex
                                                showColorPickerDialog = false
                                            }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { showColorPickerDialog = false }) {
                        Text("CANCEL", color = ParchmentDarkBrown, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
