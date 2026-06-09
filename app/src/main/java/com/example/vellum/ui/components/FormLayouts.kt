package com.example.vellum.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vellum.theme.ParchmentBlueText
import com.example.vellum.theme.ParchmentDarkBrown
import com.example.vellum.theme.ParchmentFontFamily
import com.example.vellum.theme.ParchmentLine
import com.example.vellum.theme.SettingsSectionHeader

@Composable
fun FormSectionHeader(title: String, onHelpClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SettingsSectionHeader)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = ParchmentDarkBrown.copy(alpha = 0.8f),
            fontFamily = ParchmentFontFamily,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            fontSize = 14.sp
        )
        Icon(
            imageVector = Icons.Default.HelpOutline,
            contentDescription = "Help",
            tint = ParchmentDarkBrown.copy(alpha = 0.5f),
            modifier = Modifier
                .size(18.dp)
                .then(
                    if (onHelpClick != null) {
                        Modifier.clickable { onHelpClick() }
                    } else {
                        Modifier
                    }
                )
        )
    }
}

@Composable
fun FormRow(
    label: String,
    content: @Composable BoxScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .fillMaxHeight()
                .padding(start = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = label,
                color = ParchmentBlueText,
                fontFamily = ParchmentFontFamily,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                fontSize = 16.sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(0.5.dp)
                .background(ParchmentLine)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            content()
        }
    }
    HorizontalDivider(color = ParchmentLine, thickness = 0.5.dp)
}
