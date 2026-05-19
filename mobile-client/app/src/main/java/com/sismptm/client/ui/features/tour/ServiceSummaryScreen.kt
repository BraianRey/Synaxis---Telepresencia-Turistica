package com.sismptm.client.ui.features.tour

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sismptm.client.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sismptm.client.core.network.RetrofitClient
import com.sismptm.client.data.remote.api.dto.PaymentSummaryResponse
import com.sismptm.client.data.remote.api.dto.ServiceResponse
import com.sismptm.client.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Service Summary screen using StateFlow.
 */
class ServiceSummaryViewModel : ViewModel() {
    private val _service = MutableStateFlow<ServiceResponse?>(null)
    val service = _service.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _payment = MutableStateFlow<PaymentSummaryResponse?>(null)
    val payment = _payment.asStateFlow()
    private val _paymentConfirmed = MutableStateFlow(false)
    val paymentConfirmed = _paymentConfirmed.asStateFlow()
    private var paymentPollingJob: Job? = null

    fun loadService(serviceId: Long) {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getServiceById(serviceId)
                if (response.isSuccessful) {
                    _service.value = response.body()
                    try {
                        val paymentResponse = RetrofitClient.apiService.getPaymentSummary(serviceId)
                        if (paymentResponse.isSuccessful) {
                            _payment.value = paymentResponse.body()
                        }
                        // Payment load failure is silent — service data is still shown
                    } catch (e: Exception) {
                        // Silent — do not override existing _error state
                    }
                } else {
                    _error.value = "Failed to load service details "
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setService(serviceResponse: ServiceResponse) {
        _service.value = serviceResponse
    }

    fun confirmPayment(serviceId: Long) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.confirmPayment(serviceId)
                if (response.isSuccessful) {
                    _paymentConfirmed.value = true
                    _payment.value = response.body()
                    Log.i("ServiceSummaryViewModel", "Payment confirmed for service $serviceId")
                }
            } catch (e: Exception) {
                Log.e("ServiceSummaryViewModel", "Error confirming payment: ${e.message}")
            }
        }
    }

    fun startPaymentPolling(serviceId: Long) {
        if (paymentPollingJob?.isActive == true || _payment.value != null) return
        paymentPollingJob = viewModelScope.launch {
            while (_payment.value == null) {
                try {
                    val response = RetrofitClient.apiService.getPaymentSummary(serviceId)
                    if (response.isSuccessful) {
                        _payment.value = response.body()
                        if (_payment.value != null) {
                            Log.i("ServiceSummaryViewModel", "Payment detected for service $serviceId")
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ServiceSummaryViewModel", "Polling payment error: ${e.message}")
                }
                delay(5000)
            }
        }
    }

    fun stopPaymentPolling() {
        paymentPollingJob?.cancel()
        paymentPollingJob = null
    }
}

/**
 * Screen displaying a summary of a completed service.
 * Shows duration, cost, and partner information.
 *
 * @param serviceId The ID of the completed service
 * @param service Optional pre-loaded service data
 * @param onBackToHome Callback to navigate back to home screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceSummaryScreen(
    serviceId: Long,
    service: ServiceResponse? = null,
    onBackToHome: () -> Unit,
    viewModel: ServiceSummaryViewModel = viewModel()
) {
    // Load service data if not provided
    LaunchedEffect(serviceId) {
        if (service != null) {
            viewModel.setService(service)
        } else {
            viewModel.loadService(serviceId)
        }
    }
    val serviceData by viewModel.service.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val payment by viewModel.payment.collectAsStateWithLifecycle()
    val paymentConfirmed by viewModel.paymentConfirmed.collectAsStateWithLifecycle()
    LaunchedEffect(serviceId, payment) {
        if (payment == null) {
            viewModel.startPaymentPolling(serviceId)
        } else {
            viewModel.stopPaymentPolling()
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.service_summary),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackToHome) {
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
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PrimaryAccent
                    )
                }
                error != null -> {
                    ErrorView(
                        message = error ?: "Unknown error",
                        onRetry = { viewModel.loadService(serviceId) },
                        onBack = onBackToHome
                    )
                }
                serviceData != null -> {
                    ServiceSummaryContent(
                        service = serviceData!!,
                        payment = payment,
                        paymentConfirmed = paymentConfirmed,
                        onConfirmPayment = { viewModel.confirmPayment(serviceId) },
                        onBackToHome = onBackToHome
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFE53935)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onBack) {
            Text(stringResource(R.string.back_to_home))
        }
    }
}

@Composable
private fun ServiceSummaryContent(
    service: ServiceResponse,
    payment: PaymentSummaryResponse?,
    paymentConfirmed: Boolean,
    onConfirmPayment: () -> Unit,
    onBackToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Success Header
        SuccessHeaderCard()

        Spacer(modifier = Modifier.height(16.dp))

        if (payment != null && !paymentConfirmed) {
            Button(
                onClick = onConfirmPayment,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AttachMoney,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.confirm_payment),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        } else if (paymentConfirmed) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.payment_confirmed),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
        }

        // Service Stats (Duration & Cost)
        ServiceStatsCard(service, payment)

        // Partner Information
        PartnerInfoCard(service)

        // Service Details
        ServiceDetailsCard(service)

        Spacer(modifier = Modifier.weight(1f))

        // Back to Home Button
        Button(
            onClick = onBackToHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                stringResource(R.string.back_to_home),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun SuccessHeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = Color(0xFF4CAF50)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.padding(16.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.service_completed),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.thank_you_synexis),
                fontSize = 14.sp,
                color = Color(0xFFA5D6A7),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ServiceStatsCard(service: ServiceResponse, payment: PaymentSummaryResponse?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.service_stats),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Duration Stat
                StatItem(
                    icon = Icons.Default.Timer,
                    label = stringResource(R.string.duration),
                    value = service.getFormattedDuration(),
                    color = PrimaryAccent
                )

                // Cost Stat
                StatItem(
                    icon = Icons.Default.AttachMoney,
                    label = stringResource(R.string.total_cost),
                    value = payment?.let {
                        stringResource(R.string.currency_format, "%.2f".format(it.totalAmount))
                    } ?: service.getFormattedCost(),
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.2f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(12.dp),
                tint = color
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun PartnerInfoCard(service: ServiceResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.your_guide),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = service.partnerName ?: stringResource(R.string.unknown_guide),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = service.partnerEmail ?: "",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }

            // Rating placeholder
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(5) { index ->
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (index < 4) StarColor else TextSecondary
                    )
                }
                Text(
                    text = "(4.8)",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ServiceDetailsCard(service: ServiceResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.service_details),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            DetailItem(
                icon = Icons.Default.LocationOn,
                label = stringResource(R.string.label_location),
                value = service.startLocationDescription ?: stringResource(R.string.not_specified)
            )

            service.locationReferenceImageUrl?.let { imageUrl ->
                Spacer(modifier = Modifier.height(16.dp))
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Reference image of service location",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            DetailItem(
                icon = Icons.Default.AccessTime,
                label = stringResource(R.string.service_id_label),
                value = stringResource(R.string.service_prefix) + service.serviceId
            )

            DetailItem(
                icon = Icons.Default.CalendarToday,
                label = stringResource(R.string.completed_on),
                value = service.endedAt?.let {
                    try {
                        val instant = java.time.Instant.parse(it)
                        val formatter = java.time.format.DateTimeFormatter
                            .ofPattern("MMM dd, yyyy - HH:mm")
                            .withZone(java.time.ZoneId.systemDefault())
                        formatter.format(instant)
                    } catch (e: Exception) {
                        it
                    }
                } ?: stringResource(R.string.not_available)
            )
        }
    }
}

@Composable
private fun DetailItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = TextSecondary
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextSecondary
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
