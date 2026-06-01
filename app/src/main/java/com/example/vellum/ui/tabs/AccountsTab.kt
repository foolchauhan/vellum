package com.example.vellum.ui.tabs

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vellum.theme.*
import com.example.vellum.ui.components.HorizontalDivider
import com.example.vellum.ui.components.ParchmentBackground
import com.example.vellum.ui.components.getIconForName
import com.example.vellum.ui.dialogs.JoinSharedAccountDialog
import com.example.vellum.ui.main.MainScreenViewModel
import com.example.vellum.data.local.AccountEntity

@Composable
fun AccountsTab(
    viewModel: MainScreenViewModel,
    onNavigate: (androidx.navigation3.runtime.NavKey) -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val currentUserEmail by viewModel.userEmail.collectAsState()
    val isOutlined = preferences["category_icon_style"] == "Outlined"
    var showJoinDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .blur(if (showJoinDialog) 10.dp else 0.dp)
        ) {

            // Account List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(accounts) { acc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onNavigate(com.example.vellum.AddEditAccount(accountId = acc.id))
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iconName = if (acc.shareCode != null) "shared" else (acc.icon.takeIf { it.isNotEmpty() } ?: "personal")
                        Icon(
                            imageVector = getIconForName(iconName, isOutlined),
                            contentDescription = acc.name,
                            tint = ParchmentDarkBrown,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = acc.name,
                                color = ParchmentDarkBrown,
                                fontFamily = ParchmentFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (acc.shareCode != null) {
                                Text(
                                    text = "Share Code: ${acc.shareCode}",
                                    color = ParchmentBlueText,
                                    fontFamily = ParchmentFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (acc.ownerEmail != null && acc.ownerEmail != currentUserEmail) {
                                Text(
                                    text = "Owner: ${acc.ownerEmail}",
                                    color = ParchmentDarkBrown.copy(alpha = 0.8f),
                                    fontFamily = ParchmentFontFamily,
                                    fontSize = 12.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Carry Over: ",
                                    color = ParchmentDarkBrown.copy(alpha = 0.8f),
                                    fontFamily = ParchmentFontFamily,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = if (acc.carryOver) "Enabled" else "Disabled",
                                    color = if (acc.carryOver) ChalkGreen else ChalkRed,
                                    fontFamily = ParchmentFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = ParchmentLine, thickness = 0.5.dp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showJoinDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = ParchmentDarkBrown),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ParchmentDarkBrown, RoundedCornerShape(8.dp)),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(text = "+ Join Shared Account", fontFamily = ParchmentFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom description text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "You can add extra Accounts here. For example you may want to have different accounts for different people, or have a separate 'Savings' account.",
                    color = ParchmentDarkBrown.copy(alpha = 0.6f),
                    fontFamily = ParchmentFontFamily,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    }

    if (showJoinDialog) {
        JoinSharedAccountDialog(
            viewModel = viewModel,
            onDismiss = { showJoinDialog = false }
        )
    }
}

