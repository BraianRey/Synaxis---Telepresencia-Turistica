package com.sismptm.partner.ui.features.tour

import androidx.compose.ui.res.stringResource
import com.sismptm.partner.R

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import com.sismptm.partner.core.network.NetworkConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sismptm.partner.core.network.RetrofitClient
import com.sismptm.partner.data.remote.api.dto.PaymentSummaryResponse
import com.sismptm.partner.data.remote.api.dto.ServiceResponse
import com.sismptm.partner.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Partner Service Summary screen using StateFlow.
 */
class PartnerServiceSummaryViewModel : ViewModel() {
    private val _service = MutableStateFlow<ServiceResponse?>(null)
    val service = _service.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _payment = MutableStateFlow<PaymentSummaryResponse?>(null)
    val payment = _payment.asStateFlow()

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
                    _error.value = "Failed to load service details"
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
}

/**
 * Screen displaying a summary of a completed service from the partner's perspective.
 * Shows duration, cost, and client information.
 *
 * @param serviceId The ID of the completed service
 * @param service Optional pre-loaded service data
 * @param onBackToHome Callback to navigate back to home screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerServiceSummaryScreen(
    serviceId: Long,
    service: ServiceResponse? = null,
    onBackToHome: () -> Unit,
    viewModel: PartnerServiceSummaryViewModel = viewModel()
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
                        message = error ?: stringResource(R.string.unknown_error),
                        onRetry = { viewModel.loadService(serviceId) },
                        onBack = onBackToHome
                    )
                }
                serviceData != null -> {
                    PartnerServiceSummaryContent(
                        service = serviceData!!,
                        payment = payment,
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
private fun PartnerServiceSummaryContent(
    service: ServiceResponse,
    payment: PaymentSummaryResponse?,
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
        PartnerSuccessHeaderCard()

        // Earnings Stats
        PartnerEarningsCard(service, payment)

        // Client Information
        ClientInfoCard(service)

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
                stringResource(R.string.back_to_dashboard),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun PartnerSuccessHeaderCard() {
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
                text = stringResource(R.string.service_completed_msg),
                fontSize = 14.sp,
                color = Color(0xFFA5D6A7),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PartnerEarningsCard(service: ServiceResponse, payment: PaymentSummaryResponse?) {
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
                text = stringResource(R.string.your_earnings),
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
                    label = stringResource(R.string.duration_label),
                    value = service.getFormattedDuration(),
                    color = PrimaryAccent
                )

                // Earnings Stat
                StatItem(
                    icon = Icons.Default.AttachMoney,
                    label = stringResource(R.string.you_earned_label),
                    value = payment?.let {
                        stringResource(R.string.currency_format, String.format("%,.0f", it.totalAmount))
                    } ?: service.getFormattedCost(),
                    color = Color(0xFF4CAF50)
                )
            }

            // Rate info
            Text(
                text = payment?.let {
                    "${stringResource(R.string.label_total_prefix)}${stringResource(R.string.currency_format, String.format("%,.0f", it.totalAmount))} • ${it.actualDurationMin}${stringResource(R.string.minutes_unit_short)}"
                } ?: service.hourlyRate?.let {
                    "${stringResource(R.string.label_rate_prefix)}${stringResource(R.string.currency_format, String.format("%,.0f", it))}${stringResource(R.string.per_hour_suffix)}"
                } ?: stringResource(R.string.rate_na),
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
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
private fun ClientInfoCard(service: ServiceResponse) {
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
                text = stringResource(R.string.client_info_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    if (service.clientPicDirectory != null) {
                        val imageUrl = NetworkConfig.BASE_URL + service.clientPicDirectory
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl)
                                .crossfade(300)
                                .build(),
                            contentDescription = "Client picture",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = service.clientName ?: stringResource(R.string.unknown_client),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = service.clientEmail ?: "",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
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
                label = stringResource(R.string.location_label),
                value = service.startLocationDescription ?: stringResource(R.string.not_specified)
            )

            DetailItem(
                icon = Icons.Default.AccessTime,
                label = stringResource(R.string.tour_detail_service_id).substringBefore(":"),
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
                } ?: stringResource(R.string.rate_na)
            )

            DetailItem(
                icon = Icons.Default.Schedule,
                label = stringResource(R.string.agreed_hours_label),
                value = "${service.agreedHours ?: stringResource(R.string.rate_na)}${stringResource(R.string.hours_unit)}"
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
