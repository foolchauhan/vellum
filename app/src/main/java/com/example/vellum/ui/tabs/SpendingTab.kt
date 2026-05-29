package com.example.vellum.ui.tabs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
                Text(
                    text = periodLabel,
                    style = fontStyle.copy(fontSize = 24.sp, fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
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

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Buttons
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
