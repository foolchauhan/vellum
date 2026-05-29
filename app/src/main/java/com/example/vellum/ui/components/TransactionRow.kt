package com.example.vellum.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vellum.data.local.TransactionEntity
import com.example.vellum.theme.*
import java.text.SimpleDateFormat
import java.util.*

val EXPENSE_ICONS = listOf(
    "clothes", "eating out", "entertainment", "fuel", "general", "gifts", "holidays", 
    "kids", "shopping", "sports", "travel", "health", "education", "home", "groceries", 
    "bills", "pets"
)

val INCOME_ICONS = listOf(
    "salary", "business", "investment", "gift", "refund", "other"
)

val ACCOUNT_ICONS = listOf(
    "personal", "bank", "card", "wallet", "cash", "shared", "savings"
)

fun getIconForName(name: String, isOutlined: Boolean = false): ImageVector {
    val clean = name.lowercase().removePrefix("app_icon_")
    return if (isOutlined) {
        when (clean) {
            // Expense
            "clothes" -> Icons.Outlined.Checkroom
            "eating out" -> Icons.Outlined.Restaurant
            "entertainment" -> Icons.Outlined.Tv
            "fuel" -> Icons.Outlined.LocalGasStation
            "general" -> Icons.Outlined.Label
            "gifts" -> Icons.Outlined.CardGiftcard
            "holidays" -> Icons.Outlined.Luggage
            "kids" -> Icons.Outlined.ChildCare
            "shopping" -> Icons.Outlined.ShoppingCart
            "sports" -> Icons.Outlined.DirectionsRun
            "travel" -> Icons.Outlined.DirectionsBus
            "health" -> Icons.Outlined.MedicalServices
            "education" -> Icons.Outlined.School
            "home" -> Icons.Outlined.Home
            "groceries" -> Icons.Outlined.LocalGroceryStore
            "bills" -> Icons.Outlined.Receipt
            "pets" -> Icons.Outlined.Pets
            
            // Income
            "salary" -> Icons.Outlined.AccountBalanceWallet
            "business" -> Icons.Outlined.BusinessCenter
            "investment" -> Icons.Outlined.TrendingUp
            "gift" -> Icons.Outlined.CardGiftcard
            "refund" -> Icons.Outlined.Undo
            "other" -> Icons.Outlined.MonetizationOn
            
            // Account
            "personal" -> Icons.Outlined.Person
            "bank" -> Icons.Outlined.AccountBalance
            "card" -> Icons.Outlined.CreditCard
            "wallet" -> Icons.Outlined.Wallet
            "cash" -> Icons.Outlined.Payments
            "shared" -> Icons.Outlined.People
            "savings" -> Icons.Outlined.Savings
            
            else -> Icons.Outlined.Category
        }
    } else {
        when (clean) {
            // Expense
            "clothes" -> Icons.Default.Checkroom
            "eating out" -> Icons.Default.Restaurant
            "entertainment" -> Icons.Default.Tv
            "fuel" -> Icons.Default.LocalGasStation
            "general" -> Icons.Default.Label
            "gifts" -> Icons.Default.CardGiftcard
            "holidays" -> Icons.Default.Luggage
            "kids" -> Icons.Default.ChildCare
            "shopping" -> Icons.Default.ShoppingCart
            "sports" -> Icons.Default.DirectionsRun
            "travel" -> Icons.Default.DirectionsBus
            "health" -> Icons.Default.MedicalServices
            "education" -> Icons.Default.School
            "home" -> Icons.Default.Home
            "groceries" -> Icons.Default.LocalGroceryStore
            "bills" -> Icons.Default.Receipt
            "pets" -> Icons.Default.Pets
            
            // Income
            "salary" -> Icons.Default.AccountBalanceWallet
            "business" -> Icons.Default.BusinessCenter
            "investment" -> Icons.Default.TrendingUp
            "gift" -> Icons.Default.CardGiftcard
            "refund" -> Icons.Default.Undo
            "other" -> Icons.Default.MonetizationOn
            
            // Account
            "personal" -> Icons.Default.Person
            "bank" -> Icons.Default.AccountBalance
            "card" -> Icons.Default.CreditCard
            "wallet" -> Icons.Default.Wallet
            "cash" -> Icons.Default.Payments
            "shared" -> Icons.Default.People
            "savings" -> Icons.Default.Savings
            
            else -> Icons.Default.Category
        }
    }
}

fun getSettingsIconForName(name: String): ImageVector {
    return when (name.lowercase()) {
        "timeperiod" -> Icons.Default.CalendarToday
        "budgetmode" -> Icons.Default.AccountBalanceWallet
        "carryover" -> Icons.Default.Cached
        "hidefuture" -> Icons.Default.VisibilityOff
        "dropboxsync" -> Icons.Default.CloudQueue
        "darktheme" -> Icons.Default.Brightness2
        "shownote" -> Icons.Default.Assignment
        "currencysymbol" -> Icons.Default.MonetizationOn
        "summaryfont" -> Icons.Default.TextFields
        "categoryiconstyle" -> Icons.Default.Inbox
        "tabsposition" -> Icons.Default.FolderOpen
        "reminders" -> Icons.Default.Timer
        "autobackup" -> Icons.Default.Backup
        "passcode" -> Icons.Default.Lock
        else -> Icons.Default.Settings
    }
}


@Composable
fun TransactionRow(
    tx: TransactionEntity,
    showNote: Boolean,
    currencySymbol: String = "₹",
    categoryIcon: String = "",
    isOutlined: Boolean = false,
    onClick: () -> Unit = {},
    onDelete: () -> Unit
) {
    val date = remember(tx.timestamp) {
        val format = SimpleDateFormat("MMM d, HH:mm", Locale.US)
        format.format(Date(tx.timestamp))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, ParchmentLine, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { onClick() }
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconKey = categoryIcon.takeIf { it.isNotEmpty() } ?: tx.categoryName
            Icon(
                imageVector = getIconForName(iconKey, isOutlined),
                contentDescription = tx.categoryName,
                tint = ParchmentDarkBrown,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = tx.categoryName,
                    fontFamily = ParchmentFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = ParchmentDarkBrown
                )
                if (showNote && tx.note.isNotEmpty()) {
                    Text(
                        text = tx.note,
                        fontFamily = ParchmentFontFamily,
                        fontSize = 12.sp,
                        color = ParchmentDarkBrown.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = "$date | ${tx.accountName}",
                    fontFamily = ParchmentFontFamily,
                    fontSize = 11.sp,
                    color = ParchmentDarkBrown.copy(alpha = 0.5f)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            val prefix = if (tx.type == "INCOME") "+" else "-"
            val color = if (tx.type == "INCOME") ChalkGreen else ChalkRed
            Text(
                text = String.format(Locale.US, "%s%s%.2f", prefix, currencySymbol, tx.amount),
                fontFamily = ParchmentFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = ParchmentDarkBrown.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
