package com.example.vellum.ui.dialogs

import android.content.Intent
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.vellum.theme.*
import com.example.vellum.ui.components.FormRow
import com.example.vellum.ui.components.FormSectionHeader
import com.example.vellum.ui.components.HorizontalDivider
import com.example.vellum.ui.components.ParchmentBackground
import com.example.vellum.ui.main.MainScreenViewModel
import com.example.vellum.data.local.AccountEntity
import com.example.vellum.ui.components.ACCOUNT_ICONS
import com.example.vellum.ui.components.getIconForName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountDialog(
    viewModel: MainScreenViewModel,
    accountToEdit: AccountEntity? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val isOutlined = preferences["category_icon_style"] == "Outlined"

    var name by remember { mutableStateOf(accountToEdit?.name ?: "") }
    var selectedIconName by remember { mutableStateOf(accountToEdit?.icon ?: "personal") }
    var selectedColorHex by remember { mutableStateOf<String?>(accountToEdit?.color) }
    var isShared by remember { mutableStateOf(accountToEdit?.shareCode != null) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var showIconPickerDialog by remember { mutableStateOf(false) }
    // Share code popup: shown after creating a new shared account
    var pendingShareCode by remember { mutableStateOf<String?>(null) }

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
                val shareCode = pendingShareCode
                if (shareCode != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Shared Account Created!",
                            color = ParchmentDarkBrown,
                            fontFamily = ParchmentFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Share this code with household members so they can join:",
                            color = ParchmentDarkBrown.copy(alpha = 0.7f),
                            fontFamily = ParchmentFontFamily,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        // Big share code display
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ParchmentDarkBrown.copy(alpha = 0.1f))
                                .border(2.dp, ParchmentDarkBrown, RoundedCornerShape(12.dp))
                                .padding(vertical = 20.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = shareCode,
                                color = ParchmentDarkBrown,
                                fontFamily = ParchmentFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                letterSpacing = 6.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Join my Vellum shared account")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Join my shared account on Vellum!\n\nUse this code: $shareCode\n\nOpen Vellum → Accounts → Join Shared Account → enter the code above."
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share via..."))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ParchmentDarkBrown,
                                contentColor = ParchmentBackground
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Share via...",
                                fontFamily = ParchmentFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = {
                                pendingShareCode = null
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Done",
                                color = ParchmentDarkBrown.copy(alpha = 0.7f),
                                fontFamily = ParchmentFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                } else {
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
                                    val isDuplicate = accounts.any {
                                        it.id != accountToEdit?.id && it.name.trim().lowercase() == name.trim().lowercase()
                                    }
                                    if (isDuplicate) {
                                        Toast.makeText(context, "Account already exists", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }
                                    if (isShared) {
                                        val shareCode = accountToEdit?.shareCode ?: ("FAM" + (100..999).random())
                                        val ownerEmail = accountToEdit?.ownerEmail ?: userEmail ?: ""
                                        viewModel.addAccount(
                                            name = name.trim(),
                                            icon = selectedIconName,
                                            color = selectedColorHex ?: "#4E3C30",
                                            id = accountToEdit?.id,
                                            isDefault = accountToEdit?.isDefault ?: false,
                                            shareCode = shareCode,
                                            ownerEmail = ownerEmail
                                        )
                                        // Show share code popup only when FIRST enabling sharing
                                        if (accountToEdit == null || accountToEdit.shareCode == null) {
                                            pendingShareCode = shareCode
                                        } else {
                                            onDismiss()
                                        }
                                    } else {
                                        viewModel.addAccount(
                                            name = name.trim(),
                                            icon = selectedIconName,
                                            color = selectedColorHex ?: "#4E3C30",
                                            id = accountToEdit?.id,
                                            isDefault = accountToEdit?.isDefault ?: false,
                                            shareCode = null,
                                            ownerEmail = null
                                        )
                                        onDismiss()
                                    }
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
                            IconButton(onClick = { selectedIconName = "personal" }) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Clear",
                                    tint = ParchmentDarkBrown.copy(alpha = 0.5f)
                                )
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

                    // Share Row
                    val isOwner = remember(accountToEdit, userEmail) {
                        accountToEdit == null || accountToEdit.ownerEmail == null || accountToEdit.ownerEmail == userEmail
                    }
                    FormRow(label = "Sharing") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Share with Household",
                                color = ParchmentDarkBrown,
                                fontFamily = ParchmentFontFamily,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = isShared,
                                onCheckedChange = { checked ->
                                    if (userEmail != null) {
                                        if (isOwner) {
                                            isShared = checked
                                        } else {
                                            Toast.makeText(context, "Only the owner can modify sharing for this account", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Sign in with Google to share accounts", Toast.LENGTH_SHORT).show()
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

                    // Show existing share code if account is already shared
                    if (accountToEdit != null && accountToEdit.shareCode != null && isOwner) {
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
                                viewModel.deleteAccount(accountToEdit)
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
        }
    }

    // ── Color Picker Dialog ──────────────────────────────────────────────────
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
                        text = "Select Account Color",
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

    // ── Icon Picker Dialog ───────────────────────────────────────────────────
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


}
