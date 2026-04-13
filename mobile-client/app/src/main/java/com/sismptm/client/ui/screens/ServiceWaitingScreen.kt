package com.sismptm.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sismptm.client.data.remote.RetrofitClient
import com.sismptm.client.data.remote.ServiceResponse
import com.sismptm.client.ui.theme.Background
import com.sismptm.client.ui.theme.CardBackground
import com.sismptm.client.ui.theme.PrimaryAccent
import com.sismptm.client.ui.theme.TextPrimary
import com.sismptm.client.ui.theme.TextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

private val cancellableStatuses = setOf("REQUESTED", "ACCEPTED")
private val terminalStatuses = setOf("COMPLETED", "CANCELLED")

data class ServiceWaitingUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isCancelling: Boolean = false,
    val service: ServiceResponse? = null,
    val error: String? = null,
    val infoMessage: String? = null
)

class ServiceWaitingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ServiceWaitingUiState())
    val uiState: StateFlow<ServiceWaitingUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var currentServiceId: Long? = null

    fun load(serviceId: Long) {
        if (currentServiceId == serviceId && _uiState.value.service != null) {
            return
        }
        currentServiceId = serviceId
        fetchService(serviceId, isManualRefresh = false)
        startPolling(serviceId)
    }

    fun manualRefresh() {
        val serviceId = currentServiceId ?: return
        fetchService(serviceId, isManualRefresh = true)
    }

    fun cancelService() {
        val serviceId = currentServiceId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCancelling = true, error = null, infoMessage = null)
            runCatching {
                RetrofitClient.apiService.cancelService(serviceId)
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _uiState.value = _uiState.value.copy(
                            isCancelling = false,
                            service = body,
                            infoMessage = "Tour cancelled successfully."
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isCancelling = false,
                            error = "Server returned an empty response."
                        )
                    }
                } else {
                    val backendError = parseBackendError(response.errorBody()?.string().orEmpty())
                    _uiState.value = _uiState.value.copy(
                        isCancelling = false,
                        error = backendError.ifBlank { "Could not cancel the request (${response.code()})." }
                    )
                }
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(
                    isCancelling = false,
                    error = ex.localizedMessage ?: "Connection error while cancelling"
                )
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun fetchService(serviceId: Long, isManualRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = !isManualRefresh && _uiState.value.service == null,
                isRefreshing = isManualRefresh,
                error = null,
                infoMessage = if (isManualRefresh) null else _uiState.value.infoMessage
            )

            runCatching {
                RetrofitClient.apiService.getServiceById(serviceId)
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    val service = response.body()
                    if (service != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isRefreshing = false,
                            service = service,
                            error = null
                        )
                        if (service.status.uppercase() in terminalStatuses) {
                            stopPolling()
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = "Server returned an empty response."
                        )
                    }
                } else {
                    val backendError = parseBackendError(response.errorBody()?.string().orEmpty())
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = backendError.ifBlank { "Could not fetch service status (${response.code()})." }
                    )
                }
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = ex.localizedMessage ?: "Connection error while loading service"
                )
            }
        }
    }

    private fun startPolling(serviceId: Long) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(8000)
                fetchService(serviceId, isManualRefresh = false)
            }
        }
    }

    private fun parseBackendError(body: String): String = runCatching {
        if (body.isBlank()) "" else JSONObject(body).optString("error", "")
    }.getOrDefault("")

    override fun onCleared() {
        stopPolling()
        super.onCleared()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceWaitingScreen(
    serviceId: Long,
    onBackHome: () -> Unit
) {
    val viewModel: ServiceWaitingViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val service = uiState.service
    val status = service?.status?.uppercase().orEmpty()
    val canCancel = status in cancellableStatuses
    val isTerminal = status in terminalStatuses

    LaunchedEffect(serviceId) {
        viewModel.load(serviceId)
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Service status", color = TextPrimary, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = PrimaryAccent)
                Text("Loading your active request...", color = TextSecondary)
            }

            uiState.error?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Text(
                        text = message,
                        color = Color(0xFFC62828),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            uiState.infoMessage?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Text(
                        text = message,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (service != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusBadge(status = service.status)
                        Text("Status: ${service.status}", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = when (status) {
                                "REQUESTED" -> "Waiting for a partner to accept your request."
                                "ACCEPTED" -> "A partner accepted your request. Waiting for tour start."
                                "STARTED" -> "Tour is in progress."
                                "COMPLETED" -> "Tour finished successfully."
                                "CANCELLED" -> "Tour was cancelled."
                                else -> "Checking latest status..."
                            },
                            color = TextSecondary
                        )
                        Text("Area ID: ${service.areaId}", color = TextSecondary)
                        Text("Hours: ${service.agreedHours}", color = TextSecondary)
                        Text("Rate: ${service.hourlyRate} COP", color = TextSecondary)
                    }
                }
            }

            if (canCancel && !isTerminal) {
                OutlinedButton(
                    onClick = { viewModel.cancelService() },
                    enabled = !uiState.isCancelling,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isCancelling) {
                        CircularProgressIndicator(color = Color(0xFFC62828))
                    } else {
                        Text("Cancel tour", color = Color(0xFFC62828))
                    }
                }
            }

            OutlinedButton(
                onClick = { viewModel.manualRefresh() },
                enabled = !uiState.isRefreshing && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isRefreshing) "Refreshing..." else "Refresh status")
            }

            Button(
                onClick = onBackHome,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to home")
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val normalized = status.uppercase()
    val label = if (normalized == "REQUESTED") "CREATED" else normalized
    val (bg, fg) = when (normalized) {
        "REQUESTED" -> Color(0xFF263238) to Color(0xFF90CAF9)
        "ACCEPTED" -> Color(0xFF1B5E20) to Color(0xFFA5D6A7)
        "STARTED" -> Color(0xFF4E342E) to Color(0xFFFFCC80)
        "COMPLETED" -> Color(0xFF0D47A1) to Color(0xFFBBDEFB)
        "CANCELLED" -> Color(0xFFB71C1C) to Color(0xFFFFCDD2)
        else -> Color(0xFF37474F) to Color(0xFFECEFF1)
    }

    Card(colors = CardDefaults.cardColors(containerColor = bg)) {
        Text(
            text = label,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

