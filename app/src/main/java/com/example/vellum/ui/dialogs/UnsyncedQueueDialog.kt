package com.example.vellum.ui.dialogs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.vellum.theme.*
import com.example.vellum.ui.components.getIconForName
import com.example.vellum.ui.main.MainScreenViewModel
import com.example.vellum.data.local.TransactionEntity
import com.example.vellum.data.local.CategoryEntity
import com.example.vellum.data.local.AccountEntity
import com.example.vellum.data.local.StickyNoteEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UnsyncedQueueDialog(
    viewModel: MainScreenViewModel,
    onDismiss: () -> Unit
) {
    val unsyncedTxs by viewModel.unsyncedTransactions.collectAsState()
    val unsyncedCats by viewModel.unsyncedCategories.collectAsState()
    val unsyncedAccs by viewModel.unsyncedAccounts.collectAsState()
    val unsyncedNotes by viewModel.unsyncedNotes.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val currencySymbol = remember(preferences) {
        when (val sym = preferences["currency_symbol"]) {
            "Default" -> "₹"
            "INR" -> "₹"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> sym ?: "₹"
        }
    }

    var activeTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Transactions", "Categories", "Accounts", "Notes")

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US) }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Title
                Text(
                    text = "Pending Sync Queue",
                    fontFamily = ParchmentFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = ParchmentDarkBrown,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Tab Row for selecting deck type
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.Transparent,
                    contentColor = ParchmentDarkBrown,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = ParchmentDarkBrown
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = activeTab == index,
                            onClick = { activeTab = index },
                            text = {
                                val count = when (index) {
                                    0 -> unsyncedTxs.size
                                    1 -> unsyncedCats.size
                                    2 -> unsyncedAccs.size
                                    3 -> unsyncedNotes.size
                                    else -> 0
                                }
                                Text(
                                    text = "$title ($count)",
                                    fontFamily = ParchmentFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            selectedContentColor = ParchmentDarkBrown,
                            unselectedContentColor = TabUnselected
                        )
                    }
                }

                // Deck views inside box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (activeTab) {
                        0 -> {
                            if (unsyncedTxs.isEmpty()) {
                                EmptySyncDeck(message = "All transactions are fully synchronized with Google Sheets!")
                            } else {
                                val txPagerState = rememberPagerState(pageCount = { unsyncedTxs.size })
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    HorizontalPager(
                                        state = txPagerState,
                                        modifier = Modifier.weight(1f)
                                    ) { page ->
                                        val tx = unsyncedTxs[page]
                                        TransactionDeckCard(tx = tx, dateFormat = dateFormat, currencySymbol = currencySymbol)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${txPagerState.currentPage + 1} of ${unsyncedTxs.size}",
                                        fontFamily = ParchmentFontFamily,
                                        fontSize = 12.sp,
                                        color = ParchmentDarkBrown.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                        1 -> {
                            if (unsyncedCats.isEmpty()) {
                                EmptySyncDeck(message = "All categories are fully synchronized with Google Sheets!")
                            } else {
                                val catPagerState = rememberPagerState(pageCount = { unsyncedCats.size })
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    HorizontalPager(
                                        state = catPagerState,
                                        modifier = Modifier.weight(1f)
                                    ) { page ->
                                        val cat = unsyncedCats[page]
                                        CategoryDeckCard(cat = cat, currencySymbol = currencySymbol)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${catPagerState.currentPage + 1} of ${unsyncedCats.size}",
                                        fontFamily = ParchmentFontFamily,
                                        fontSize = 12.sp,
                                        color = ParchmentDarkBrown.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                        2 -> {
                            if (unsyncedAccs.isEmpty()) {
                                EmptySyncDeck(message = "All accounts are fully synchronized with Google Sheets!")
                            } else {
                                val accPagerState = rememberPagerState(pageCount = { unsyncedAccs.size })
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    HorizontalPager(
                                        state = accPagerState,
                                        modifier = Modifier.weight(1f)
                                    ) { page ->
                                        val acc = unsyncedAccs[page]
                                        AccountDeckCard(acc = acc)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${accPagerState.currentPage + 1} of ${unsyncedAccs.size}",
                                        fontFamily = ParchmentFontFamily,
                                        fontSize = 12.sp,
                                        color = ParchmentDarkBrown.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                        3 -> {
                            if (unsyncedNotes.isEmpty()) {
                                EmptySyncDeck(message = "All sticky notes are fully synchronized with Google Sheets!")
                            } else {
                                val notesPagerState = rememberPagerState(pageCount = { unsyncedNotes.size })
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    HorizontalPager(
                                        state = notesPagerState,
                                        modifier = Modifier.weight(1f)
                                    ) { page ->
                                        val note = unsyncedNotes[page]
                                        StickyNoteDeckCard(note = note)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${notesPagerState.currentPage + 1} of ${unsyncedNotes.size}",
                                        fontFamily = ParchmentFontFamily,
                                        fontSize = 12.sp,
                                        color = ParchmentDarkBrown.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dismiss Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = ParchmentDarkBrown),
                    modifier = Modifier.fillMaxWidth(0.5f)
                ) {
                    Text(
                        text = "Close",
                        fontFamily = ParchmentFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = ParchmentBackground
                    )
                }
            }
        }
    }
}

@Composable
fun StickyNoteDeckCard(note: StickyNoteEntity) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .border(1.dp, ParchmentDarkBrown, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sticky Note",
                    fontFamily = ParchmentFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = ParchmentDarkBrown
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            try {
                                Color(android.graphics.Color.parseColor(note.colorHex))
                            } catch (e: Exception) {
                                ParchmentDarkBrown
                            }
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = note.content,
                fontFamily = ParchmentFontFamily,
                fontSize = 14.sp,
                color = ParchmentDarkBrown,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Bottom
            ) {
                val status = when {
                    note.isDeleted -> "Deleted"
                    note.updatedAt > 0 -> "Edited"
                    else -> "New"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ParchmentBlueText.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = status,
                        fontFamily = ParchmentFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ParchmentBlueText
                    )
                }
            }
        }
    }
}

@Composable
fun EmptySyncDeck(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, ParchmentLine, RoundedCornerShape(8.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontFamily = ParchmentFontFamily,
            fontSize = 14.sp,
            color = ParchmentDarkBrown.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TransactionDeckCard(tx: TransactionEntity, dateFormat: SimpleDateFormat, currencySymbol: String) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .border(1.dp, ParchmentDarkBrown, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getIconForName(tx.categoryName),
                        contentDescription = tx.categoryName,
                        tint = ParchmentDarkBrown,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tx.categoryName,
                        fontFamily = ParchmentFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ParchmentDarkBrown
                    )
                }
                
                val (prefix, color) = if (tx.type == "INCOME") Pair("+", ChalkGreen) else Pair("-", ChalkRed)
                Text(
                    text = String.format(Locale.US, "%s%s%.2f", prefix, currencySymbol, tx.amount),
                    fontFamily = ParchmentFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Account: ${tx.accountName}",
                    fontFamily = ParchmentFontFamily,
                    fontSize = 14.sp,
                    color = ParchmentDarkBrown
                )
                if (tx.note.isNotEmpty()) {
                    Text(
                        text = "Note: ${tx.note}",
                        fontFamily = ParchmentFontFamily,
                        fontSize = 14.sp,
                        color = ParchmentDarkBrown.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = dateFormat.format(Date(tx.timestamp)),
                    fontFamily = ParchmentFontFamily,
                    fontSize = 12.sp,
                    color = ParchmentDarkBrown.copy(alpha = 0.5f)
                )
                
                val status = when {
                    tx.isDeleted -> "Deleted"
                    tx.updatedAt > tx.timestamp -> "Edited"
                    else -> "New"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ParchmentBlueText.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = status,
                        fontFamily = ParchmentFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ParchmentBlueText
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryDeckCard(cat: CategoryEntity, currencySymbol: String) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .border(1.dp, ParchmentDarkBrown, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getIconForName(cat.icon),
                        contentDescription = cat.name,
                        tint = ParchmentDarkBrown,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = cat.name,
                        fontFamily = ParchmentFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ParchmentDarkBrown
                    )
                }

                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            try {
                                Color(android.graphics.Color.parseColor(cat.chartColor))
                            } catch (e: Exception) {
                                ParchmentDarkBrown
                            }
                        )
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Type: ${cat.type}",
                    fontFamily = ParchmentFontFamily,
                    fontSize = 14.sp,
                    color = ParchmentDarkBrown
                )
                if (cat.budget > 0.0) {
                    Text(
                        text = String.format(Locale.US, "Budget Limit: %s%.2f", currencySymbol, cat.budget),
                        fontFamily = ParchmentFontFamily,
                        fontSize = 14.sp,
                        color = ParchmentDarkBrown
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (cat.isDefault) "Default Category" else "Custom Category",
                    fontFamily = ParchmentFontFamily,
                    fontSize = 12.sp,
                    color = ParchmentDarkBrown.copy(alpha = 0.5f)
                )

                val status = when {
                    cat.isDeleted -> "Deleted"
                    cat.updatedAt > 0 -> "Edited"
                    else -> "New"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ParchmentBlueText.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = status,
                        fontFamily = ParchmentFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ParchmentBlueText
                    )
                }
            }
        }
    }
}

@Composable
fun AccountDeckCard(acc: AccountEntity) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .border(1.dp, ParchmentDarkBrown, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getIconForName(acc.icon),
                        contentDescription = acc.name,
                        tint = ParchmentDarkBrown,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = acc.name,
                        fontFamily = ParchmentFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ParchmentDarkBrown
                    )
                }

                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            try {
                                Color(android.graphics.Color.parseColor(acc.color))
                            } catch (e: Exception) {
                                ParchmentDarkBrown
                            }
                        )
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Carry Over Balance: ${if (acc.carryOver) "Yes" else "No"}",
                    fontFamily = ParchmentFontFamily,
                    fontSize = 14.sp,
                    color = ParchmentDarkBrown
                )
                if (!acc.shareCode.isNullOrEmpty()) {
                    Text(
                        text = "Household Code: ${acc.shareCode}",
                        fontFamily = ParchmentFontFamily,
                        fontSize = 14.sp,
                        color = ParchmentDarkBrown,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (acc.isDefault) "Default Account" else "Sub-account",
                    fontFamily = ParchmentFontFamily,
                    fontSize = 12.sp,
                    color = ParchmentDarkBrown.copy(alpha = 0.5f)
                )

                val status = when {
                    acc.isDeleted -> "Deleted"
                    acc.updatedAt > 0 -> "Edited"
                    else -> "New"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ParchmentBlueText.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = status,
                        fontFamily = ParchmentFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ParchmentBlueText
                    )
                }
            }
        }
    }
}
