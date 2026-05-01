package com.sismptm.partner.ui.features.tour

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sismptm.partner.R

/**
 * Screen that prepares the partner for the streaming session after accepting a tour.
 */
@Composable
fun ServiceReadyScreen(
    serviceId: Long,
    onReadyConfirmed: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ServiceReadyViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is ServiceReadyViewModel.ReadyUiState.Success) {
            onReadyConfirmed(serviceId)
        }
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
                    text = "Ready to Start?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Service Reference: #$serviceId",
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
                        text = "The client has been notified. Please confirm your readiness to begin the live broadcast.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB9C0CB)
                    )
                }
            }

            if (uiState is ServiceReadyViewModel.ReadyUiState.Error) {
                Text(
                    text = (uiState as ServiceReadyViewModel.ReadyUiState.Error).message,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = { viewModel.markAsReady(serviceId) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                enabled = uiState !is ServiceReadyViewModel.ReadyUiState.Loading
            ) {
                if (uiState is ServiceReadyViewModel.ReadyUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("START TRANSMISSION", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Text("Go back to dashboard", color = Color(0xFF9DA5B3))
        }
    }
}

