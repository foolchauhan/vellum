package com.example.vellum.ui.dialogs

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.vellum.data.local.TransactionEntity
import com.example.vellum.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ConflictResolutionDialog(
    localTx: TransactionEntity,
    remoteTx: TransactionEntity,
    currencySymbol: String = "₹",
    onResolve: (useLocal: Boolean) -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val localDateStr = sdf.format(Date(localTx.timestamp))
    val remoteDateStr = sdf.format(Date(remoteTx.timestamp))

    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ParchmentBackground),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .border(1.dp, ParchmentLine, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Sync Conflict Detected",
                    fontFamily = ParchmentFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = ParchmentDarkBrown,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "This transaction has changes in the cloud. Choose which version to keep.",
                    fontFamily = ParchmentFontFamily,
                    fontSize = 13.sp,
                    color = ParchmentDarkBrown.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Local Card
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = ParchmentBackground),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, ParchmentLine, RoundedCornerShape(8.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "LOCAL DEVICE",
                                fontFamily = ParchmentFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = ParchmentBlueText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            ConflictField(label = "Amount", value = String.format("%s%.2f", currencySymbol, localTx.amount))
                            ConflictField(label = "Type", value = localTx.type)
                            ConflictField(label = "Category", value = localTx.categoryName)
                            ConflictField(label = "Account", value = localTx.accountName)
                            ConflictField(label = "Date", value = localDateStr)
                            ConflictField(label = "Note", value = localTx.note.ifEmpty { "(None)" })
                        }
                    }

                    // Remote Card
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = ParchmentBackground),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, ParchmentLine, RoundedCornerShape(8.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "CLOUD VERSION",
                                fontFamily = ParchmentFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = ChalkRed,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            ConflictField(label = "Amount", value = String.format("%s%.2f", currencySymbol, remoteTx.amount))
                            ConflictField(label = "Type", value = remoteTx.type)
                            ConflictField(label = "Category", value = remoteTx.categoryName)
                            ConflictField(label = "Account", value = remoteTx.accountName)
                            ConflictField(label = "Date", value = remoteDateStr)
                            ConflictField(label = "Note", value = remoteTx.note.ifEmpty { "(None)" })
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { onResolve(false) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "KEEP CLOUD",
                            color = ChalkRed,
                            fontFamily = ParchmentFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    TextButton(
                        onClick = { onResolve(true) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "KEEP LOCAL",
                            color = ParchmentBlueText,
                            fontFamily = ParchmentFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConflictField(label: String, value: String) {
    Column {
        Text(
            text = label.uppercase(),
            fontFamily = ParchmentFontFamily,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = ParchmentDarkBrown.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            fontFamily = ParchmentFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = ParchmentDarkBrown
        )
    }
}
