package com.sismptm.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sismptm.client.ui.theme.*

/**
 * Screen displaying tour request confirmation and details.
 * Allows users to review their tour request, configure request options,
 * and proceed to view service details.
 *
 * @param onViewDetails Callback triggered when user confirms and views service details.
 * @param onBack Callback triggered when user navigates back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitudScreen(
    onViewDetails: () -> Unit,
    onBack: () -> Unit
) {
    var isDurationEnabled by remember { mutableStateOf(true) }
    var selectedWhen by remember { mutableStateOf("Now") }
    var specialInstructions by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Request Tour",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Background
                )
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            // Partner Summary Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Partner avatar",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Carlos Medina",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Bogotá, Colombia",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star",
                                tint = StarColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "4.9",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                // Available badge
                Box(
                    modifier = Modifier
                        .background(AvailableBadgeBg, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Available",
                        color = AvailableBadgeText,
                        fontSize = 11.sp
                    )
                }
            }

            // Tour Details
            Text(
                text = "Tour Details",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Destination
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Destination",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "Bogotá Historic Center",
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }

                    Divider(color = DividerBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                    // Duration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Duration",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = isDurationEnabled,
                                onCheckedChange = { isDurationEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TextPrimary,
                                    checkedTrackColor = PrimaryAccent,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = ToggleInactive
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "60 min",
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                        }
                    }

                    Divider(color = DividerBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                    // Tour type
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tour type",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "Historic City",
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }

                    Divider(color = DividerBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                    // When
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "When",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { selectedWhen = "Now" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedWhen == "Now") PrimaryAccent else ToggleInactive
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Now",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selectedWhen == "Now") TextPrimary else TextSecondary
                                )
                            }
                            Button(
                                onClick = { selectedWhen = "Schedule" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedWhen == "Schedule") PrimaryAccent else ToggleInactive
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Schedule",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selectedWhen == "Schedule") TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Special Instructions
            Text(
                text = "Special Instructions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            TextField(
                value = specialInstructions,
                onValueChange = { specialInstructions = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(100.dp),
                placeholder = {
                    Text(
                        text = "Any specific places you want to visit or instructions for your guide...",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                supportingText = {
                    Text(
                        text = "${specialInstructions.length} / 200",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.End)
                    )
                }
            )

            // Tour Summary
            Text(
                text = "Tour Summary",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Duration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Duration",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "60 minutes",
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }

                    Divider(color = DividerBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                    // Tour type
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tour type",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "Historic City",
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }

                    Divider(color = DividerBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                    // Partner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Partner",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "Carlos Medina",
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Button
            Button(
                onClick = onViewDetails,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                shape = RoundedCornerShape(28.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(
                    text = "Request Tour",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}