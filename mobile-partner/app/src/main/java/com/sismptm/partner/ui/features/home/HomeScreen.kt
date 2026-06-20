package com.sismptm.partner.ui.features.home

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import com.sismptm.partner.ui.features.profile.ProfileUploadViewModel
import com.sismptm.partner.ui.features.profile.ProfileViewModel
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.sismptm.partner.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sismptm.partner.R
import com.sismptm.partner.core.session.SessionManager
import com.sismptm.partner.core.network.NetworkConfig
import com.sismptm.partner.data.remote.api.dto.ServiceResponse
import com.sismptm.partner.manager.location.LocationManager
import com.sismptm.partner.ui.common.RequestCard
import kotlinx.coroutines.delay
import java.time.Instant

private val ProfileInfoCardPadding = 16.dp
private val ProfileInfoCardCornerRadius = 12.dp
private val ProfileInfoCardBackground = Color(0xFF1C2533)
private val ProfileInfoIconBackground = Color(0xFF2563EB).copy(alpha = 0.2f)
private val ProfileInfoIconTint = Color(0xFF2563EB)

/**
 * Main dashboard for the partner. Manages online availability, incoming tour requests,
 * and tracks performance statistics.
 */
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNavigateToServiceReady: (Long) -> Unit,
    homeViewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            launcher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
        LocationManager.init(context)
    }

    if (!hasLocationPermission) {
        HomePermissionDeniedScreen { launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
    } else {
        HomeContent(onLogout = onLogout, onNavigateToServiceReady = onNavigateToServiceReady, homeViewModel = homeViewModel)
    }
}

@Composable
private fun HomePermissionDeniedScreen(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Background).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = stringResource(R.string.gps_permission_required), style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(text = stringResource(R.string.gps_permission_explanation), style = MaterialTheme.typography.bodyLarge, color = TextTertiary, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)) {
                Text(stringResource(R.string.grant_permissions))
            }
        }
    }
}

@Composable
private fun HomeContent(
    onLogout: () -> Unit,
    onNavigateToServiceReady: (Long) -> Unit,
    homeViewModel: HomeViewModel
) {
    var selectedTab by remember { mutableStateOf(0) }
    var isOnline by remember { mutableStateOf(false) }

    val profileViewModel: ProfileViewModel = viewModel()
    val profileBitmap by profileViewModel.profilePictureBitmap.collectAsStateWithLifecycle()

    val requestsState by homeViewModel.requestsState.collectAsState()
    val acceptedTour by homeViewModel.acceptedTour.collectAsState()
    val acceptingServiceId by homeViewModel.acceptingServiceId.collectAsState()
    val acceptErrorMessage by homeViewModel.acceptErrorMessage.collectAsState()
    val partnerServicesState by homeViewModel.partnerServicesState.collectAsState()
    val averageRating by homeViewModel.averageRating.collectAsState()
    val ratingCount by homeViewModel.ratingCount.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(partnerServicesState) {
        val state = partnerServicesState
        if (state is HomeViewModel.PartnerServicesUiState.Success) {
            val services = state.services
            services.forEach { service ->
                if (service.status.uppercase() == "WAITING_FOR_START" && !service.scheduledAt.isNullOrBlank()) {
                    com.sismptm.partner.manager.notification.AlarmScheduler.scheduleServiceAlarm(
                        context,
                        service.serviceId,
                        service.scheduledAt
                    )
                }
            }
        }
    }

    LaunchedEffect(isOnline) {
        if (isOnline) {
            homeViewModel.loadAvailableRequests()
            while (true) {
                delay(10_000)
                homeViewModel.loadAvailableRequests(silent = true)
            }
        }
    }

    LaunchedEffect(acceptedTour) {
        acceptedTour?.let {
            // Only redirect to prep screen if it's explicitly marked as NOT scheduled (false)
            // If scheduled is null or true, treat as scheduled service and don't redirect
            if (it.scheduled == false) {
                onNavigateToServiceReady(it.serviceId)
            }
            homeViewModel.clearAcceptedTour()
        }
    }

    LaunchedEffect(Unit) {
        homeViewModel.loadPartnerServices()
        homeViewModel.loadPartnerRatings()
        // Poll partner services silently to detect status changes without flickering
        while (true) {
            delay(5_000)
            homeViewModel.loadPartnerServices(silent = true)
        }
    }

    if (acceptErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { homeViewModel.clearAcceptError() },
            title = { Text(stringResource(R.string.accept_request_failed)) },
            text = { Text(acceptErrorMessage!!) },
            confirmButton = {
                TextButton(onClick = { homeViewModel.clearAcceptError() }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).background(Background)) {
            if (selectedTab != 3) {
                HeaderSection(
                    partnerName = SessionManager.partnerName.ifBlank { stringResource(R.string.default_partner_name) },
                    profileBitmap = profileBitmap,
                    onProfileClick = { selectedTab = 3 },
                    modifier = Modifier.padding(16.dp)
                )
            }

            when (selectedTab) {
                0 -> RequestsTabContent(
                    isOnline = isOnline,
                    onToggleOnline = { isOnline = it },
                    requestsState = requestsState,
                    acceptingServiceId = acceptingServiceId,
                    onAccept = { homeViewModel.acceptTour(it) },
                    averageRating = averageRating,
                    ratingCount = ratingCount
                )
                1 -> MyServicesTabContent(partnerServicesState = partnerServicesState, onNavigateToServiceReady = onNavigateToServiceReady)
                2 -> UpcomingTabContent(partnerServicesState = partnerServicesState, onNavigateToServiceReady = onNavigateToServiceReady, homeViewModel = homeViewModel)
                3 -> ProfileTab(
                    onLogout = onLogout,
                    partnerServicesState = partnerServicesState,
                    averageRating = averageRating,
                    ratingCount = ratingCount
                )
            }
        }
    }
}

@Composable
private fun RequestsTabContent(
    isOnline: Boolean,
    onToggleOnline: (Boolean) -> Unit,
    requestsState: HomeViewModel.RequestsUiState,
    acceptingServiceId: Long?,
    onAccept: (ServiceResponse) -> Unit,
    averageRating: String,
    ratingCount: Int
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

        item { AvailabilityCard(isOnline = isOnline, onToggleOnline = onToggleOnline) }
        item {
            OutlinedButton(
                onClick = { LocationManager.requestSingleUpdate() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryAccent)
            ) {
                Text(text = stringResource(R.string.send_location))
            }
        }
        item { StatsGrid(averageRating = averageRating, ratingCount = ratingCount) }

        item { IncomingRequestsHeader(newCount = if (requestsState is HomeViewModel.RequestsUiState.Success) requestsState.requests.size else 0) }

        if (!isOnline) {
            item { OfflineStatusCard() }
        } else {
            when (requestsState) {
                is HomeViewModel.RequestsUiState.Loading -> item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryAccent)
                    }
                }
                is HomeViewModel.RequestsUiState.Success -> {
                    if (requestsState.requests.isEmpty()) {
                        item { Text(stringResource(R.string.no_requests_yet), color = TextTertiary) }
                    } else {
                        items(requestsState.requests, key = { it.serviceId }) { service ->
                            val location = service.startLocationDescription?.ifBlank { stringResource(R.string.not_specified) } ?: stringResource(R.string.not_specified)
                            val duration = service.agreedHours?.let { "${it}${stringResource(R.string.hours_unit).first()}" } ?: stringResource(R.string.rate_na)
                            val price = service.hourlyRate?.let { "$${"%.0f".format(it)}${stringResource(R.string.rate_unit_suffix)}" } ?: stringResource(R.string.rate_na)
                            val clientName = service.clientName?.ifBlank { stringResource(R.string.unknown_client) } ?: stringResource(R.string.unknown_client)
                            val elapsedLabel = if (service.scheduled == true && !service.scheduledAt.isNullOrBlank()) {
                                try {
                                    val instant = Instant.parse(service.scheduledAt)
                                    val zoned = instant.atZone(java.time.ZoneId.systemDefault())
                                    val fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm").withLocale(java.util.Locale.getDefault())
                                    "At ${fmt.format(zoned)}"
                                } catch (e: Exception) {
                                    "Reserved"
                                }
                            } else {
                                stringResource(R.string.status_new)
                            }
                            Log.d("RequestDebug", "Service ${service.serviceId}: scheduled=${service.scheduled}, scheduledAt=${service.scheduledAt}, clientName=$clientName")

                            RequestCard(
                                clientName = clientName,
                                location = location,
                                elapsedTime = elapsedLabel,
                                duration = duration,
                                price = price,
                                clientPicDirectory = service.clientPicDirectory,
                                onDecline = {},
                                onAccept = { onAccept(service) },
                                isAccepting = acceptingServiceId == service.serviceId
                            )
                        }
                    }
                }
                is HomeViewModel.RequestsUiState.Error -> item { Text(requestsState.message, color = Error) }
                else -> {}
            }
        }
    }
}

@Composable
private fun MyServicesTabContent(partnerServicesState: HomeViewModel.PartnerServicesUiState, onNavigateToServiceReady: (Long) -> Unit) {
    when (partnerServicesState) {
        HomeViewModel.PartnerServicesUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryAccent) }
        is HomeViewModel.PartnerServicesUiState.Success -> {
            val activeStatuses = setOf("ACCEPTED", "WAITING_FOR_START", "READY", "STARTED", "IN_PROGRESS")
            val activeServices = partnerServicesState.services.filter { it.status.uppercase() in activeStatuses }
            val historyServices = partnerServicesState.services.filter { it.status.uppercase() !in activeStatuses }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (activeServices.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.services_active)) }
                    items(activeServices, key = { it.serviceId }) { service -> ServiceHistoryCard(service, onNavigateToServiceReady) }
                }

                if (historyServices.isNotEmpty()) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    item { SectionHeader(stringResource(R.string.services_history)) }
                    items(historyServices, key = { it.serviceId }) { service -> ServiceHistoryCard(service, null) }
                }

                if (activeServices.isEmpty() && historyServices.isEmpty()) {
                    item { Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_requests_yet), color = Color.Gray) } }
                }
            }
        }
        is HomeViewModel.PartnerServicesUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(partnerServicesState.message, color = Error) }
        else -> {}
    }
}

@Composable
private fun UpcomingTabContent(
    partnerServicesState: HomeViewModel.PartnerServicesUiState, 
    onNavigateToServiceReady: (Long) -> Unit,
    homeViewModel: HomeViewModel
) {
    when (partnerServicesState) {
        HomeViewModel.PartnerServicesUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryAccent) }
        is HomeViewModel.PartnerServicesUiState.Success -> {
            val activeStatuses = setOf("ACCEPTED", "WAITING_FOR_START", "READY", "STARTED", "IN_PROGRESS")
            val upcomingServices = partnerServicesState.services.filter {
                it.status.uppercase() in activeStatuses && !it.scheduledAt.isNullOrBlank()
            }.sortedWith(compareByDescending<ServiceResponse> { service ->
                val isActive = service.status.uppercase() in setOf("READY", "STARTED", "IN_PROGRESS")
                var isTimeToStart = false
                if (!service.scheduledAt.isNullOrBlank()) {
                    try {
                        val instant = Instant.parse(service.scheduledAt)
                        isTimeToStart = instant.isBefore(Instant.now()) || instant.equals(Instant.now())
                    } catch (e: Exception) {}
                }
                isActive || isTimeToStart
            }.thenBy { it.scheduledAt ?: "ZZZZ" })

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (upcomingServices.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.upcoming_reservations)) }
                    items(upcomingServices, key = { it.serviceId }) { service -> UpcomingServiceCard(service, onNavigateToServiceReady) }
                } else {
                    item { Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_upcoming_reservations), color = Color.Gray) } }
                }
            }

            LaunchedEffect(upcomingServices) {
                while (true) {
                    delay(10000)
                    val hasWaitingServices = upcomingServices.any { 
                        it.status.uppercase() == "WAITING_FOR_START" 
                    }
                    if (hasWaitingServices) {
                        homeViewModel.loadPartnerServices(silent = true)
                    }
                }
            }
        }
        is HomeViewModel.PartnerServicesUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(partnerServicesState.message, color = Error) }
        else -> {}
    }
}

@Composable
private fun UpcomingServiceCard(service: ServiceResponse, onNavigateToServiceReady: (Long) -> Unit) {
    var isTimeToStart by remember { mutableStateOf(false) }

    val scheduledText = service.scheduledAt?.let {
        try {
            val instant = Instant.parse(it)
            isTimeToStart = instant.isBefore(Instant.now()) || instant.equals(Instant.now())
            val zoned = instant.atZone(java.time.ZoneId.systemDefault())
            val fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm").withLocale(java.util.Locale.getDefault())
            fmt.format(zoned)
        } catch (e: Exception) {
            it
        }
    } ?: stringResource(R.string.status_new)

    val statusDisplay = when (service.status.uppercase()) {
        "WAITING_FOR_START" -> stringResource(R.string.starts_at, scheduledText)
        "READY" -> stringResource(R.string.time_arrived)
        "ACCEPTED" -> stringResource(R.string.ready_to_start_label)
        "IN_PROGRESS" -> stringResource(R.string.in_progress)
        else -> service.status
    }

    val backgroundColor = when (service.status.uppercase()) {
        "WAITING_FOR_START" -> if (isTimeToStart) Color(0xFF2E3B2E) else Color(0xFF3B3B2E)
        "READY", "STARTED", "IN_PROGRESS" -> Color(0xFF2E3B2E)
        else -> CardBackground
    }

    val isActive = service.status.uppercase() in setOf("READY", "STARTED", "IN_PROGRESS") || (service.status.uppercase() == "WAITING_FOR_START" && isTimeToStart)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToServiceReady(service.serviceId) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = if (isActive) BorderStroke(2.dp, PrimaryAccent) else null
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val badgeStatus = if (service.status.uppercase() == "WAITING_FOR_START" && isTimeToStart) "READY" else service.status
                Text("${stringResource(R.string.service_prefix)}${service.serviceId}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                ServiceStatusBadge(badgeStatus)
            }
            Text(service.startLocationDescription?.ifBlank { stringResource(R.string.not_specified) } ?: stringResource(R.string.not_specified),
                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text("${stringResource(R.string.label_client)}: ${service.clientName?.ifBlank { stringResource(R.string.unknown_client) } ?: stringResource(R.string.unknown_client)}",
                color = Color(0xFFCCCCCC), fontSize = 11.sp, maxLines = 1)
            
            val finalStatusDisplay = if (service.status.uppercase() == "WAITING_FOR_START" && isTimeToStart) stringResource(R.string.time_arrived) else statusDisplay
            Text(finalStatusDisplay, color = if (isActive) Success else if (service.status.uppercase() == "WAITING_FOR_START") Color(0xFFF39C12) else PrimaryAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
private fun ServiceHistoryCard(service: ServiceResponse, onNavigateToServiceReady: ((Long) -> Unit)? = null) {
    val isActive = service.status.uppercase() in setOf("WAITING_FOR_START", "READY", "STARTED", "IN_PROGRESS")
    val isWaitingToStart = service.status.uppercase() == "WAITING_FOR_START"
    
    // Determine border color based on status
    val borderColor = when {
        isWaitingToStart -> Color(0xFFFFB74D)  // Orange warning for WAITING_FOR_START
        isActive && onNavigateToServiceReady != null -> PrimaryAccent  // Normal accent for active
        else -> Color.Transparent
    }
    val borderWidth = if (isActive && onNavigateToServiceReady != null) 2.dp else 0.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onNavigateToServiceReady != null) {
                if (onNavigateToServiceReady != null) {
                    onNavigateToServiceReady(service.serviceId)
                }
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            // Background image
            service.locationReferenceImageUrl?.let { imageUrl ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .addHeader("User-Agent", "TourPresence/1.0 (Android; academic project)")
                        .crossfade(true)
                        .build(),
                    contentDescription = service.startLocationDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = android.R.drawable.ic_dialog_alert),
                    placeholder = painterResource(id = android.R.drawable.ic_menu_gallery)
                )
            } ?: run {
                // Fallback gradient if no image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CardBackground)
                )
            }

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC000000)),
                            startY = 60f
                        )
                    )
            )

            // Content overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${stringResource(R.string.service_prefix)}${service.serviceId}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    ServiceStatusBadge(service.status)
                }
                Text(service.startLocationDescription?.ifBlank { stringResource(R.string.not_specified) } ?: stringResource(R.string.not_specified),
                    color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${stringResource(R.string.label_client)}: ${service.clientName?.ifBlank { stringResource(R.string.unknown_client) } ?: stringResource(R.string.unknown_client)}",
                        color = Color(0xFFCCCCCC), fontSize = 11.sp, maxLines = 1)
                    
                    if (isActive && onNavigateToServiceReady != null) {
                        Button(
                            onClick = { onNavigateToServiceReady(service.serviceId) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(stringResource(R.string.btn_reenter_service), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceStatusBadge(status: String) {
    val normalized = status.uppercase()
    val label = if (normalized == "REQUESTED") "CREATED" else normalized
    val (bg, fg) = when (normalized) {
        "REQUESTED" -> Color(0xFF263238) to Color(0xFF90CAF9)
        "ACCEPTED" -> Color(0xFF1B5E20) to Color(0xFFA5D6A7)
        "WAITING_FOR_START" -> Color(0xFFF9A825) to Color(0xFFFFFDE7)
        "READY" -> Color(0xFF4CAF50) to Color(0xFFE8F5E9)
        "STARTED", "IN_PROGRESS" -> Color(0xFF4E342E) to Color(0xFFFFCC80)
        "COMPLETED" -> Color(0xFF0D47A1) to Color(0xFFBBDEFB)
        "CANCELLED" -> Color(0xFFB71C1C) to Color(0xFFFFCDD2)
        else -> Color(0xFF37474F) to Color(0xFFECEFF1)
    }
    Surface(color = bg, shape = RoundedCornerShape(999.dp)) {
        Text(text = label, color = fg, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

@Composable
private fun HeaderSection(partnerName: String, profileBitmap: Bitmap? = null, onProfileClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = buildAnnotatedString {
                pushStyle(SpanStyle(color = TextTertiary, fontSize = 16.sp))
                append(stringResource(R.string.welcome_back) + "\n")
                pop()
                pushStyle(SpanStyle(color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold))
                append(partnerName)
                pop()
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(PrimaryAccent)
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            if (profileBitmap != null) {
                Image(
                    bitmap = profileBitmap.asImageBitmap(),
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(partnerName.take(1).uppercase(), color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AvailabilityCard(isOnline: Boolean, onToggleOnline: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBackground)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.availability_status), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(stringResource(R.string.availability_explanation), style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
                }
                Switch(checked = isOnline, onCheckedChange = onToggleOnline)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (isOnline) Success else Error))
                Text(text = if (isOnline) stringResource(R.string.status_online) else stringResource(R.string.status_offline), color = TextSecondary)
            }
        }
    }
}

@Composable
private fun StatsGrid(averageRating: String, ratingCount: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        StatsCard(modifier = Modifier.weight(1f).aspectRatio(1f), title = stringResource(R.string.tours_today), value = "0")
        StatsCard(
            modifier = Modifier.weight(1f).aspectRatio(1f),
            title = stringResource(R.string.your_rating),
            value = averageRating,
            subtitle = if (ratingCount > 0) "$ratingCount reviews" else null
        )
    }
}

@Composable
private fun StatsCard(modifier: Modifier, title: String, value: String, subtitle: String? = null) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBackground)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            Column {
                Text(value, style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                }
            }
        }
    }
}

@Composable
private fun IncomingRequestsHeader(newCount: Int) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.incoming_requests), style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(PrimaryAccent).padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text(stringResource(R.string.new_requests_count, newCount), color = TextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun OfflineStatusCard() {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.status_offline_title), color = Error, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.status_offline_explanation), textAlign = TextAlign.Center, color = TextTertiary)
        }
    }
}

@Composable
private fun BottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF1E1E1E),
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = if (selectedTab == 0) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                    contentDescription = stringResource(R.string.tab_requests)
                )
            },
            label = { Text(stringResource(R.string.tab_requests)) },
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryAccent,
                selectedTextColor = PrimaryAccent,
                unselectedIconColor = TextTertiary,
                unselectedTextColor = TextTertiary,
                indicatorColor = Color.Transparent
            )
        )

        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = if (selectedTab == 1) Icons.Filled.Assignment else Icons.Outlined.Assignment,
                    contentDescription = stringResource(R.string.tab_my_services)
                )
            },
            label = { Text(stringResource(R.string.tab_my_services)) },
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryAccent,
                selectedTextColor = PrimaryAccent,
                unselectedIconColor = TextTertiary,
                unselectedTextColor = TextTertiary,
                indicatorColor = Color.Transparent
            )
        )

        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = if (selectedTab == 2) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                    contentDescription = "Upcoming"
                )
            },
            label = { Text("Upcoming") },
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryAccent,
                selectedTextColor = PrimaryAccent,
                unselectedIconColor = TextTertiary,
                unselectedTextColor = TextTertiary,
                indicatorColor = Color.Transparent
            )
        )

        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = if (selectedTab == 3) Icons.Filled.Person else Icons.Outlined.Person,
                    contentDescription = stringResource(R.string.profile)
                )
            },
            label = { Text(stringResource(R.string.profile)) },
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryAccent,
                selectedTextColor = PrimaryAccent,
                unselectedIconColor = TextTertiary,
                unselectedTextColor = TextTertiary,
                indicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun ProfileTab(
    onLogout: () -> Unit,
    partnerServicesState: HomeViewModel.PartnerServicesUiState,
    averageRating: String,
    ratingCount: Int
) {
    val profileViewModel: ProfileViewModel = viewModel()
    val uploadViewModel: ProfileUploadViewModel = viewModel()
    val uiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val profileBitmap by profileViewModel.profilePictureBitmap.collectAsStateWithLifecycle()
    val profilePictureDirectory by profileViewModel.profilePictureDirectory.collectAsStateWithLifecycle()
    val uploadState by uploadViewModel.photoUploadState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showPhotoOptions by remember { mutableStateOf(false) }
    val name = uiState.name.ifBlank { SessionManager.partnerName.ifBlank { stringResource(R.string.default_partner_name) } }
    val email = uiState.email.ifBlank { SessionManager.partnerEmail.ifBlank { stringResource(R.string.not_specified) } }
    val role = uiState.role.ifBlank { SessionManager.partnerRole.ifBlank { stringResource(R.string.not_specified) } }
    val status = uiState.status.ifBlank { stringResource(R.string.not_specified) }
    val language = uiState.language.ifBlank { SessionManager.language }
    var isEditing by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf(language) }

    LaunchedEffect(uiState.language) {
        selectedLanguage = uiState.language.ifBlank { SessionManager.language }
    }

    val onUpdatePhoto: (android.net.Uri) -> Unit = { uri ->
        uploadViewModel.updateProfilePicture(context, uri)
    }
    val onTakePhoto: (Bitmap) -> Unit = { bitmap ->
        uploadViewModel.updateProfilePicture(context, bitmap)
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let(onUpdatePhoto)
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let(onTakePhoto)
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            cameraLauncher.launch(null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.my_profile),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        )

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryAccent)
            }
        }

        uiState.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = error, color = Error, style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = { profileViewModel.loadProfile() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                    ) {
                        Text(stringResource(R.string.retry), color = TextPrimary)
                    }
                }
            }
        }

        val currentUploadState = uploadState
        if (currentUploadState is ProfileUploadViewModel.ProfilePhotoUploadState.Error) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = currentUploadState.message,
                        color = Color(0xFFB71C1C),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { showPhotoOptions = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                    ) {
                        Text(stringResource(R.string.retry), color = TextPrimary)
                    }
                }
            }
        }

        AvatarSection(
            initial = name.take(1).uppercase(),
            profileBitmap = profileBitmap,
            profilePictureDirectory = profilePictureDirectory,
            isUploading = uploadState is ProfileUploadViewModel.ProfilePhotoUploadState.Loading,
            onGallerySelected = {
                showPhotoOptions = false
                imagePickerLauncher.launch("image/*")
            },
            onCameraSelected = {
                showPhotoOptions = false
                if (hasCameraPermission) {
                    cameraLauncher.launch(null)
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onPhotoButtonClicked = { showPhotoOptions = true }
        )

        Spacer(modifier = Modifier.height(24.dp))
        InfoCardsSection(
            name = name,
            email = email,
            role = role,
            status = status,
            rating = averageRating,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (isEditing) {
            EditModeSection(
                selectedLanguage = selectedLanguage,
                onLanguageSelected = { selectedLanguage = it },
                onCancel = {
                    selectedLanguage = SessionManager.language
                    isEditing = false
                },
                onSave = {
                    SessionManager.updateLanguage(selectedLanguage)
                    isEditing = false
                }
            )
        } else {
            ViewModeSection(onEditClick = { isEditing = true }, language = language)
        }

        Spacer(modifier = Modifier.height(24.dp))
        LogoutButton(onLogout, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AvatarSection(
    initial: String,
    profileBitmap: Bitmap?,
    profilePictureDirectory: String?,
    isUploading: Boolean,
    onGallerySelected: () -> Unit,
    onCameraSelected: () -> Unit,
    onPhotoButtonClicked: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(Color(0xFF2563EB).copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = {
                    expanded = true
                    onPhotoButtonClicked()
                }),
            contentAlignment = Alignment.Center
        ) {
            if (profileBitmap != null) {
                Image(
                    bitmap = profileBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.change_profile_photo),
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else if (profilePictureDirectory != null) {
                val imageUrl = NetworkConfig.BASE_URL + profilePictureDirectory
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(300)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .networkCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = stringResource(R.string.change_profile_photo),
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = initial,
                    color = Color(0xFF2563EB),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = stringResource(R.string.change_profile_photo),
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (isUploading) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.photo_uploading),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.choose_from_gallery), color = Color.White) },
                onClick = {
                    expanded = false
                    onGallerySelected()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.take_photo), color = Color.White) },
                onClick = {
                    expanded = false
                    onCameraSelected()
                }
            )
        }
    }
}

@Composable
private fun InfoCardsSection(name: String, email: String, role: String, status: String, rating: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        ProfileInfoCard(icon = Icons.Default.Person, label = stringResource(R.string.full_name), value = name)
        Spacer(modifier = Modifier.height(12.dp))
        ProfileInfoCard(icon = Icons.Default.Email, label = stringResource(R.string.email), value = email)
        Spacer(modifier = Modifier.height(12.dp))
        ProfileInfoCard(icon = Icons.Default.Work, label = stringResource(R.string.role), value = role)
        Spacer(modifier = Modifier.height(12.dp))
        ProfileInfoCard(icon = Icons.Default.Info, label = stringResource(R.string.status), value = status)
        Spacer(modifier = Modifier.height(12.dp))
        ProfileInfoCard(icon = Icons.Default.Star, label = stringResource(R.string.your_rating), value = rating)
    }
}

@Composable
private fun EditModeSection(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    EditableLanguageCard(
        selectedLanguage = selectedLanguage,
        onLanguageSelected = onLanguageSelected
    )

    Spacer(modifier = Modifier.height(32.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            border = BorderStroke(1.dp, Color(0xFF9CA3AF))
        ) {
            Text(stringResource(R.string.cancel), color = Color.White)
        }

        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
        ) {
            Text(stringResource(R.string.save_changes), color = Color.White)
        }
    }
}

@Composable
private fun ViewModeSection(onEditClick: () -> Unit, language: String) {
    ProfileInfoCard(
        icon = Icons.Default.Settings,
        label = stringResource(R.string.preferred_language),
        value = language.uppercase()
    )

    Spacer(modifier = Modifier.height(32.dp))
    Button(
        onClick = onEditClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
    ) {
        Text(stringResource(R.string.edit), color = Color.White)
    }
}

@Composable
private fun LogoutButton(onLogout: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = {
            SessionManager.clearSession()
            onLogout()
        },
        border = BorderStroke(1.dp, Color(0xFF9CA3AF)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .widthIn(min = 160.dp)
            .height(48.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.logout),
            color = Color.White
        )
    }
}

@Composable
private fun ProfileInfoCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ProfileInfoCardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = ProfileInfoCardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ProfileInfoCardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ProfileInfoIconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ProfileInfoIconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF9CA3AF))
                Text(text = value, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun EditableLanguageCard(selectedLanguage: String, onLanguageSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf(
        "en" to stringResource(R.string.language_english),
        "es" to stringResource(R.string.language_spanish)
    )
    val selectedLabel = languages.find { it.first == selectedLanguage }?.second ?: selectedLanguage

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ProfileInfoCardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = ProfileInfoCardBackground)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(ProfileInfoCardPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ProfileInfoIconBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = ProfileInfoIconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.preferred_language),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF)
                    )
                    Text(
                        text = selectedLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                languages.forEach { (code, label) ->
                    DropdownMenuItem(
                        text = { Text(label, color = Color.White) },
                        onClick = {
                            onLanguageSelected(code)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
