package com.sismptm.client.ui.features.tour

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.sismptm.client.core.network.NetworkConfig
import com.sismptm.client.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sismptm.client.core.network.RetrofitClient
import com.sismptm.client.data.remote.api.dto.PaymentSummaryResponse
import com.sismptm.client.data.remote.api.dto.RatingRequest
import com.sismptm.client.data.remote.api.dto.RatingResponse
import com.sismptm.client.data.remote.api.dto.ServiceResponse
import com.sismptm.client.ui.theme.Background
import com.sismptm.client.ui.theme.CardBackground
import com.sismptm.client.ui.theme.Error
import com.sismptm.client.ui.theme.PrimaryAccent
import com.sismptm.client.ui.theme.StarColor
import com.sismptm.client.ui.theme.Success
import com.sismptm.client.ui.theme.SuccessLight
import com.sismptm.client.ui.theme.TextPrimary
import com.sismptm.client.ui.theme.TextSecondary
import com.sismptm.client.ui.theme.TextTertiary
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

    // Rating state
    private val _existingRating = MutableStateFlow<RatingResponse?>(null)
    val existingRating = _existingRating.asStateFlow()

    private val _showRatingDialog = MutableStateFlow(false)
    val showRatingDialog = _showRatingDialog.asStateFlow()

    private val _ratingScore = MutableStateFlow(0)
    val ratingScore = _ratingScore.asStateFlow()

    private val _ratingComment = MutableStateFlow("")
    val ratingComment = _ratingComment.asStateFlow()

    private val _isSubmittingRating = MutableStateFlow(false)
    val isSubmittingRating = _isSubmittingRating.asStateFlow()

    private val _ratingError = MutableStateFlow<String?>(null)
    val ratingError = _ratingError.asStateFlow()

    private val _ratingSubmitted = MutableStateFlow(false)
    val ratingSubmitted = _ratingSubmitted.asStateFlow()

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
                    } catch (e: java.io.IOException) {
                        // Generic exception kept: Could be network or parsing error
                        // Silent — do not override existing _error state
                        Log.e("ServiceSummaryViewModel", "Payment load failed silently", e)
                    }
                } else {
                    _error.value = "Failed to load service details "
                }
            } catch (e: java.io.IOException) {
                // Generic exception kept: Could be network or parsing error
                Log.e("ServiceSummaryViewModel", "Failed to load service details", e)
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
            } catch (e: java.io.IOException) {
                // Generic exception kept: Network or communication error
                Log.e("ServiceSummaryViewModel", "Error confirming payment", e)
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
                } catch (e: java.io.IOException) {
                    // Generic exception kept: Network error during polling
                    Log.e("ServiceSummaryViewModel", "Polling payment error", e)
                }
                delay(5000)
            }
        }
    }

    fun stopPaymentPolling() {
        paymentPollingJob?.cancel()
        paymentPollingJob = null
    }

    fun checkExistingRating(serviceId: Long) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getRatingByService(serviceId)
                if (response.isSuccessful) {
                    _existingRating.value = response.body()
                }
            } catch (e: java.io.IOException) {
                // Generic exception kept: Network error checking rating
                Log.e("ServiceSummaryViewModel", "Error checking rating", e)
            }
        }
    }

    fun openRatingDialog() {
        _showRatingDialog.value = true
        _ratingError.value = null
    }

    fun dismissRatingDialog() {
        _showRatingDialog.value = false
        _ratingScore.value = 0
        _ratingComment.value = ""
        _ratingError.value = null
    }

    fun skipRating() {
        _showRatingDialog.value = false
        _ratingScore.value = 0
        _ratingComment.value = ""
        _ratingError.value = null
    }

    fun onRatingScoreChanged(score: Int) {
        _ratingScore.value = score
        _ratingError.value = null
    }

    fun onRatingCommentChanged(comment: String) {
        _ratingComment.value = comment.take(200)
        _ratingError.value = null
    }

    fun submitRating(serviceId: Long) {
        if (_ratingScore.value < 1) {
            _ratingError.value = "Please select a rating"
            return
        }
        _isSubmittingRating.value = true
        _ratingError.value = null
        viewModelScope.launch {
            try {
                val request = RatingRequest(
                    serviceId = serviceId,
                    score = _ratingScore.value,
                    comment = _ratingComment.value // empty string if blank avoids DB NOT NULL constraint
                )
                val response = RetrofitClient.apiService.createRating(request)
                if (response.isSuccessful) {
                    _existingRating.value = response.body()
                    _showRatingDialog.value = false
                    _ratingSubmitted.value = true
                    _ratingScore.value = 0
                    _ratingComment.value = ""
                    Log.i("ServiceSummaryViewModel", "Rating submitted for service $serviceId")
                } else {
                    val errorBody = response.errorBody()?.string()
                    val backendMessage = try {
                        val json = org.json.JSONObject(errorBody ?: "{}")
                        json.optString("error", "")
                    } catch (ex: org.json.JSONException) {
                        Log.e("ServiceSummaryViewModel", "Error parsing JSON", ex)
                        ""
                    }
                    val displayMessage = if (backendMessage.isNotBlank()) backendMessage
                        else "Failed to submit rating. Try again. (HTTP ${response.code()})"
                    _ratingError.value = displayMessage
                    Log.e("ServiceSummaryViewModel", "Rating failed: HTTP ${response.code()}, body: $errorBody")
                }
            } catch (e: java.io.IOException) {
                // Generic exception kept: Network error submitting rating
                _ratingError.value = e.message ?: "Unknown error"
                Log.e("ServiceSummaryViewModel", "Error submitting rating", e)
            } finally {
                _isSubmittingRating.value = false
            }
        }
    }

    fun resetRatingSubmitted() {
        _ratingSubmitted.value = false
    }
}

private data class ServiceSummaryContentState(
    val payment: PaymentSummaryResponse?,
    val paymentConfirmed: Boolean,
    val existingRating: RatingResponse?
)

private data class ServiceSummaryActions(
    val onConfirmPayment: () -> Unit,
    val onBackToHome: () -> Unit,
    val onOpenRatingDialog: () -> Unit,
    val onRetry: () -> Unit
)

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
        viewModel.checkExistingRating(serviceId)
    }
    val serviceData by viewModel.service.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val payment by viewModel.payment.collectAsStateWithLifecycle()
    val paymentConfirmed by viewModel.paymentConfirmed.collectAsStateWithLifecycle()

    val existingRating by viewModel.existingRating.collectAsStateWithLifecycle()
    val showRatingDialog by viewModel.showRatingDialog.collectAsStateWithLifecycle()
    val ratingScore by viewModel.ratingScore.collectAsStateWithLifecycle()
    val ratingComment by viewModel.ratingComment.collectAsStateWithLifecycle()
    val isSubmittingRating by viewModel.isSubmittingRating.collectAsStateWithLifecycle()
    val ratingError by viewModel.ratingError.collectAsStateWithLifecycle()
    val ratingSubmitted by viewModel.ratingSubmitted.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    ServiceSummaryScreenEffects(
        serviceId = serviceId,
        service = service,
        viewModel = viewModel,
        payment = payment,
        ratingSubmitted = ratingSubmitted,
        snackbarHostState = snackbarHostState
    )

    ServiceSummaryScreenScaffold(
        serviceData = serviceData,
        isLoading = isLoading,
        error = error,
        payment = payment,
        paymentConfirmed = paymentConfirmed,
        existingRating = existingRating,
        snackbarHostState = snackbarHostState,
        actions = ServiceSummaryActions(
            onConfirmPayment = { viewModel.confirmPayment(serviceId) },
            onBackToHome = onBackToHome,
            onOpenRatingDialog = { viewModel.openRatingDialog() },
            onRetry = { viewModel.loadService(serviceId) }
        )
    )

    if (showRatingDialog && existingRating == null) {
        RatingModal(
            score = ratingScore,
            comment = ratingComment,
            isSubmitting = isSubmittingRating,
            error = ratingError,
            onScoreChange = { viewModel.onRatingScoreChanged(it) },
            onCommentChange = { viewModel.onRatingCommentChanged(it) },
            onSubmit = { viewModel.submitRating(serviceId) },
            onSkip = { viewModel.skipRating() },
            onDismiss = { viewModel.dismissRatingDialog() }
        )
    }
}

@Composable
private fun ServiceSummaryScreenEffects(
    serviceId: Long,
    service: ServiceResponse?,
    viewModel: ServiceSummaryViewModel,
    payment: PaymentSummaryResponse?,
    ratingSubmitted: Boolean,
    snackbarHostState: SnackbarHostState
) {
    LaunchedEffect(serviceId) {
        if (service != null) {
            viewModel.setService(service)
        } else {
            viewModel.loadService(serviceId)
        }
        viewModel.checkExistingRating(serviceId)
    }

    LaunchedEffect(serviceId, payment) {
        if (payment == null) {
            viewModel.startPaymentPolling(serviceId)
        } else {
            viewModel.stopPaymentPolling()
        }
    }

    LaunchedEffect(ratingSubmitted) {
        if (ratingSubmitted) {
            snackbarHostState.showSnackbar(
                message = "Thank you! Your rating has been submitted.",
                duration = SnackbarDuration.Short
            )
            viewModel.resetRatingSubmitted()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceSummaryScreenScaffold(
    serviceData: ServiceResponse?,
    isLoading: Boolean,
    error: String?,
    payment: PaymentSummaryResponse?,
    paymentConfirmed: Boolean,
    existingRating: RatingResponse?,
    snackbarHostState: SnackbarHostState,
    actions: ServiceSummaryActions
) {
    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    IconButton(onClick = actions.onBackToHome) {
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
                        onRetry = actions.onRetry,
                        onBack = actions.onBackToHome
                    )
                }
                serviceData != null -> {
                    ServiceSummaryContent(
                        service = serviceData,
                        state = ServiceSummaryContentState(
                            payment = payment,
                            paymentConfirmed = paymentConfirmed,
                            existingRating = existingRating
                        ),
                        actions = actions
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
            tint = Error
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
    state: ServiceSummaryContentState,
    actions: ServiceSummaryActions
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

        if (state.payment != null && !state.paymentConfirmed) {
            Button(
                onClick = actions.onConfirmPayment,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Success
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
        } else if (state.paymentConfirmed) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Success,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.payment_confirmed),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Success
                )
            }
        }

        // Service Stats (Duration & Cost)
        ServiceStatsCard(service, state.payment)

        // Partner Information
        PartnerInfoCard(service, state.existingRating, actions.onOpenRatingDialog)

        // Service Details
        ServiceDetailsCard(service)

        Spacer(modifier = Modifier.weight(1f))

        // Back to Home Button
        Button(
            onClick = actions.onBackToHome,
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
        colors = CardDefaults.cardColors(containerColor = SuccessLight),
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
                color = Success
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
                color = SuccessLight,
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
                    color = Success
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
private fun PartnerInfoCard(
    service: ServiceResponse,
    existingRating: RatingResponse?,
    onOpenRatingDialog: () -> Unit
) {
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

            PartnerHeaderSection(service)

            if (existingRating != null) {
                ExistingRatingSection(existingRating)
            } else {
                RatingButtonSection(onOpenRatingDialog)
            }
        }
    }
}

@Composable
private fun PartnerHeaderSection(service: ServiceResponse) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        PartnerAvatarSection(service)

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
}

@Composable
private fun PartnerAvatarSection(service: ServiceResponse) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color.Gray),
        contentAlignment = Alignment.Center
    ) {
        if (service.partnerPicDirectory != null) {
            val imageUrl = NetworkConfig.BASE_URL + service.partnerPicDirectory
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(300)
                    .build(),
                contentDescription = "Partner picture",
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
}

@Composable
private fun ExistingRatingSection(existingRating: RatingResponse) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(5) { index ->
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (index < existingRating.score) StarColor else TextSecondary
                )
            }
            Text(
                text = "(${existingRating.score}.0)",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        if (!existingRating.comment.isNullOrBlank()) {
            Text(
                text = "\"${existingRating.comment}\"",
                fontSize = 13.sp,
                color = TextTertiary,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun RatingButtonSection(onOpenRatingDialog: () -> Unit) {
    Button(
        onClick = onOpenRatingDialog,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.White
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            stringResource(R.string.rate_your_guide),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
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
                        .height(120.dp)
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
                    } catch (e: java.time.format.DateTimeParseException) {
                        Log.e("ServiceSummaryScreen", "Error parsing date", e)
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
