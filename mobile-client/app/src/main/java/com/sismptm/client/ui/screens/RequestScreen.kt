package com.sismptm.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sismptm.client.data.remote.ServiceResponse
import com.sismptm.client.ui.theme.Background
import com.sismptm.client.ui.theme.CardBackground
import com.sismptm.client.ui.theme.DividerBorder
import com.sismptm.client.ui.theme.PrimaryAccent
import com.sismptm.client.ui.theme.TextPrimary
import com.sismptm.client.ui.theme.TextSecondary

private data class RequestAreaOption(
    val label: String,
    val longitude: Double,
    val latitude: Double
)

private val requestAreaOptions = listOf(
    RequestAreaOption("Popayan", -76.6134, 2.4382),
    RequestAreaOption("Cali", -76.5320, 3.4516),
    RequestAreaOption("Medellin", -75.5636, 6.2518),
    RequestAreaOption("Bogota", -74.0721, 4.7110)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestScreen(
    onViewDetails: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: RequestTourViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val successState = uiState as? RequestTourViewModel.RequestUiState.Success
    val activeServiceState = uiState as? RequestTourViewModel.RequestUiState.ActiveService
    val errorState = uiState as? RequestTourViewModel.RequestUiState.Error
    val isLoading = uiState is RequestTourViewModel.RequestUiState.Loading

    // Track whether we already navigated away so we don't re-trigger on recompose.
    var hasNavigatedToWaiting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Only check once per screen entry; skip if we already handled it.
        if (!hasNavigatedToWaiting) {
            viewModel.checkActiveServiceBeforeCreate()
        }
    }

    LaunchedEffect(activeServiceState?.service?.serviceId) {
        val activeServiceId = activeServiceState?.service?.serviceId ?: return@LaunchedEffect
        if (!hasNavigatedToWaiting) {
            hasNavigatedToWaiting = true
            onViewDetails(activeServiceId)
            viewModel.resetState()
        }
    }

    var areaExpanded by remember { mutableStateOf(false) }
    var selectedArea by remember { mutableStateOf<RequestAreaOption?>(null) }
    var agreedHoursText by remember { mutableStateOf("1") }
    var hourlyRateText by remember { mutableStateOf("") }
    var meetingPointText by remember { mutableStateOf("") }

    val agreedHours = agreedHoursText.toIntOrNull()
    val hourlyRate = hourlyRateText.toDoubleOrNull()
    val canSubmit = selectedArea != null &&
        agreedHours != null && agreedHours > 0 &&
        hourlyRate != null && hourlyRate > 0.0 &&
        !isLoading

    val requestFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedLabelColor = TextPrimary,
        unfocusedLabelColor = TextSecondary,
        focusedPlaceholderColor = TextSecondary,
        unfocusedPlaceholderColor = TextSecondary,
        cursorColor = PrimaryAccent,
        focusedBorderColor = PrimaryAccent,
        unfocusedBorderColor = DividerBorder
    )

    if (successState != null) {
        RequestCreatedDialog(
            service = successState.service,
            areaName = successState.service.startLocationDescription ?: "Location not specified",
            onDismiss = { viewModel.resetState() },
            onConfirm = {
                viewModel.resetState()
                onViewDetails(successState.service.serviceId)
            }
        )
    }

    if (activeServiceState != null) {
        ActiveServiceDialog(
            service = activeServiceState.service,
            message = activeServiceState.message,
            onDismiss = { viewModel.resetState() },
            onConfirm = {
                viewModel.resetState()
                onViewDetails(activeServiceState.service.serviceId)
            }
        )
    }

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
                onClick = {
                    viewModel.requestTour(
                        longitude = selectedArea!!.longitude,
                        latitude = selectedArea!!.latitude,
                        agreedHours = agreedHours!!,
                        hourlyRate = hourlyRate!!,
                        locationDescription = meetingPointText
                    )
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
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
