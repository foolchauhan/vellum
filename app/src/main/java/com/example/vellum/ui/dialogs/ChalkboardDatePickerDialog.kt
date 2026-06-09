package com.example.vellum.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.vellum.theme.*
import com.example.vellum.ui.components.ParchmentBackground
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChalkboardDatePickerDialog(
    initialTimestamp: Long,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val selectedCalendar = remember { Calendar.getInstance().apply { timeInMillis = initialTimestamp } }
    var currentMonthCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply { timeInMillis = initialTimestamp })
    }

    val monthHeader = remember(currentMonthCalendar) {
        SimpleDateFormat("MMMM yyyy", Locale.US).format(currentMonthCalendar.time)
    }

    val daysInMonth = currentMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = remember(currentMonthCalendar) {
        (currentMonthCalendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }.get(Calendar.DAY_OF_WEEK)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ParchmentBackground),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, ParchmentLine, RoundedCornerShape(12.dp))
        ) {
            ParchmentBackground(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select Date",
                        fontFamily = ParchmentFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ParchmentDarkBrown,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            currentMonthCalendar = (currentMonthCalendar.clone() as Calendar).apply {
                                add(Calendar.MONTH, -1)
                            }
                        }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Prev Month", tint = ParchmentDarkBrown)
                        }

                        Text(
                            text = monthHeader,
                            fontFamily = ParchmentFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = ParchmentDarkBrown
                        )

                        IconButton(onClick = {
                            currentMonthCalendar = (currentMonthCalendar.clone() as Calendar).apply {
                                add(Calendar.MONTH, 1)
                            }
                        }) {
                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Next Month", tint = ParchmentDarkBrown)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weekdays.forEach { day ->
                            Text(
                                text = day,
                                fontFamily = ParchmentFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = ParchmentDarkBrown.copy(alpha = 0.6f),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val totalCells = (firstDayOfWeek - 1) + daysInMonth
                    val rowsCount = (totalCells + 6) / 7

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (r in 0 until rowsCount) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (c in 0..6) {
                                    val cellIndex = r * 7 + c
                                    val dayNum = cellIndex - (firstDayOfWeek - 2)
                                    if (dayNum in 1..daysInMonth) {
                                        val isSelected = selectedCalendar.get(Calendar.YEAR) == currentMonthCalendar.get(Calendar.YEAR) &&
                                                selectedCalendar.get(Calendar.MONTH) == currentMonthCalendar.get(Calendar.MONTH) &&
                                                selectedCalendar.get(Calendar.DAY_OF_MONTH) == dayNum
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isSelected) ParchmentDarkBrown else Color.Transparent)
                                                .clickable {
                                                    val resultCal = (currentMonthCalendar.clone() as Calendar).apply {
                                                        set(Calendar.DAY_OF_MONTH, dayNum)
                                                    }
                                                    onDateSelected(resultCal.timeInMillis)
                                                }
                                                .padding(2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayNum.toString(),
                                                fontFamily = ParchmentFontFamily,
                                                color = if (isSelected) ParchmentBackground else ParchmentDarkBrown,
                                                fontSize = 14.sp
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "CANCEL",
                                color = ParchmentDarkBrown,
                                fontFamily = ParchmentFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
