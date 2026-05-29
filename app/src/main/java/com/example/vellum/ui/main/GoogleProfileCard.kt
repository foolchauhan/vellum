package com.example.vellum.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vellum.MainActivity
import com.example.vellum.theme.*

@Composable
fun GoogleProfileCard(viewModel: MainScreenViewModel) {
    val userEmail by viewModel.userEmail.collectAsState()
    val userDisplayName by viewModel.userDisplayName.collectAsState()
    val userPhotoUrl by viewModel.userPhotoUrl.collectAsState()
    val context = LocalContext.current
    val activity = context as? MainActivity
 
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, ParchmentLine, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SettingsSectionHeader)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (userEmail != null) {
                val initials = remember(userEmail, userDisplayName) {
                    val name = userDisplayName ?: userEmail ?: ""
                    if (name.isNotEmpty()) name.take(1).uppercase() else "?"
                }
                if (userPhotoUrl != null) {
                    coil.compose.AsyncImage(
                        model = userPhotoUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .border(1.dp, ParchmentLine, CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(ParchmentDarkBrown)
                            .border(1.dp, ParchmentLine, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = ParchmentBackground,
                            fontFamily = ParchmentFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = userDisplayName ?: "Google User",
                    color = ParchmentDarkBrown,
                    fontFamily = ParchmentFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Text(
                    text = userEmail ?: "",
                    color = ParchmentDarkBrown.copy(alpha = 0.7f),
                    fontFamily = ParchmentFontFamily,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        activity?.triggerGoogleSignOut {
                            viewModel.signOut()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .border(1.dp, ChalkRed, RoundedCornerShape(8.dp)),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Sign Out",
                        color = ChalkRed,
                        fontFamily = ParchmentFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            } else {
                Text(
                    text = "Google Account Sync",
                    color = ParchmentDarkBrown,
                    fontFamily = ParchmentFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Sign in to backup settings, transactions, and share accounts with household members.",
                    color = ParchmentDarkBrown.copy(alpha = 0.7f),
                    fontFamily = ParchmentFontFamily,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        activity?.triggerGoogleSignIn { email: String, displayName: String?, photoUrl: String? ->
                            viewModel.signIn(email, displayName, photoUrl)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .border(1.dp, ParchmentBlueText, RoundedCornerShape(8.dp)),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Sign In with Google",
                        color = ParchmentBlueText,
                        fontFamily = ParchmentFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
