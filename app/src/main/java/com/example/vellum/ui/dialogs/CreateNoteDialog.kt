package com.example.vellum.ui.dialogs

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.vellum.theme.*
import com.example.vellum.ui.components.ParchmentBackground

@Composable
fun CreateNoteDialog(
    initialNote: String,
    onDismiss: () -> Unit,
    onSetNote: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialNote) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Create Note",
                        fontFamily = ParchmentFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = ParchmentDarkBrown,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
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
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "CANCEL",
                                color = ParchmentBlueText,
                                fontFamily = ParchmentFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = {
                            onSetNote(text)
                            onDismiss()
                        }) {
                            Text(
                                text = "SET",
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
