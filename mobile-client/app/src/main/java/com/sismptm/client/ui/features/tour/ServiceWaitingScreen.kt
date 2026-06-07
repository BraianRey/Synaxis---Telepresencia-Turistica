package com.sismptm.client.ui.features.tour

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight as TextFontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sismptm.client.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sismptm.client.core.network.RetrofitClient
import com.sismptm.client.data.remote.api.dto.ServiceResponse
import com.sismptm.client.ui.theme.Background
import com.sismptm.client.ui.theme.CardBackground
import com.sismptm.client.ui.theme.ErrorDark
import com.sismptm.client.ui.theme.PrimaryAccent
import com.sismptm.client.ui.theme.Success
import com.sismptm.client.ui.theme.SuccessBackground
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
import java.time.Instant

private val cancellableStatuses = setOf("REQUESTED", "ACCEPTED", "WAITING_FOR_START", "READY")
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
                delay(5000)
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
    onBackHome: () -> Unit,
    onNavigateToStreaming: (Long) -> Unit,
    onNavigateToSummary: (Long) -> Unit = {}
) {
    val viewModel: ServiceWaitingViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val service = uiState.service
    val status = service?.status?.uppercase().orEmpty()
    val canCancel = status in cancellableStatuses
    val isTerminal = status in terminalStatuses

    // Added: local time check to sync status visually when reservation time arrives
    var isTimeToStart by remember { mutableStateOf(false) }
    LaunchedEffect(service?.scheduledAt) {
        val scheduledAt = service?.scheduledAt
        if (!scheduledAt.isNullOrBlank()) {
            while (true) {
                try {
                    val scheduled = Instant.parse(scheduledAt)
                    val now = Instant.now()
                    isTimeToStart = !now.isBefore(scheduled)
                    if (isTimeToStart) break
                } catch (e: Exception) {}
                delay(5000)
            }
        } else {
            isTimeToStart = false
        }
    }

    LaunchedEffect(serviceId) {
        viewModel.load(serviceId)
    }

    // Auto-navigate to streaming when status is READY or IN_PROGRESS or STARTED
    LaunchedEffect(status) {
        if (status == "READY" || status == "STARTED" || status == "IN_PROGRESS") {
            onNavigateToStreaming(serviceId)
        }
    }

    LaunchedEffect(status) {
        if (status == "COMPLETED") {
            onNavigateToSummary(serviceId)
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.service_status), color = TextPrimary, fontWeight = FontWeight.Bold) },
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
            loadingSection(uiState.isLoading)
            errorCard(uiState.error)
            infoCard(uiState.infoMessage)
            serviceDetailsCard(service, status)
            cancelButton(canCancel, isTerminal, uiState.isCancelling) { viewModel.cancelService() }
            refreshButton(uiState.isRefreshing, uiState.isLoading) { viewModel.manualRefresh() }
            backToHomeButton(onBackHome)
        }
    }
}

@Composable
private fun loadingSection(isLoading: Boolean) {
    if (isLoading) {
        CircularProgressIndicator(color = PrimaryAccent)
        Text(stringResource(R.string.loading_active_request), color = TextSecondary)
    }
}

@Composable
private fun errorCard(error: String?) {
    error?.let { message ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
        ) {
            Text(text = message, color = Color(0xFFC62828), modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
private fun infoCard(infoMessage: String?) {
    infoMessage?.let { message ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SuccessBackground)
        ) {
            Text(text = message, color = Color(0xFF2E7D32), modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
private fun serviceDetailsCard(service: ServiceResponse?, status: String) {
    if (service != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statusBadge(status = service.status)
                Text("${stringResource(R.string.service_prefix)}${service.serviceId}", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    text = when (status) {
                        "REQUESTED" -> stringResource(R.string.waiting_for_partner_accept)
                        "ACCEPTED" -> stringResource(R.string.partner_accepted_waiting_start)
                        "READY" -> stringResource(R.string.ready_for_tour_start)
                        "STARTED" -> stringResource(R.string.partner_ready_streaming)
                        "COMPLETED" -> stringResource(R.string.tour_finished_successfully)
                        "CANCELLED" -> stringResource(R.string.tour_cancelled)
                        else -> stringResource(R.string.checking_latest_status)
                    },
                    color = TextSecondary
                )
                Text("${stringResource(R.string.location_prefix)}${service.startLocationDescription ?: stringResource(R.string.not_specified)}", color = TextSecondary)
            }
        }
    }
}

@Composable
private fun cancelButton(canCancel: Boolean, isTerminal: Boolean, isCancelling: Boolean, onCancel: () -> Unit) {
    if (canCancel && !isTerminal) {
        OutlinedButton(
            onClick = onCancel,
            enabled = !isCancelling,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isCancelling) {
                CircularProgressIndicator(color = ErrorDark)
            } else {
                Text(stringResource(R.string.cancel_tour), color = ErrorDark)
            }
        }
    }
}

@Composable
private fun refreshButton(isRefreshing: Boolean, isLoading: Boolean, onRefresh: () -> Unit) {
    OutlinedButton(
        onClick = onRefresh,
        enabled = !isRefreshing && !isLoading,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (isRefreshing) stringResource(R.string.refreshing) else stringResource(R.string.refresh_status))
    }
}

@Composable
private fun backToHomeButton(onBackHome: () -> Unit) {
    Button(
        onClick = onBackHome,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.back_to_home))
    }
}

private fun getStatusColors(status: String): Pair<Color, Color> = when (status.uppercase()) {
    "REQUESTED" -> Color(0xFF263238) to Color(0xFF90CAF9)
    "ACCEPTED" -> Color(0xFF1B5E20) to Color(0xFFA5D6A7)
    "READY" -> Success to SuccessBackground
    "STARTED" -> Color(0xFF4E342E) to Color(0xFFFFCC80)
    "COMPLETED" -> Color(0xFF0D47A1) to Color(0xFFBBDEFB)
    "CANCELLED" -> Color(0xFFB71C1C) to Color(0xFFFFCDD2)
    else -> Color(0xFF37474F) to Color(0xFFECEFF1)
}

@Composable
private fun statusBadge(status: String) {
    val normalized = status.uppercase()
    val label = if (normalized == "REQUESTED") "CREATED" else normalized
    val (bg, fg) = getStatusColors(normalized)

    Card(colors = CardDefaults.cardColors(containerColor = bg)) {
        Text(
            text = label,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
