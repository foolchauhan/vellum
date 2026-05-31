package com.example.vellum.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.drawText
import com.example.vellum.theme.*
import com.example.vellum.ui.components.ChalkboardBackground
import com.example.vellum.data.local.TransactionEntity
import java.util.*

data class PieSlice(val name: String, val amount: Double, val color: Color)
data class MonthData(val label: String, val income: Double, val expense: Double)

@Composable
fun LandscapeReports(
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val currentTransactions by viewModel.transactions.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val periodLabel by viewModel.periodLabel.collectAsState()
    val preferences by viewModel.preferences.collectAsState()

    var isPercentageMode by remember { mutableStateOf(false) }
    var reportType by remember { mutableStateOf("Categories") } // "Categories", "Cash Flow"
    var chartType by remember { mutableStateOf("Pie") } // "Pie", "Bar"

    var barChartPage by remember { mutableStateOf(0) }
    var cashFlowInterval by remember { mutableStateOf("Monthly") } // "Daily", "Weekly", "Monthly", "Yearly"

    LaunchedEffect(periodLabel, chartType, reportType) {
        barChartPage = 0
    }

    val textMeasurer = rememberTextMeasurer()

    // Filter and compute category sums for Expenses
    val expenses = currentTransactions.filter { it.type == "EXPENSE" }
    val categorySums = expenses.groupBy { it.categoryId }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .filter { it.value > 0 }

    val totalExpense = categorySums.values.sum()

    val slicesRaw = categorySums.map { (catId, amt) ->
        val cat = categories.find { it.id == catId }
        val name = cat?.name ?: "Unknown"
        val colorHex = cat?.chartColor ?: "#4E3C30"
        Triple(name, amt, colorHex)
    }.sortedByDescending { it.second }

    val slices = remember(slicesRaw) {
        val distinctPool = listOf(
            "#E91E63", "#FF9800", "#9C27B0", "#FFEB3B", 
            "#E040FB", "#00BCD4", "#4CAF50", "#E57373", 
            "#3F51B5", "#03A9F4", "#009688", "#9E9E9E",
            "#607D8B", "#FF5722", "#795548", "#CDDC39"
        )
        val usedColors = mutableSetOf<String>()
        val poolIterator = distinctPool.iterator()
        
        slicesRaw.map { (name, amt, colorHex) ->
            val upperColor = colorHex.uppercase()
            val finalColorHex = if (usedColors.contains(upperColor)) {
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
                nextColor
            } else {
                upperColor
            }
            usedColors.add(finalColorHex)
            
            val color = try {
                Color(android.graphics.Color.parseColor(finalColorHex))
            } catch (e: Exception) {
                ChalkGray
            }
            PieSlice(name = name, amount = amt, color = color)
        }
    }

    val maxPage = (slices.size - 1).coerceAtLeast(0) / 5
    val visibleSlices = slices.drop(barChartPage * 5).take(5)

    val currencySymbol = when (val sym = preferences["currency_symbol"]) {
        "Default" -> "₹"
        "INR" -> "₹"
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> sym ?: "₹"
    }

    ChalkboardBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Bar
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top-Left: Navigation buttons or Title
                Box(modifier = Modifier.width(160.dp), contentAlignment = Alignment.CenterStart) {
                    if (reportType == "Categories") {
                        if (chartType == "Bar") {
                            ChalkActionButtons(
                                onMinusClick = { if (barChartPage > 0) barChartPage-- },
                                onPlusClick = { if (barChartPage < maxPage) barChartPage++ }
                            )
                        } else {
                            ChalkActionButtons(
                                onMinusClick = { viewModel.navigatePeriod(false) },
                                onPlusClick = { viewModel.navigatePeriod(true) }
                            )
                        }
                    } else {
                        // Cash Flow: Interval Toggle
                        ChalkToggle(
                            options = listOf("D", "W", "M", "Y"),
                            selectedOption = when (cashFlowInterval) {
                                "Daily" -> "D"
                                "Weekly" -> "W"
                                "Monthly" -> "M"
                                "Yearly" -> "Y"
                                else -> "M"
                            },
                            onOptionSelected = {
                                cashFlowInterval = when (it) {
                                    "D" -> "Daily"
                                    "W" -> "Weekly"
                                    "M" -> "Monthly"
                                    "Y" -> "Yearly"
                                    else -> "Monthly"
                                }
                            }
                        )
                    }
                }

                // Top-Center: Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (reportType == "Categories") {
                        val legendSlices = if (chartType == "Bar") visibleSlices else slices.take(4)
                        legendSlices.forEach { slice ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(slice.color)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = slice.name,
                                    color = Color(0xFFBBBBBB),
                                    fontSize = 12.sp,
                                    fontFamily = ParchmentFontFamily
                                )
                            }
                        }
                    } else {
                        // Cash Flow Legend: Income green circle, Expense red diamond
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(ChalkGreen)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Income",
                                color = Color(0xFFBBBBBB),
                                fontSize = 12.sp,
                                fontFamily = ParchmentFontFamily
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .rotate(45f)
                                    .background(ChalkRed)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Expense",
                                color = Color(0xFFBBBBBB),
                                fontSize = 12.sp,
                                fontFamily = ParchmentFontFamily
                            )
                        }
                    }
                }

                // Top-Right: % vs Currency Toggle
                Box(modifier = Modifier.width(160.dp), contentAlignment = Alignment.CenterEnd) {
                    if (reportType == "Categories") {
                        ChalkToggle(
                            options = listOf("%", currencySymbol),
                            selectedOption = if (isPercentageMode) "%" else currencySymbol,
                            onOptionSelected = { isPercentageMode = (it == "%") }
                        )
                    } else {
                        Text(
                            text = when (cashFlowInterval) {
                                "Daily" -> "Last 12 Days"
                                "Weekly" -> "Last 12 Weeks"
                                "Monthly" -> "Last 12 Months"
                                "Yearly" -> "Last 12 Years"
                                else -> "Last 12 Months"
                            },
                            color = Color.White,
                            fontFamily = ParchmentFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 2. Middle Content Area (Chart + Rotated Period Label)
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Middle-Left: Rotated label (only for Categories)
                Box(
                    modifier = Modifier.width(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (reportType == "Categories") {
                        Text(
                            text = periodLabel,
                            color = Color(0xFFBBBBBB),
                            fontSize = 16.sp,
                            fontFamily = ParchmentFontFamily,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.rotate(-90f)
                        )
                    }
                }

                // Main Chart Display
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    if (reportType == "Categories") {
                        if (totalExpense <= 0) {
                            Text(
                                text = "No transactions for this period",
                                color = Color(0xFF888888),
                                fontFamily = ParchmentFontFamily,
                                fontSize = 16.sp
                            )
                        } else {
                            if (chartType == "Pie") {
                                PieChartCanvas(
                                    slices = slices,
                                    totalExpense = totalExpense,
                                    isPercentageMode = isPercentageMode,
                                    textMeasurer = textMeasurer,
                                    currencySymbol = currencySymbol,
                                    modifier = Modifier.size(220.dp)
                                )
                            } else {
                                BarChartCanvas(
                                    slices = visibleSlices,
                                    totalExpense = totalExpense,
                                    isPercentageMode = isPercentageMode,
                                    textMeasurer = textMeasurer,
                                    currencySymbol = currencySymbol,
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp)
                                )
                            }
                        }
                    } else {
                        // Cash Flow Chart
                        val cashFlowData = remember(allTransactions, cashFlowInterval) {
                            calculateCashFlowData(allTransactions, cashFlowInterval)
                        }
                        CashFlowLineChartCanvas(
                            data = cashFlowData,
                            textMeasurer = textMeasurer,
                            currencySymbol = currencySymbol,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp)
                        )
                    }
                }
            }

            // 3. Bottom Bar
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bottom-Left: Navigation arrows (Faded out when in Cash Flow mode)
                Row(
                    modifier = Modifier.width(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val enabled = reportType == "Categories"
                    Text(
                        text = "<",
                        color = if (enabled) Color.White else Color(0xFF555555),
                        fontFamily = ParchmentFontFamily,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable(enabled = enabled) { viewModel.navigatePeriod(false) }
                            .padding(8.dp)
                    )
                    Text(
                        text = ">",
                        color = if (enabled) Color.White else Color(0xFF555555),
                        fontFamily = ParchmentFontFamily,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable(enabled = enabled) { viewModel.navigatePeriod(true) }
                            .padding(8.dp)
                    )
                }

                // Bottom-Center: Categories vs Cash Flow Toggle
                ChalkToggle(
                    options = listOf("Categories", "Cash Flow"),
                    selectedOption = reportType,
                    onOptionSelected = { reportType = it }
                )

                // Bottom-Right: Pie vs Bar Toggle (Categories only)
                Box(modifier = Modifier.width(160.dp), contentAlignment = Alignment.CenterEnd) {
                    if (reportType == "Categories") {
                        ChalkToggle(
                            options = listOf("Pie", "Bar"),
                            selectedOption = chartType,
                            onOptionSelected = { chartType = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChalkToggle(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2D3231).copy(alpha = 0.5f))
            .border(1.dp, Color(0xFF666666), RoundedCornerShape(8.dp))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) Color(0xFF444C4A) else Color.Transparent)
                    .clickable { onOptionSelected(option) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = option,
                    color = if (isSelected) Color.White else Color(0xFFBBBBBB),
                    fontFamily = ParchmentFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ChalkActionButtons(
    onMinusClick: () -> Unit,
    onPlusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2D3231).copy(alpha = 0.5f))
            .border(1.dp, Color(0xFF666666), RoundedCornerShape(8.dp))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onMinusClick() }
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "-",
                color = Color.White,
                fontFamily = ParchmentFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onPlusClick() }
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "+",
                color = Color.White,
                fontFamily = ParchmentFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PieChartCanvas(
    slices: List<PieSlice>,
    totalExpense: Double,
    isPercentageMode: Boolean,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val fontFamily = ParchmentFontFamily
    Canvas(modifier = modifier) {
        var startAngle = -90f
        slices.forEach { slice ->
            val sweepAngle = ((slice.amount / totalExpense) * 360f).toFloat()
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true
            )
            drawArc(
                color = Color(0xFF1E2322),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                style = Stroke(width = 1.dp.toPx())
            )

            // Label positioning
            val angleRad = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
            val radius = size.width / 2
            val labelRadius = radius * 0.65f
            val labelX = center.x + labelRadius * Math.cos(angleRad).toFloat()
            val labelY = center.y + labelRadius * Math.sin(angleRad).toFloat()

            val labelText = if (isPercentageMode) {
                val pct = (slice.amount / totalExpense) * 100
                String.format(Locale.US, "%.0f%%", pct)
            } else {
                String.format(Locale.US, "%s%.0f", currencySymbol, slice.amount)
            }

            val textLayoutResult = textMeasurer.measure(
                text = labelText,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily
                )
            )

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x = labelX - textLayoutResult.size.width / 2,
                    y = labelY - textLayoutResult.size.height / 2
                )
            )

            startAngle += sweepAngle
        }
    }
}

@Composable
fun BarChartCanvas(
    slices: List<PieSlice>,
    totalExpense: Double,
    isPercentageMode: Boolean,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val fontFamily = ParchmentFontFamily
    Canvas(modifier = modifier) {
        val chartWidth = size.width
        val chartHeight = size.height - 70.dp.toPx()

        val maxAmount = slices.maxOfOrNull { it.amount } ?: 1.0
        val count = slices.size
        val barWidth = 36.dp.toPx()
        val spacing = 16.dp.toPx()
        val groupWidth = count * barWidth + (count - 1) * spacing
        val startX = (chartWidth - groupWidth) / 2

        // Draw baseline
        drawLine(
            color = Color.White.copy(alpha = 0.4f),
            start = Offset(0f, chartHeight),
            end = Offset(chartWidth, chartHeight),
            strokeWidth = 2f
        )

        slices.forEachIndexed { index, slice ->
            val barHeight = ((slice.amount / maxAmount) * chartHeight).toFloat()
            val x = startX + index * (barWidth + spacing)
            val y = chartHeight - barHeight

            drawRect(
                color = slice.color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )
            drawRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                style = Stroke(width = 1f)
            )

            val labelText = if (isPercentageMode) {
                val pct = (slice.amount / totalExpense) * 100
                String.format(Locale.US, "%.0f%%", pct)
            } else {
                String.format(Locale.US, "%s%.0f", currencySymbol, slice.amount)
            }

            val textLayoutResult = textMeasurer.measure(
                text = labelText,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily
                )
            )

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x = x + barWidth / 2 - textLayoutResult.size.width / 2,
                    y = y - textLayoutResult.size.height - 4.dp.toPx()
                )
            )

            val nameLayoutResult = textMeasurer.measure(
                text = slice.name,
                style = TextStyle(
                    color = Color(0xFFBBBBBB),
                    fontSize = 10.sp,
                    fontFamily = fontFamily
                )
            )

            val labelX = x + barWidth / 2
            val labelY = chartHeight + 8.dp.toPx()

            withTransform({
                rotate(degrees = 45f, pivot = Offset(labelX, labelY))
            }) {
                drawText(
                    textLayoutResult = nameLayoutResult,
                    topLeft = Offset(labelX, labelY)
                )
            }
        }
    }
}

@Composable
fun CashFlowLineChartCanvas(
    data: List<MonthData>,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val fontFamily = ParchmentFontFamily
    Canvas(modifier = modifier) {
        val labelPadding = 60.dp.toPx()
        val bottomPadding = 30.dp.toPx()

        val chartWidth = size.width - labelPadding
        val chartHeight = size.height - bottomPadding

        val startX = labelPadding
        val startY = 10.dp.toPx()
        val endX = size.width
        val endY = chartHeight

        val maxVal = data.flatMap { listOf(it.income, it.expense) }.maxOfOrNull { it } ?: 100.0
        val maxChartVal = when {
            maxVal <= 100.0 -> 100.0
            maxVal <= 500.0 -> 500.0
            maxVal <= 1000.0 -> 1000.0
            else -> {
                val rem = maxVal % 200
                if (rem == 0.0) maxVal else maxVal + (200 - rem)
            }
        }

        val stepCount = 5
        val valueStep = maxChartVal / stepCount
        val yStep = chartHeight / stepCount

        // Grid lines & Y labels
        for (i in 0..stepCount) {
            val y = endY - i * yStep
            drawLine(
                color = Color(0xFF333333),
                start = Offset(startX, y),
                end = Offset(endX, y),
                strokeWidth = 1f
            )

            val labelText = String.format(Locale.US, "%s%.0f", currencySymbol, i * valueStep)
            val textLayoutResult = textMeasurer.measure(
                text = labelText,
                style = TextStyle(
                    color = Color(0xFF888888),
                    fontSize = 10.sp,
                    fontFamily = fontFamily
                )
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x = startX - textLayoutResult.size.width - 8.dp.toPx(),
                    y = y - textLayoutResult.size.height / 2
                )
            )
        }

        // X labels
        val xStep = chartWidth / 11
        data.forEachIndexed { index, item ->
            val x = startX + index * xStep
            val textLayoutResult = textMeasurer.measure(
                text = item.label,
                style = TextStyle(
                    color = Color(0xFF888888),
                    fontSize = 10.sp,
                    fontFamily = fontFamily
                )
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x = x - textLayoutResult.size.width / 2,
                    y = endY + 8.dp.toPx()
                )
            )
        }

        // Points
        val incomePoints = data.mapIndexed { index, item ->
            val x = startX + index * xStep
            val y = endY - (item.income / maxChartVal).toFloat() * chartHeight
            Offset(x, y)
        }
        val expensePoints = data.mapIndexed { index, item ->
            val x = startX + index * xStep
            val y = endY - (item.expense / maxChartVal).toFloat() * chartHeight
            Offset(x, y)
        }

        // Lines
        for (i in 0 until incomePoints.size - 1) {
            drawLine(
                color = ChalkGreen,
                start = incomePoints[i],
                end = incomePoints[i+1],
                strokeWidth = 2.dp.toPx()
            )
        }
        for (i in 0 until expensePoints.size - 1) {
            drawLine(
                color = ChalkRed,
                start = expensePoints[i],
                end = expensePoints[i+1],
                strokeWidth = 2.dp.toPx()
            )
        }

        // Markers
        incomePoints.forEach { pt ->
            drawCircle(
                color = ChalkGreen,
                radius = 4.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = Color.White,
                radius = 1.5.dp.toPx(),
                center = pt
            )
        }
        expensePoints.forEach { pt ->
            val d = 4.dp.toPx()
            val path = Path().apply {
                moveTo(pt.x, pt.y - d)
                lineTo(pt.x + d, pt.y)
                lineTo(pt.x, pt.y + d)
                lineTo(pt.x - d, pt.y)
                close()
            }
            drawPath(path = path, color = ChalkRed)
        }
    }
}

private fun calculateCashFlowData(
    allTransactions: List<TransactionEntity>,
    interval: String
): List<MonthData> {
    val result = mutableListOf<MonthData>()
    
    for (i in 11 downTo 0) {
        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        var label = ""

        when (interval) {
            "Daily" -> {
                startCal.add(Calendar.DAY_OF_YEAR, -i)
                endCal.add(Calendar.DAY_OF_YEAR, -i)
                val day = startCal.get(Calendar.DAY_OF_MONTH)
                val monthStr = startCal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US) ?: ""
                label = "$day $monthStr"
            }
            "Weekly" -> {
                startCal.add(Calendar.WEEK_OF_YEAR, -i)
                startCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                
                endCal.timeInMillis = startCal.timeInMillis
                endCal.add(Calendar.DAY_OF_YEAR, 6)
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)

                val day = startCal.get(Calendar.DAY_OF_MONTH)
                val monthStr = startCal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US) ?: ""
                label = "$day $monthStr"
            }
            "Monthly" -> {
                startCal.add(Calendar.MONTH, -i)
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                
                endCal.timeInMillis = startCal.timeInMillis
                endCal.set(Calendar.DAY_OF_MONTH, startCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                
                label = startCal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US) ?: ""
            }
            "Yearly" -> {
                startCal.add(Calendar.YEAR, -i)
                startCal.set(Calendar.MONTH, Calendar.JANUARY)
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                
                endCal.timeInMillis = startCal.timeInMillis
                endCal.set(Calendar.MONTH, Calendar.DECEMBER)
                endCal.set(Calendar.DAY_OF_MONTH, 31)
                
                label = startCal.get(Calendar.YEAR).toString()
            }
        }

        val startMs = startCal.timeInMillis
        val endMs = endCal.timeInMillis

        val periodTx = allTransactions.filter { it.timestamp in startMs..endMs }
        val income = periodTx.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expense = periodTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        result.add(MonthData(label = label, income = income, expense = expense))
    }
    return result
}
