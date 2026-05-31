package com.example.vellum.ui.main

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.vellum.data.local.AccountEntity
import com.example.vellum.theme.*
import com.example.vellum.ui.components.*
import com.example.vellum.ui.components.HorizontalDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountScreen(
    viewModel: MainScreenViewModel,
    accountId: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val isOutlined = preferences["category_icon_style"] == "Outlined"

    val accountToEdit = remember(accountId, accounts) {
        accountId?.let { id -> accounts.find { it.id == id } }
    }

    var name by remember { mutableStateOf(accountToEdit?.name ?: "") }
    var selectedIconName by remember { mutableStateOf(accountToEdit?.icon ?: "personal") }
    var selectedColorHex by remember { mutableStateOf<String?>(accountToEdit?.color) }
    var isShared by remember { mutableStateOf(accountToEdit?.shareCode != null) }

    var showIconPickerDialog by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }

    val colorsPalette = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7",
        "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
        "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
        "#FFEB3B", "#FFC107", "#FF9800", "#FF5722",
        "#795548", "#9E9E9E", "#607D8B"
    )

    // Sharing Owner calculation
    val isOwner = remember(accountToEdit, userEmail) {
        accountToEdit == null || accountToEdit.ownerEmail == null || accountToEdit.ownerEmail == userEmail
    }

    val isNestedBlurActive = showIconPickerDialog || showColorPickerDialog

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
                        text = if (accountToEdit != null) "Edit Account" else "Add Account",
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
                                if (name.trim().isEmpty()) {
                                    Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }
                                val isDuplicate = accounts.any {
                                    it.id != accountToEdit?.id && it.name.trim().lowercase() == name.trim().lowercase()
                                }
                                if (isDuplicate) {
                                    Toast.makeText(context, "Account already exists", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }

                                if (isShared && userEmail == null) {
                                    Toast.makeText(context, "Please Sign In to share accounts", Toast.LENGTH_LONG).show()
                                    return@clickable
                                }

                                val shareCodeVal = if (isShared) {
                                    accountToEdit?.shareCode ?: ("FAM" + (100..999).random())
                                } else {
                                    null
                                }
                                val ownerEmailVal = if (isShared) {
                                    accountToEdit?.ownerEmail ?: userEmail
                                } else {
                                    null
                                }

                                viewModel.addAccount(
                                    name = name.trim(),
                                    icon = selectedIconName,
                                    color = selectedColorHex ?: "#4E3C30",
                                    id = accountToEdit?.id,
                                    isDefault = accountToEdit?.isDefault ?: false,
                                    shareCode = shareCodeVal,
                                    ownerEmail = ownerEmailVal
                                )
                                onBack()
                            }
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section Header
                FormSectionHeader(title = "Account Details")
                HorizontalDivider(color = ParchmentLine, thickness = 0.5.dp)

                // Name Row
                FormRow(label = "Name") {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Name", color = Color.Gray, fontFamily = ParchmentFontFamily) },
                        singleLine = true,
                        enabled = isOwner,
                        textStyle = TextStyle(
                            color = if (isOwner) ParchmentDarkBrown else Color.Gray,
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
                            .clickable(enabled = isOwner) { showIconPickerDialog = true }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = getIconForName(selectedIconName, isOutlined),
                                contentDescription = selectedIconName,
                                tint = if (isOwner) ParchmentDarkBrown else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedIconName.replaceFirstChar { it.uppercase() },
                                color = if (isOwner) ParchmentDarkBrown else Color.Gray,
                                fontFamily = ParchmentFontFamily,
                                fontSize = 16.sp
                            )
                        }
                        if (isOwner) {
                            IconButton(onClick = { selectedIconName = "personal" }) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Clear",
                                    tint = ParchmentDarkBrown.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                // Color Row
                FormRow(label = "Colour") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isOwner) { showColorPickerDialog = true }
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
                            if (isOwner) {
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
                }

                // Sharing Switch Row
                FormRow(label = "Sharing") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Share with Household",
                            color = if (isOwner) ParchmentDarkBrown else Color.Gray,
                            fontFamily = ParchmentFontFamily,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isShared,
                            onCheckedChange = { checked ->
                                if (isOwner) {
                                    isShared = checked
                                }
                            },
                            enabled = isOwner,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ParchmentDarkBrown,
                                checkedTrackColor = ParchmentLine
                            )
                        )
                    }
                }

                // Share Code Display
                if (accountToEdit != null && accountToEdit.shareCode != null && isOwner) {
                    Spacer(modifier = Modifier.height(16.dp))
                    FormRow(label = "Share Code") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = accountToEdit.shareCode,
                                color = ParchmentBlueText,
                                fontFamily = ParchmentFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                letterSpacing = 3.sp
                            )
                            IconButton(onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Join my Vellum shared account")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Join my shared account on Vellum!\n\nUse this code: ${accountToEdit.shareCode}\n\nOpen Vellum → Accounts → Join Shared Account → enter the code above."
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share via..."))
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share code",
                                    tint = ParchmentDarkBrown
                                )
                            }
                        }
                    }
                }

                if (accountToEdit != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (isOwner) {
                                viewModel.deleteAccount(accountToEdit)
                            } else {
                                viewModel.leaveSharedAccount(accountToEdit)
                            }
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isOwner) "Delete Account" else "Leave Shared Account",
                            fontFamily = ParchmentFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    if (showIconPickerDialog) {
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
                    val rows = ACCOUNT_ICONS.chunked(4)
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
                        text = "Select Color",
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
