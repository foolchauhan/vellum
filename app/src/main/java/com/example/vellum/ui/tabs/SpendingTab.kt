package com.example.vellum.ui.tabs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vellum.theme.*
import com.example.vellum.ui.components.ChalkboardBackground
import com.example.vellum.ui.components.HorizontalDivider
import com.example.vellum.ui.main.MainScreenViewModel
import java.util.*

@Composable
fun SpendingTab(
    viewModel: MainScreenViewModel,
    onAddTransactionClicked: (String) -> Unit
) {
    val context = LocalContext.current
    val periodLabel by viewModel.periodLabel.collectAsState()
    val metrics by viewModel.spendingMetrics.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val activeAccount by viewModel.selectedFilterAccount.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()

    val currencySymbol = when (val sym = preferences["currency_symbol"]) {
        "Default" -> "₹"
        "INR" -> "₹"
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> sym ?: "₹"
    }

    val accountOptions = listOf(null) + accounts
    val currentIndex = accountOptions.indexOfFirst { it?.id == activeAccount?.id }
    val activeAccountLabel = activeAccount?.name ?: "All Accounts"

    val categorySpending = remember(transactions, categories) {
        val raw = transactions.filter { it.type == "EXPENSE" }
            .groupBy { it.categoryId }
            .map { (catId, txs) ->
                val catName = txs.firstOrNull()?.categoryName ?: categories.find { it.id == catId }?.name ?: "Unknown"
                val amount = txs.sumOf { it.amount }
                val colorHex = categories.find { it.id == catId }?.chartColor ?: "#4E3C30"
                Triple(catName, amount, colorHex)
            }
            .sortedByDescending { it.second }

        val distinctPool = listOf(
            "#E91E63", "#FF9800", "#9C27B0", "#FFEB3B", 
            "#E040FB", "#00BCD4", "#4CAF50", "#E57373", 
            "#3F51B5", "#03A9F4", "#009688", "#9E9E9E",
            "#607D8B", "#FF5722", "#795548", "#CDDC39"
        )
        val usedColors = mutableSetOf<String>()
        val result = mutableListOf<Triple<String, Double, String>>()
        val poolIterator = distinctPool.iterator()
        
        for (item in raw) {
            val (name, amount, colorHex) = item
            val upperColor = colorHex.uppercase()
            if (usedColors.contains(upperColor)) {
                var nextColor = upperColor
                while (poolIterator.hasNext()) {
                    val candidate = poolIterator.next().uppercase()
                    if (!usedColors.contains(candidate)) {
                        nextColor = candidate
                        break
                    }
                }
                if (nextColor == upperColor) {
                    do {
                        val r = (100..255).random()
                        val g = (100..255).random()
                        val b = (100..255).random()
                        nextColor = String.format(Locale.US, "#%02X%02X%02X", r, g, b)
                    } while (usedColors.contains(nextColor))
                }
                result.add(Triple(name, amount, nextColor))
                usedColors.add(nextColor)
            } else {
                result.add(item)
                usedColors.add(upperColor)
            }
        }
        result
    }

    val fontStyle = TextStyle(
        fontFamily = ParchmentFontFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.Normal,
        color = ParchmentDarkBrown
    )
    val titleStyle = TextStyle(
        fontFamily = ParchmentFontFamily,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = ParchmentDarkBrown
    )
    val labelStyle = TextStyle(
        fontFamily = ParchmentFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = ParchmentDarkBrown
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Centered period switching arrows
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "<",
                    style = fontStyle.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .clickable {
                            viewModel.navigatePeriod(false)
                        }
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = periodLabel,
                        style = fontStyle.copy(fontSize = 22.sp, fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = activeAccountLabel,
                        style = fontStyle.copy(fontSize = 14.sp, color = ParchmentBlueText, fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    text = ">",
                    style = fontStyle.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .clickable {
                            viewModel.navigatePeriod(true)
                        }
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable Middle Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Segmented progress bar
                val total = metrics.totalIncome + metrics.totalExpense
                val fraction = if (total > 0) (metrics.totalIncome / total).toFloat() else 0.5f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, ParchmentDarkBrown, RoundedCornerShape(8.dp))
                        .background(ChalkRed)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(ChalkGreen)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Data Rows (Income / Expense / Balance)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    // Income
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Income", style = fontStyle)
                        Text(text = String.format(Locale.US, "%s%.2f", currencySymbol, metrics.totalIncome), style = fontStyle.copy(color = ChalkGreen))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Expense
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Expense", style = fontStyle)
                        Text(text = String.format(Locale.US, "%s%.2f", currencySymbol, metrics.totalExpense), style = fontStyle.copy(color = ChalkRed))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dashed line
                    val lineCol = ParchmentLine
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                    ) {
                        drawLine(
                           color = lineCol,
                           start = Offset(0f, size.height / 2),
                           end = Offset(size.width, size.height / 2),
                           strokeWidth = 2f,
                           pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Balance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Balance", style = titleStyle)
                        Text(text = String.format(Locale.US, "%s%.2f", currencySymbol, metrics.balance), style = titleStyle.copy(color = ChalkBlue))
                    }
                }

                // Category wise spending breakdown
                if (categorySpending.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Spending by Category",
                        style = labelStyle.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    categorySpending.forEach { (catName, amount, colorHex) ->
                        val color = try {
                            Color(android.graphics.Color.parseColor(colorHex))
                        } catch (e: Exception) {
                            ParchmentDarkBrown
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = catName,
                                    style = fontStyle.copy(fontSize = 16.sp)
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "%s%.2f", currencySymbol, amount),
                                style = fontStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                        HorizontalDivider(
                            color = ParchmentLine.copy(alpha = 0.5f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Buttons (Sticky)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // + Expense Button
                Button(
                    onClick = { onAddTransactionClicked("EXPENSE") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = ParchmentDarkBrown),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, ParchmentDarkBrown, RoundedCornerShape(8.dp)),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(text = "+ Expense", style = labelStyle)
                }

                // + Income Button
                Button(
                    onClick = { onAddTransactionClicked("INCOME") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = ParchmentDarkBrown),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, ParchmentDarkBrown, RoundedCornerShape(8.dp)),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(text = "+ Income", style = labelStyle)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Help text
            Text(
                text = "** Rotate device to view reports **",
                style = labelStyle.copy(color = ParchmentDarkBrown.copy(alpha = 0.5f), fontSize = 12.sp),
                textAlign = TextAlign.Center
            )
        }
    }
}
