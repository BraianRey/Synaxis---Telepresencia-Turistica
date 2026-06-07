package com.sismptm.partner.ui.features.tour

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sismptm.partner.R
import kotlinx.coroutines.delay
import java.time.Instant

/**
 * Screen that prepares the partner for the streaming session after accepting a tour.
 * Allows the partner to start the transmission (gated by scheduled time) or
 * cancel the service and return to Home.
 */
@Composable
fun ServiceReadyScreen(
    serviceId: Long,
    onReadyConfirmed: (Long) -> Unit,
    onBack: () -> Unit,
    onServiceCancelled: () -> Unit = onBack,
    viewModel: ServiceReadyViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val cancelUiState by viewModel.cancelUiState.collectAsState()
    val serviceState by viewModel.serviceState.collectAsState()

    var showCancelDialog by remember { mutableStateOf(false) }
    var isTimeToStartEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(serviceId) {
        viewModel.fetchService(serviceId)
    }
    
    // Improved time-gate logic to enable start button for scheduled services
    LaunchedEffect(serviceState) {
        serviceState?.let { service ->
            if (service.scheduled == false || service.status.uppercase() in setOf("READY", "ACCEPTED")) {
                isTimeToStartEnabled = true
            } else if (service.status.uppercase() == "WAITING_FOR_START") {
                // Periodically check if the scheduled time has arrived
                while (true) {
                    val scheduledTime = service.scheduledAt?.let {
                        try { Instant.parse(it) } catch (e: Exception) { null }
                    }
                    
                    val now = Instant.now()
                    if (scheduledTime == null || now.isAfter(scheduledTime) || now == scheduledTime) {
                        isTimeToStartEnabled = true
                        break 
                    } else {
                        isTimeToStartEnabled = false
                    }
                    delay(5000) // Re-check every 5 seconds
                }
            }
        }
    }

    // Navigate to streaming on successful start
    LaunchedEffect(uiState) {
        if (uiState is ServiceReadyViewModel.ReadyUiState.Success) {
            onReadyConfirmed(serviceId)
        }
    }

    // Navigate back to Home on successful cancellation
    LaunchedEffect(cancelUiState) {
        if (cancelUiState is ServiceReadyViewModel.CancelUiState.Success) {
            onServiceCancelled()
        }
    }

    // Confirmation dialog before cancelling
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(stringResource(R.string.cancel_service), color = Color.White) },
            text = {
                Text(
                    stringResource(R.string.cancel_service_confirm, serviceId),
                    color = Color(0xFF9DA5B3)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        viewModel.cancelService(serviceId)
                    }
                ) {
                    Text(stringResource(R.string.yes_cancel), color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text(stringResource(R.string.go_back), color = Color(0xFF9DA5B3))
                }
            },
            containerColor = Color(0xFF1E2430)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF12151B))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = Color(0xFF2563EB).copy(alpha = 0.2f)
            ) {
                Icon(
                    imageVector = Icons.Default.Stream,
                    contentDescription = null,
                    modifier = Modifier.padding(20.dp),
                    tint = Color(0xFF2563EB)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.ready_to_start),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.service_ref_prefix) + serviceId,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF9DA5B3)
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2430)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF2563EB))
                    Text(
                        text = stringResource(R.string.ready_start_instruction),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB9C0CB)
                    )
                }
            }

            // Error from start-transmission
            if (uiState is ServiceReadyViewModel.ReadyUiState.Error) {
                Text(
                    text = (uiState as ServiceReadyViewModel.ReadyUiState.Error).message,
                    color = Color(0xFFEF4444),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            // Error from cancellation
            if (cancelUiState is ServiceReadyViewModel.CancelUiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1219)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = (cancelUiState as ServiceReadyViewModel.CancelUiState.Error).message,
                        color = Color(0xFFF87171),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Scheduled time info banner: show when waiting for time
            if (serviceState?.scheduled == true && serviceState?.status?.uppercase() == "WAITING_FOR_START") {
                val bannerColor = if (isTimeToStartEnabled) Color(0xFF065F46) else Color(0xFF78350F)
                val textColor = if (isTimeToStartEnabled) Color(0xFFD1FAE5) else Color(0xFFFBD34D)
                val text = if (isTimeToStartEnabled) 
                    stringResource(R.string.its_time_ready)
                    else stringResource(R.string.waiting_scheduled_time)

                Card(
                    colors = CardDefaults.cardColors(containerColor = bannerColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = text,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Start Transmission button
            val isActionInProgress = uiState is ServiceReadyViewModel.ReadyUiState.Loading ||
                    cancelUiState is ServiceReadyViewModel.CancelUiState.Loading

            Button(
                onClick = { viewModel.markAsReady(serviceId) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    disabledContainerColor = Color(0xFF2563EB).copy(alpha = 0.4f)
                ),
                enabled = !isActionInProgress && isTimeToStartEnabled
            ) {
                if (uiState is ServiceReadyViewModel.ReadyUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Stream, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_start_transmission), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // Cancel Service button
            OutlinedButton(
                onClick = { showCancelDialog = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isActionInProgress,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = if (isActionInProgress) 0.3f else 1f))
            ) {
                if (cancelUiState is ServiceReadyViewModel.CancelUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFFEF4444), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.cancel_service), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }

        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Text(stringResource(R.string.btn_go_back_dashboard), color = Color(0xFF9DA5B3))
        }
    }
}
