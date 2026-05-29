package com.example.vellum.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.vellum.theme.*
import com.example.vellum.ui.components.ParchmentBackground
import com.example.vellum.ui.main.MainScreenViewModel

@Composable
fun JoinSharedAccountDialog(
    viewModel: MainScreenViewModel,
    onDismiss: () -> Unit
) {
    var shareCode by remember { mutableStateOf("") }
    var isJoining by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ChalkboardSlate),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, ParchmentLine, RoundedCornerShape(12.dp))
        ) {
            ParchmentBackground(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Join Shared Account",
                        fontFamily = ParchmentFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ParchmentDarkBrown,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Enter the 6-character sharing code of the household account.",
                        fontFamily = ParchmentFontFamily,
                        fontSize = 12.sp,
                        color = ParchmentDarkBrown.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    TextField(
                        value = shareCode,
                        onValueChange = { shareCode = it.uppercase().take(6) },
                        singleLine = true,
                        placeholder = { Text("e.g. FAM392", color = Color.Gray, fontFamily = ParchmentFontFamily) },
                        textStyle = TextStyle(
                            color = ParchmentDarkBrown,
                            fontFamily = ParchmentFontFamily,
                            fontSize = 16.sp
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = ParchmentDarkBrown,
                            unfocusedIndicatorColor = ParchmentLine
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            enabled = !isJoining
                        ) {
                            Text(
                                text = "CANCEL",
                                color = ParchmentBlueText,
                                fontFamily = ParchmentFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                if (shareCode.length < 6) {
                                    Toast.makeText(context, "Code must be 6 characters", Toast.LENGTH_SHORT).show()
                                    return@TextButton
                                }
                                isJoining = true
                                viewModel.joinSharedAccount(shareCode) { success ->
                                    isJoining = false
                                    if (success) {
                                        Toast.makeText(context, "Successfully joined shared account!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "Failed to join. Invalid code or network error.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !isJoining
                        ) {
                            Text(
                                text = if (isJoining) "JOINING..." else "JOIN",
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
}
