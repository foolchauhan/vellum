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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawWithContent
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
    "bills", "pets", "beauty", "fitness", "insurance", "taxes", "donations", "subscriptions",
    "repairs", "coffee", "parking", "utilities", "electronics", "hobbies", "toys", "music",
    "movies", "drinks", "bakery", "fast food", "laundry", "furniture", "gardening", "software",
    "games", "salon", "flights", "trains", "hotel", "camera", "cleaning", "jewelry", "automotive",
    "baby", "optics", "hardware", "books", "stationery"
)

val INCOME_ICONS = listOf(
    "salary", "business", "investment", "gift", "refund", "other", "freelance", "rental", 
    "awards", "crypto", "interest", "cashback", "allowance", "royalties", "grants", "lottery",
    "sales", "stipend"
)

val ACCOUNT_ICONS = listOf(
    "personal", "bank", "card", "wallet", "cash", "shared", "savings", "crypto wallet", 
    "credit line", "salary account", "investment account"
)

fun getIconForName(name: String, isOutlined: Boolean = false): ImageVector {
    val clean = name.lowercase().removePrefix("app_icon_").replace("_", " ").trim()
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
            "beauty" -> Icons.Outlined.Spa
            "fitness" -> Icons.Outlined.FitnessCenter
            "insurance" -> Icons.Outlined.Shield
            "taxes" -> Icons.Outlined.Percent
            "donations" -> Icons.Outlined.VolunteerActivism
            "subscriptions" -> Icons.Outlined.Subscriptions
            "repairs" -> Icons.Outlined.Handyman
            "coffee" -> Icons.Outlined.LocalCafe
            "parking" -> Icons.Outlined.LocalParking
            "utilities" -> Icons.Outlined.Bolt
            "electronics" -> Icons.Outlined.Devices
            "hobbies" -> Icons.Outlined.Palette
            "toys" -> Icons.Outlined.SmartToy
            "music" -> Icons.Outlined.MusicNote
            "movies" -> Icons.Outlined.Movie
            "drinks" -> Icons.Outlined.LocalBar
            "bakery" -> Icons.Outlined.Cake
            "fast food" -> Icons.Outlined.Fastfood
            "laundry" -> Icons.Outlined.LocalLaundryService
            "furniture" -> Icons.Outlined.Weekend
            "gardening" -> Icons.Outlined.Yard
            "software" -> Icons.Outlined.Terminal
            "games" -> Icons.Outlined.SportsEsports
            "salon" -> Icons.Outlined.Brush
            "flights" -> Icons.Outlined.Flight
            "trains" -> Icons.Outlined.Train
            "hotel" -> Icons.Outlined.Hotel
            "camera" -> Icons.Outlined.PhotoCamera
            "cleaning" -> Icons.Outlined.CleaningServices
            "jewelry" -> Icons.Outlined.Diamond
            "automotive" -> Icons.Outlined.DirectionsCar
            "baby" -> Icons.Outlined.BabyChangingStation
            "optics" -> Icons.Outlined.Visibility
            "hardware" -> Icons.Outlined.Hardware
            "books" -> Icons.Outlined.MenuBook
            "stationery" -> Icons.Outlined.Create

            // Income
            "salary" -> Icons.Outlined.AccountBalanceWallet
            "business" -> Icons.Outlined.BusinessCenter
            "investment" -> Icons.Outlined.TrendingUp
            "gift" -> Icons.Outlined.CardGiftcard
            "refund" -> Icons.Outlined.Undo
            "other" -> Icons.Outlined.MonetizationOn
            "freelance" -> Icons.Outlined.LaptopMac
            "rental" -> Icons.Outlined.Key
            "awards" -> Icons.Outlined.EmojiEvents
            "crypto" -> Icons.Outlined.CurrencyBitcoin
            "interest" -> Icons.Outlined.AddCard
            "cashback" -> Icons.Outlined.PriceCheck
            "allowance" -> Icons.Outlined.SupervisedUserCircle
            "royalties" -> Icons.Outlined.Copyright
            "grants" -> Icons.Outlined.CardMembership
            "lottery" -> Icons.Outlined.Casino
            "sales" -> Icons.Outlined.LocalMall
            "stipend" -> Icons.Outlined.ReceiptLong

            // Account
            "personal" -> Icons.Outlined.Person
            "bank" -> Icons.Outlined.AccountBalance
            "card" -> Icons.Outlined.CreditCard
            "wallet" -> Icons.Outlined.Wallet
            "cash" -> Icons.Outlined.Payments
            "shared" -> Icons.Outlined.People
            "savings" -> Icons.Outlined.Savings
            "crypto wallet" -> Icons.Outlined.CurrencyExchange
            "credit line" -> Icons.Outlined.CreditScore
            "salary account" -> Icons.Outlined.Domain
            "investment account" -> Icons.Outlined.Analytics
            
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
            "beauty" -> Icons.Default.Spa
            "fitness" -> Icons.Default.FitnessCenter
            "insurance" -> Icons.Default.Shield
            "taxes" -> Icons.Default.Percent
            "donations" -> Icons.Default.VolunteerActivism
            "subscriptions" -> Icons.Default.Subscriptions
            "repairs" -> Icons.Default.Handyman
            "coffee" -> Icons.Default.LocalCafe
            "parking" -> Icons.Default.LocalParking
            "utilities" -> Icons.Default.Bolt
            "electronics" -> Icons.Default.Devices
            "hobbies" -> Icons.Default.Palette
            "toys" -> Icons.Default.SmartToy
            "music" -> Icons.Default.MusicNote
            "movies" -> Icons.Default.Movie
            "drinks" -> Icons.Default.LocalBar
            "bakery" -> Icons.Default.Cake
            "fast food" -> Icons.Default.Fastfood
            "laundry" -> Icons.Default.LocalLaundryService
            "furniture" -> Icons.Default.Weekend
            "gardening" -> Icons.Default.Yard
            "software" -> Icons.Default.Terminal
            "games" -> Icons.Default.SportsEsports
            "salon" -> Icons.Default.Brush
            "flights" -> Icons.Default.Flight
            "trains" -> Icons.Default.Train
            "hotel" -> Icons.Default.Hotel
            "camera" -> Icons.Default.PhotoCamera
            "cleaning" -> Icons.Default.CleaningServices
            "jewelry" -> Icons.Default.Diamond
            "automotive" -> Icons.Default.DirectionsCar
            "baby" -> Icons.Default.BabyChangingStation
            "optics" -> Icons.Default.Visibility
            "hardware" -> Icons.Default.Hardware
            "books" -> Icons.Default.MenuBook
            "stationery" -> Icons.Default.Create

            // Income
            "salary" -> Icons.Default.AccountBalanceWallet
            "business" -> Icons.Default.BusinessCenter
            "investment" -> Icons.Default.TrendingUp
            "gift" -> Icons.Default.CardGiftcard
            "refund" -> Icons.Default.Undo
            "other" -> Icons.Default.MonetizationOn
            "freelance" -> Icons.Default.LaptopMac
            "rental" -> Icons.Default.Key
            "awards" -> Icons.Default.EmojiEvents
            "crypto" -> Icons.Default.CurrencyBitcoin
            "interest" -> Icons.Default.AddCard
            "cashback" -> Icons.Default.PriceCheck
            "allowance" -> Icons.Default.SupervisedUserCircle
            "royalties" -> Icons.Default.Copyright
            "grants" -> Icons.Default.CardMembership
            "lottery" -> Icons.Default.Casino
            "sales" -> Icons.Default.LocalMall
            "stipend" -> Icons.Default.ReceiptLong

            // Account
            "personal" -> Icons.Default.Person
            "bank" -> Icons.Default.AccountBalance
            "card" -> Icons.Default.CreditCard
            "wallet" -> Icons.Default.Wallet
            "cash" -> Icons.Default.Payments
            "shared" -> Icons.Default.People
            "savings" -> Icons.Default.Savings
            "crypto wallet" -> Icons.Default.CurrencyExchange
            "credit line" -> Icons.Default.CreditScore
            "salary account" -> Icons.Default.Domain
            "investment account" -> Icons.Default.Analytics
            
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
        "financialtutor" -> Icons.Default.School
        "download" -> Icons.Default.ArrowDownward
        "upload" -> Icons.Default.ArrowUpward
        else -> Icons.Default.Settings
    }
}


@Composable
fun TransactionRow(
    tx: TransactionEntity,
    showNote: Boolean,
    currencySymbol: String = "₹",
    categoryIcon: String = "",
    categoryName: String = tx.categoryName,
    accountName: String = tx.accountName,
    isOutlined: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onDelete: () -> Unit
) {
    val date = remember(tx.timestamp) {
        val format = SimpleDateFormat("MMM d, HH:mm", Locale.US)
        format.format(Date(tx.timestamp))
    }

    var isDeleting by remember { androidx.compose.runtime.mutableStateOf(false) }
    val wipeProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isDeleting) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 600, easing = androidx.compose.animation.core.LinearEasing),
        finishedListener = {
            if (it == 1f) {
                onDelete()
            }
        },
        label = "EraserWipe"
    )

    val chalkboardBg = androidx.compose.material3.MaterialTheme.colorScheme.background
    val eraserColor = androidx.compose.ui.graphics.Color(0xFF5C4033) // Felt eraser brown
    val eraserWoodColor = androidx.compose.ui.graphics.Color(0xFF8B5A2B) // Wooden eraser handle

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                if (wipeProgress > 0f) {
                    val w = size.width
                    val h = size.height
                    val eraserX = w * (1f - wipeProgress)

                    // Overwrite wiped out content with background color
                    drawRect(
                        color = chalkboardBg,
                        topLeft = androidx.compose.ui.geometry.Offset(eraserX, 0f),
                        size = androidx.compose.ui.geometry.Size(w - eraserX, h)
                    )

                    // Draw chalk dust/smudge
                    drawCircle(
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f * wipeProgress),
                        radius = h * 0.4f,
                        center = androidx.compose.ui.geometry.Offset(eraserX + 10f, h / 2f)
                    )

                    // Draw the felt eraser
                    val eraserWidth = 40.dp.toPx()
                    drawRoundRect(
                        color = eraserColor,
                        topLeft = androidx.compose.ui.geometry.Offset(eraserX - eraserWidth / 2f, 0f),
                        size = androidx.compose.ui.geometry.Size(eraserWidth, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Draw wood handle top
                    drawRoundRect(
                        color = eraserWoodColor,
                        topLeft = androidx.compose.ui.geometry.Offset(eraserX - eraserWidth * 0.35f, h * 0.1f),
                        size = androidx.compose.ui.geometry.Size(eraserWidth * 0.7f, h * 0.8f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }
    ) {
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
                val iconKey = categoryIcon.takeIf { it.isNotEmpty() } ?: categoryName
                Icon(
                    imageVector = getIconForName(iconKey, isOutlined),
                    contentDescription = categoryName,
                    tint = ParchmentDarkBrown,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = categoryName,
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
                        text = "$date | $accountName",
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
                IconButton(onClick = { isDeleting = true }) {
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
}
