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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vellum.theme.*
import com.example.vellum.ui.components.ChalkboardBackground
import com.example.vellum.ui.components.HorizontalDivider
import com.example.vellum.ui.main.MainScreenViewModel
import java.util.*
import kotlinx.serialization.json.Json
import com.example.vellum.data.local.TransactionSplit
import com.example.vellum.data.local.CategoryEntity

@Composable
fun SpendingTab(
    viewModel: MainScreenViewModel,
    onAddTransactionClicked: (String) -> Unit,
    onCategoryClicked: (CategoryEntity) -> Unit
) {
    val context = LocalContext.current
    val periodLabel by viewModel.periodLabel.collectAsState()
    val metrics by viewModel.spendingMetrics.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val activeAccount by viewModel.selectedFilterAccount.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val timePeriodPref by viewModel.timePeriod.collectAsState()

    val isNavigable = timePeriodPref != "All" && timePeriodPref != "Custom"

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
        val contributions = mutableListOf<Pair<String, Double>>()
        transactions.filter { it.type == "EXPENSE" }.forEach { tx ->
            if (tx.splits.isNotEmpty()) {
                try {
                    val splits = Json.decodeFromString<List<TransactionSplit>>(tx.splits)
                    splits.forEach { split ->
                        contributions.add(Pair(split.categoryId, split.amount))
                    }
                } catch (e: Exception) {
                    contributions.add(Pair(tx.categoryId, tx.amount))
                }
            } else {
                contributions.add(Pair(tx.categoryId, tx.amount))
            }
        }
        val raw = contributions.groupBy { it.first }
            .map { (catId, items) ->
                val amount = items.sumOf { it.second }
                val category = categories.find { it.id == catId }
                val catName = category?.name ?: "Unknown"
                val colorHex = category?.chartColor ?: "#4E3C30"
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
                    style = fontStyle.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNavigable) ParchmentDarkBrown else Color.Transparent
                    ),
                    modifier = Modifier
                        .then(
                            if (isNavigable) {
                                Modifier.clickable { viewModel.navigatePeriod(false) }
                            } else Modifier
                        )
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
                    style = fontStyle.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNavigable) ParchmentDarkBrown else Color.Transparent
                    ),
                    modifier = Modifier
                        .then(
                            if (isNavigable) {
                                Modifier.clickable { viewModel.navigatePeriod(true) }
                            } else Modifier
                        )
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
                        ChalkAnimatedAmount(
                            targetValue = metrics.totalIncome,
                            currencySymbol = currencySymbol,
                            style = fontStyle.copy(color = ChalkGreen)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Expense
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Expense", style = fontStyle)
                        ChalkAnimatedAmount(
                            targetValue = metrics.totalExpense,
                            currencySymbol = currencySymbol,
                            style = fontStyle.copy(color = ChalkRed)
                        )
                    }

                    // Carry Over starting balance row (if enabled for the account)
                    val showCarryOver = activeAccount?.carryOver == true || (activeAccount == null && accounts.any { it.carryOver })
                    val priorBalance = metrics.balance - (metrics.totalIncome - metrics.totalExpense)
                    if (showCarryOver && priorBalance != 0.0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Carry Over", style = fontStyle)
                            ChalkAnimatedAmount(
                                targetValue = priorBalance,
                                currencySymbol = currencySymbol,
                                style = fontStyle.copy(color = if (priorBalance >= 0) ChalkGreen else ChalkRed)
                            )
                        }
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
                        ChalkAnimatedAmount(
                            targetValue = metrics.balance,
                            currencySymbol = currencySymbol,
                            style = titleStyle.copy(color = ChalkBlue)
                        )
                    }
                }

                val showFinancialTutor = preferences["financial_tutor"] ?: "On"
                if (showFinancialTutor == "On") {
                    val aiInsights by viewModel.aiInsights.collectAsState()
                    if (aiInsights.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { aiInsights.size })
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDarkTheme) Color(0xFF2C3231) else Color(0xFFF2EAD8))
                                .border(1.dp, ParchmentLine, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                androidx.compose.foundation.pager.HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxWidth().wrapContentHeight()
                                ) { page ->
                                    val insight = aiInsights.getOrNull(page) ?: ""
                                    var isExpanded by remember(page) { mutableStateOf(false) }
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                                    ) {
                                        val density = androidx.compose.ui.platform.LocalDensity.current
                                        val owlSizePx = 56f * density.density
                                        val drawColor = ParchmentDarkBrown
                                        Canvas(modifier = Modifier.size(36.dp)) {
                                            val scaleFactor = size.width / owlSizePx
                                            withTransform({
                                                scale(scaleFactor, scaleFactor, pivot = Offset(0f, 0f))
                                            }) {
                                                drawCircle(
                                                    color = drawColor,
                                                    radius = 22f,
                                                    center = Offset(28f, 28f),
                                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                                                )
                                                drawCircle(
                                                    color = drawColor,
                                                    radius = 5f,
                                                    center = Offset(19f, 24f),
                                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                                                )
                                                drawCircle(
                                                    color = drawColor,
                                                    radius = 5f,
                                                    center = Offset(37f, 24f),
                                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                                                )
                                                drawCircle(
                                                    color = drawColor,
                                                    radius = 2.2f,
                                                    center = Offset(19f, 24f)
                                                )
                                                drawCircle(
                                                    color = drawColor,
                                                    radius = 2.2f,
                                                    center = Offset(37f, 24f)
                                                )
                                                val beakPath = androidx.compose.ui.graphics.Path().apply {
                                                    moveTo(28f, 27f)
                                                    lineTo(25f, 32f)
                                                    lineTo(31f, 32f)
                                                    close()
                                                }
                                                drawPath(path = beakPath, color = drawColor)
                                                val capPath = androidx.compose.ui.graphics.Path().apply {
                                                    moveTo(28f, 2f)
                                                    lineTo(14f, 8f)
                                                    lineTo(28f, 14f)
                                                    lineTo(42f, 8f)
                                                    close()
                                                }
                                                drawPath(path = capPath, color = drawColor)
                                                drawLine(color = drawColor, start = Offset(42f, 8f), end = Offset(45f, 18f), strokeWidth = 1.5f)
                                            }
                                        }

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { isExpanded = !isExpanded }
                                        ) {
                                            Text(
                                                text = "Financial Tutor (${page + 1}/${aiInsights.size})",
                                                fontFamily = ParchmentFontFamily,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ParchmentBlueText
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = insight,
                                                fontFamily = ParchmentFontFamily,
                                                fontSize = 13.sp,
                                                color = ParchmentDarkBrown,
                                                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                                                overflow = if (isExpanded) androidx.compose.ui.text.style.TextOverflow.Clip else androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                
                                // Dot indicators for pager
                                if (aiInsights.size > 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        repeat(aiInsights.size) { iteration ->
                                            val color = if (pagerState.currentPage == iteration) ParchmentBlueText else ParchmentLine
                                            Box(
                                                modifier = Modifier
                                                    .padding(2.dp)
                                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                                    .background(color)
                                                    .size(6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
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
                        val categoryObj = categories.find { it.name == catName }
                        val budget = categoryObj?.budget ?: 0.0

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    categoryObj?.let { onCategoryClicked(it) }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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

                            if (budget > 0.0) {
                                val remaining = budget - amount
                                val fraction = if (remaining > 0) (remaining / budget).toFloat() else 0f
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "Remaining: %s%.2f (%.0f%%)", currencySymbol, if (remaining > 0) remaining else 0.0, fraction * 100),
                                        style = fontStyle.copy(fontSize = 12.sp, color = ParchmentDarkBrown.copy(alpha = 0.6f))
                                    )
                                    Text(
                                        text = String.format(Locale.US, "Budget: %s%.2f", currencySymbol, budget),
                                        style = fontStyle.copy(fontSize = 12.sp, color = ParchmentDarkBrown.copy(alpha = 0.6f))
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                ) {
                                    val w = size.width
                                    val h = size.height
                                    // Faint dashed line for budget path
                                    drawLine(
                                        color = color.copy(alpha = 0.2f),
                                        start = Offset(0f, h / 2f),
                                        end = Offset(w, h / 2f),
                                        strokeWidth = 4f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                    )
                                    // Stronger wavy/dashed line for remaining budget path
                                    if (fraction > 0f) {
                                        drawLine(
                                            color = color,
                                            start = Offset(0f, h / 2f),
                                            end = Offset(w * fraction, h / 2f),
                                            strokeWidth = 6f,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                                        )
                                    }
                                }
                            }
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

@Composable
fun ChalkAnimatedAmount(
    targetValue: Double,
    currencySymbol: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    prefix: String = ""
) {
    val animatedValue by androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetValue.toFloat(),
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "ChalkAmount"
    )
    Text(
        text = String.format(Locale.US, "%s%s%.2f", prefix, currencySymbol, animatedValue),
        style = style,
        modifier = modifier
    )
}
