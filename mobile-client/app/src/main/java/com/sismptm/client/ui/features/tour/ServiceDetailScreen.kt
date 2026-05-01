package com.sismptm.client.ui.features.tour

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sismptm.client.ui.theme.*

/**
 * Screen displaying comprehensive details of a booked tour service.
 * Shows tour information including partner details, location, duration, pricing,
 * itinerary, and allows users to confirm or go back.
 *
 * @param onConfirm Callback triggered when user confirms the tour booking.
 * @param onBack Callback triggered when user navigates back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailScreen(
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1A1A1A),
                modifier = Modifier.height(56.dp)
            ) {
                val items = listOf("Explore", "Favorites", "Tours", "Messages", "Account")
                val icons = listOf(
                    Icons.Default.Home,
                    Icons.Default.Favorite,
                    Icons.Default.Star,
                    Icons.Default.MailOutline,
                    Icons.Default.Person
                )
                val selectedIndex = 0 // Explore is active
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = icons[index],
                                contentDescription = item,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item,
                                fontSize = 12.sp
                            )
                        },
                        selected = index == selectedIndex,
                        onClick = { /* Handle navigation */ },
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryAccent,
                            selectedTextColor = PrimaryAccent,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color.Gray), // Placeholder for cover photo
                contentAlignment = Alignment.TopCenter
            ) {
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Background)
                            )
                        )
                        .align(Alignment.BottomCenter)
                )
                // Title
                Text(
                    text = "Partner Profile",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 48.dp) // Assuming status bar space
                )
            }

            // Avatar Overlay
            Box(
                modifier = Modifier
                    .offset(y = (-40).dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.Gray), // Placeholder avatar
                    contentAlignment = Alignment.Center
                ) {
                    // Avatar content, e.g., Icon or Image
                }
                // Online dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Partner Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Carlos Medina",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bogotá, Colombia",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(5) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star",
                            tint = StarColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "4.9 (127 reviews)",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "38",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Tours done",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(DividerBorder)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "127",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Reviews",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(DividerBorder)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "2",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Min use",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Status Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Available  chip
                Row(
                    modifier = Modifier
                        .background(AvailableBadgeBg, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(AvailableBadgeText)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Available now",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = AvailableBadgeText
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Schedule chip
                Box(
                    modifier = Modifier
                        .background(ScheduleBadgeBg, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "○ Schedule for later",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ScheduleBadgeText
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // About Section
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "About",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Experienced guide specializing in historic city tours and cultural experiences in Bogotá. I offer immersive real-time tours through the most iconic neighborhoods and landmarks.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Read more",
                    fontSize = 14.sp,
                    color = PrimaryAccent
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBackground)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .height(72.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Starting from",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "~$15.000 COP / hour",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Request Tour",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
