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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sismptm.client.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sismptm.client.data.remote.api.dto.ServiceResponse
import com.sismptm.client.ui.theme.Background
import com.sismptm.client.ui.theme.CardBackground
import com.sismptm.client.ui.theme.DividerBorder
import com.sismptm.client.ui.theme.ErrorDark
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

    // Dialogs
    RequestDialogs(
        successState = successState,
        activeServiceState = activeServiceState,
        onDismissSuccess = { viewModel.resetState() },
        onConfirmSuccess = {
            viewModel.resetState()
            onViewDetails(successState!!.service.serviceId)
        },
        onDismissActive = { viewModel.resetState() },
        onConfirmActive = {
            viewModel.resetState()
            onViewDetails(activeServiceState!!.service.serviceId)
        }
    )

    Scaffold(
        containerColor = Background,
        topBar = { RequestTopBar(onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RequestFormCard(
                selectedArea = selectedArea,
                onAreaSelected = { selectedArea = it },
                meetingPointText = meetingPointText,
                onMeetingPointChange = { meetingPointText = it },
                requestFieldColors = requestFieldColors
            )

            errorState?.let {
                RequestErrorCard(message = it.message)
            }

            RequestSummaryCard(
                areaName = selectedArea?.label ?: stringResource(R.string.not_selected),
                meetingPoint = meetingPointText.ifBlank { stringResource(R.string.no_additional_notes) }
            )

            RequestSubmitButton(
                isLoading = isLoading,
                enabled = canSubmit,
                onClick = {
                    viewModel.requestTour(
                        longitude = selectedArea!!.longitude,
                        latitude = selectedArea!!.latitude,
                        locationDescription = meetingPointText
                    )
                }
            )
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
                text = stringResource(R.string.request_summary),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            SummaryRow(label = stringResource(R.string.area), value = areaName)
            SummaryRow(label = stringResource(R.string.notes), value = meetingPoint)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestDialogs(
    successState: RequestTourViewModel.RequestUiState.Success?,
    activeServiceState: RequestTourViewModel.RequestUiState.ActiveService?,
    onDismissSuccess: () -> Unit,
    onConfirmSuccess: () -> Unit,
    onDismissActive: () -> Unit,
    onConfirmActive: () -> Unit
) {
    if (successState != null) {
        RequestCreatedDialog(
            service = successState.service,
            areaName = successState.service.startLocationDescription ?: stringResource(R.string.not_specified),
            onDismiss = onDismissSuccess,
            onConfirm = onConfirmSuccess
        )
    }

    if (activeServiceState != null) {
        ActiveServiceDialog(
            service = activeServiceState.service,
            message = activeServiceState.message,
            onDismiss = onDismissActive,
            onConfirm = onConfirmActive
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.create_service_request),
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = TextPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestFormCard(
    selectedArea: RequestAreaOption?,
    onAreaSelected: (RequestAreaOption) -> Unit,
    meetingPointText: String,
    onMeetingPointChange: (String) -> Unit,
    requestFieldColors: androidx.compose.material3.TextFieldColors
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
                text = stringResource(R.string.service_need_description),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.tour_request_explanation),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            var areaExpanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = areaExpanded,
                onExpandedChange = { areaExpanded = !areaExpanded }
            ) {
                OutlinedTextField(
                    value = selectedArea?.label.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.destination_area)) },
                    placeholder = { Text(stringResource(R.string.select_city)) },
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
                                onAreaSelected(option)
                                areaExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = meetingPointText,
                onValueChange = { onMeetingPointChange(it.take(255)) },
                label = { Text(stringResource(R.string.meeting_point_notes)) },
                placeholder = { Text(stringResource(R.string.meeting_point_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = requestFieldColors
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = message,
            color = ErrorDark,
            modifier = Modifier.padding(14.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestSubmitButton(
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
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
                text = stringResource(R.string.create_service),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestCreatedDialog(
    service: ServiceResponse,
    areaName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.service_created_successfully)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.request_visible_to_partners))
                Text("${stringResource(R.string.service_id_prefix)}${service.serviceId}")
                Text("${stringResource(R.string.area_prefix)}$areaName")
                Text("${stringResource(R.string.status_prefix)}${service.status}")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.done_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.stay_here))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveServiceDialog(
    service: ServiceResponse,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.active_request_exists)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(message)
                Text("${stringResource(R.string.service_id_prefix)}${service.serviceId}")
                Text("${stringResource(R.string.status_prefix)}${service.status}")
                Text(stringResource(R.string.open_waiting_screen_message))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.done_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.stay_here_button))
            }
        }
    )
}

