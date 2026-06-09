package com.example.vellum.ui.tabs

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vellum.data.local.StickyNoteEntity
import com.example.vellum.theme.*
import com.example.vellum.ui.main.MainScreenViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StickyNotesTab(
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.stickyNotes.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val context = LocalContext.current
    
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<StickyNoteEntity?>(null) }
    
    // Available pastel colors for sticky notes
    val colorsList = listOf(
        "#FFF9C4", // Pastel Yellow
        "#F8BBD0", // Pastel Pink
        "#C8E6C9", // Pastel Green
        "#B3E5FC", // Pastel Blue
        "#D1C4E9", // Pastel Purple
        "#FFE0B2"  // Pastel Orange
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Parchment Board",
                fontFamily = ParchmentFontFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = ParchmentDarkBrown,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.dp, ParchmentLine.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "This board is empty. Tap the '+' button below to pin your first sticky note!",
                        color = ParchmentDarkBrown.copy(alpha = 0.6f),
                        fontFamily = ParchmentFontFamily,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        // Generate a pseudo-random rotation angle based on note ID hash to stay consistent
                        val rotationAngle = remember(note.id) {
                            val hash = note.id.hashCode()
                            val degrees = (hash % 6) - 3f // Between -3f and +3f
                            degrees
                        }
                        
                        val noteColor = remember(note.colorHex) {
                            try {
                                Color(android.graphics.Color.parseColor(note.colorHex))
                            } catch (e: Exception) {
                                Color(0xFFFFF9C4)
                            }
                        }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    rotationZ = rotationAngle
                                }
                                .combinedClickable(
                                    onClick = {
                                        noteToEdit = note
                                        showAddNoteDialog = true
                                    },
                                    onLongClick = {
                                        viewModel.deleteStickyNote(note)
                                        Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                .border(1.dp, ParchmentLine.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                            colors = CardDefaults.cardColors(containerColor = noteColor),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Pinned dot/pin visual at the top center
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(Color.Red.copy(alpha = 0.7f))
                                            .align(Alignment.CenterHorizontally)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = note.content,
                                        fontFamily = ParchmentFontFamily,
                                        fontSize = 15.sp,
                                        color = Color(0xFF3E2723), // Dark brown ink
                                        minLines = 4,
                                        maxLines = 8
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val dateStr = remember(note.createdAt) {
                                            SimpleDateFormat("dd MMM", Locale.US).format(Date(note.createdAt))
                                        }
                                        Text(
                                            text = dateStr,
                                            fontFamily = ParchmentFontFamily,
                                            fontSize = 10.sp,
                                            color = Color(0xFF3E2723).copy(alpha = 0.5f)
                                        )
                                        
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color(0xFF3E2723).copy(alpha = 0.4f),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    viewModel.deleteStickyNote(note)
                                                    Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Floating action button to add note
        FloatingActionButton(
            onClick = {
                noteToEdit = null
                showAddNoteDialog = true
            },
            containerColor = ParchmentDarkBrown,
            contentColor = ParchmentBackground,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Sticky Note"
            )
        }
    }
    
    if (showAddNoteDialog) {
        var contentText by remember { mutableStateOf(noteToEdit?.content ?: "") }
        var selectedColorHex by remember { mutableStateOf(noteToEdit?.colorHex ?: colorsList.first()) }
        
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = {
                Text(
                    text = if (noteToEdit != null) "Edit Sticky Note" else "Add Sticky Note",
                    fontFamily = ParchmentFontFamily,
                    color = ParchmentDarkBrown,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = contentText,
                        onValueChange = { contentText = it },
                        placeholder = { Text("Write your thoughts...", fontFamily = ParchmentFontFamily) },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = ParchmentFontFamily,
                            fontSize = 16.sp,
                            color = ParchmentDarkBrown
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ParchmentDarkBrown,
                            unfocusedBorderColor = ParchmentLine
                        )
                    )
                    
                    Text(
                        text = "Choose Paper Color:",
                        fontFamily = ParchmentFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ParchmentDarkBrown
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorsList.forEach { colorHex ->
                            val c = Color(android.graphics.Color.parseColor(colorHex))
                            val isSelected = selectedColorHex == colorHex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(c)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.5.dp,
                                        color = if (isSelected) ParchmentDarkBrown else ParchmentLine,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { selectedColorHex = colorHex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (contentText.trim().isEmpty()) {
                            Toast.makeText(context, "Note cannot be empty", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addStickyNote(
                            content = contentText.trim(),
                            colorHex = selectedColorHex,
                            id = noteToEdit?.id
                        )
                        showAddNoteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ParchmentDarkBrown)
                ) {
                    Text(
                        text = "Save",
                        fontFamily = ParchmentFontFamily,
                        color = MaterialTheme.colorScheme.background
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) {
                    Text(
                        text = "Cancel",
                        fontFamily = ParchmentFontFamily,
                        color = ParchmentDarkBrown
                    )
                }
            },
            containerColor = ParchmentBackground
        )
    }
}
