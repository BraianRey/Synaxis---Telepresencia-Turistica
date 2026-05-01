package com.sismptm.client.ui.features.tour

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sismptm.client.data.remote.api.dto.ServiceResponse
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
    var meetingPointText by remember { mutableStateOf("") }

    val canSubmit = selectedArea != null && !isLoading

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
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create service request",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Tell us what service you need",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Create a tour request and available partners in the selected area will be able to accept it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    ExposedDropdownMenuBox(
                        expanded = areaExpanded,
                        onExpandedChange = { areaExpanded = !areaExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedArea?.label.orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Destination area") },
                            placeholder = { Text("Select a city") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = areaExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = requestFieldColors
                        )
                        DropdownMenu(
                            expanded = areaExpanded,
                            onDismissRequest = { areaExpanded = false }
                        ) {
                            requestAreaOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        selectedArea = option
                                        areaExpanded = false
                                    }
                                )
                            }
                        }
                    }


                    OutlinedTextField(
                        value = meetingPointText,
                        onValueChange = { meetingPointText = it.take(255) },
                        label = { Text("Meeting point / notes") },
                        placeholder = { Text("Optional: Historic center, plaza, museum entrance...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = requestFieldColors
                    )
                }
            }

            if (errorState != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = errorState.message,
                        color = Color(0xFFC62828),
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            RequestSummaryCard(
                areaName = selectedArea?.label ?: "Not selected",
                meetingPoint = meetingPointText.ifBlank { "No additional notes" }
            )

            Button(
                onClick = {
                    viewModel.requestTour(
                        longitude = selectedArea!!.longitude,
                        latitude = selectedArea!!.latitude,
                        locationDescription = meetingPointText
                    )
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                shape = RoundedCornerShape(28.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Create service",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestSummaryCard(
    areaName: String,
    meetingPoint: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Request summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            SummaryRow(label = "Area", value = areaName)
            SummaryRow(label = "Notes", value = meetingPoint)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun RequestCreatedDialog(
    service: ServiceResponse,
    areaName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Service created successfully") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Your request is now visible to available partners.")
                Text("Service ID: ${service.serviceId}")
                Text("Area: $areaName")
                Text("Status: ${service.status}")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Stay here")
            }
        }
    )
}

@Composable
private fun ActiveServiceDialog(
    service: ServiceResponse,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("You already have an active request") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(message)
                Text("Service ID: ${service.serviceId}")
                Text("Status: ${service.status}")
                Text("Open the waiting screen to track or cancel this request.")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Go to waiting screen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Stay here")
            }
        }
    )
}

