package com.sismptm.partner.ui.screens

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sismptm.partner.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ServiceReadyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ReadyUiState>(ReadyUiState.Idle)
    val uiState: StateFlow<ReadyUiState> = _uiState

    sealed interface ReadyUiState {
        object Idle : ReadyUiState
        object Loading : ReadyUiState
        object Success : ReadyUiState
        data class Error(val message: String) : ReadyUiState
    }

    fun markAsReady(serviceId: Long) {
        viewModelScope.launch {
            _uiState.value = ReadyUiState.Loading
            try {
                val response = RetrofitClient.apiService.markServiceAsReady(serviceId)
                if (response.isSuccessful) {
                    _uiState.value = ReadyUiState.Success
                } else {
                    _uiState.value = ReadyUiState.Error("Failed to notify server. Please try again.")
                }
            } catch (e: Exception) {
                _uiState.value = ReadyUiState.Error(e.localizedMessage ?: "Connection error")
            }
        }
    }
}

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
                    text = "Service Accepted!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Service ID: #$serviceId",
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
                        text = "The client is being notified. Please tap below when you are physically ready to start the streaming.",
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
                    Text("I AM READY", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Text("Cancel and go back", color = Color(0xFF9DA5B3))
        }
    }
}
