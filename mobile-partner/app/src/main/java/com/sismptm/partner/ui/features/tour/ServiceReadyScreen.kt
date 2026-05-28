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
import com.sismptm.partner.core.websocket.ServiceWebSocketClient

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
    
    // Observe WebSocket notifications for server-driven time gate
    val webSocketClient = remember { ServiceWebSocketClient.getInstance(context) }
    val serviceTimeArrivedUpdate by webSocketClient.serviceTimeArrivedUpdate.collectAsState()
    val serviceStatusUpdate by webSocketClient.serviceStatusUpdate.collectAsState()

    var showCancelDialog by remember { mutableStateOf(false) }
    var isTimeToStartEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(serviceId) {
        viewModel.fetchService(serviceId)
    }

    // CRITICAL: Server validates time gate via WebSocket SERVICE_TIME_ARRIVED notification
    // Partner cannot manipulate device time to bypass this check
    LaunchedEffect(serviceTimeArrivedUpdate) {
        serviceTimeArrivedUpdate?.let { update ->
            if (update.serviceId == serviceId) {
                isTimeToStartEnabled = true
            }
        }
    }
    
    // Immediate service (no scheduled time) can start immediately
    LaunchedEffect(serviceState) {
        serviceState?.let { service ->
            if (service.scheduledAt.isNullOrBlank()) {
                // Immediate service: no scheduled time, can start now
                isTimeToStartEnabled = true
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
            title = { Text("Cancelar servicio", color = Color.White) },
            text = {
                Text(
                    "¿Estás seguro de que deseas cancelar el servicio #$serviceId? Esta acción no se puede deshacer.",
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
                    Text("Sí, cancelar", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Volver", color = Color(0xFF9DA5B3))
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

            // Scheduled time info banner: server-driven
            if (serviceState?.scheduledAt?.isNotBlank() == true && !isTimeToStartEnabled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF78350F)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Esperando la hora programada. Te notificaremos cuando puedas iniciar.",
                        color = Color(0xFFFBD34D),
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
                // SECURITY: Server must approve via WebSocket notification
                // Local time validation removed to prevent manipulation
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
                    Text("Cancelar servicio", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
