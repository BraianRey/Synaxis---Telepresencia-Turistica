package com.sismptm.partner.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sismptm.partner.R
import com.sismptm.partner.data.remote.ServiceResponse
import com.sismptm.partner.location.LocationService
import com.sismptm.partner.ui.components.RequestCard
import com.sismptm.partner.utils.SessionManager
import kotlinx.coroutines.delay

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
        LocationService.init(context)
    }

    if (!hasLocationPermission) {
        PermissionDeniedScreen { launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
    } else {
        HomeContent(onLogout = onLogout, onNavigateToServiceReady = onNavigateToServiceReady, homeViewModel = homeViewModel)
    }
}

@Composable
fun PermissionDeniedScreen(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF12151B)).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = stringResource(R.string.gps_permission_required), style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = stringResource(R.string.gps_permission_explanation), style = MaterialTheme.typography.bodyLarge, color = Color(0xFFB9C0CB), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))) {
                Text(stringResource(R.string.grant_permissions))
            }
        }
    }
}

@Composable
fun HomeContent(
    onLogout: () -> Unit,
    onNavigateToServiceReady: (Long) -> Unit,
    homeViewModel: HomeViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    var isOnline by remember { mutableStateOf(false) }

    val requestsState by homeViewModel.requestsState.collectAsState()
    val acceptedTour by homeViewModel.acceptedTour.collectAsState()
    val acceptingServiceId by homeViewModel.acceptingServiceId.collectAsState()
    val acceptErrorMessage by homeViewModel.acceptErrorMessage.collectAsState()
    val partnerServicesState by homeViewModel.partnerServicesState.collectAsState()

    // Load requests when partner is online; poll every 10s.
    LaunchedEffect(isOnline) {
        if (isOnline) {
            homeViewModel.loadAvailableRequests()          // first load with spinner
            while (true) {
                delay(10_000)
                homeViewModel.loadAvailableRequests(silent = true)  // silent refresh
            }
        }
    }

    LaunchedEffect(acceptedTour) {
        acceptedTour?.let {
            onNavigateToServiceReady(it.serviceId)
            homeViewModel.clearAcceptedTour()
        }
    }

    LaunchedEffect(Unit) {
        homeViewModel.loadPartnerServices()
    }

    if (acceptErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { homeViewModel.clearAcceptError() },
            title = { Text(stringResource(R.string.error_accept_request)) },
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
            Button(
                onClick = { SessionManager.clearSession(); onLogout() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2430), contentColor = Color.White)
            ) {
                Text(stringResource(R.string.logout))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF12151B))
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E2430),
                contentColor = Color.White
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.tab_requests)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.tab_my_services)) }
                )
            }

            when (selectedTab) {
                0 -> RequestsTabContent(
                    isOnline = isOnline,
                    onToggleOnline = { isOnline = it },
                    requestsState = requestsState,
                    acceptingServiceId = acceptingServiceId,
                    onAccept = { homeViewModel.acceptTour(it) },
                    onRetry = { homeViewModel.loadAvailableRequests() }
                )
                1 -> MyServicesTabContent(
                    partnerServicesState = partnerServicesState,
                    onRetry = { homeViewModel.loadPartnerServices() }
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
    onRetry: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { HeaderSection(partnerName = SessionManager.partnerName.ifBlank { "Partner" }) }
        item { AvailabilityCard(isOnline = isOnline, onToggleOnline = onToggleOnline) }
        item {
            OutlinedButton(
                onClick = { LocationService.sendCurrentLocationOnce() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB))
            ) {
                Text(text = stringResource(R.string.send_location))
            }
        }
        item { StatsGrid() }

        // ── Incoming requests section ──────────────────────────────────
        if (!isOnline) {
            item { IncomingRequestsHeader(newCount = 0) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2430))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.status_offline_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.status_offline_explanation),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB9C0CB),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            when (val state = requestsState) {
                is HomeViewModel.RequestsUiState.Loading -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF2563EB))
                        }
                    }
                }
                is HomeViewModel.RequestsUiState.Success -> {
                    item { IncomingRequestsHeader(newCount = state.requests.size) }
                    if (state.requests.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.no_requests_yet),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFB9C0CB)
                            )
                        }
                    } else {
                        items(state.requests, key = { it.serviceId }) { service ->
                            ServiceRequestCard(
                                service = service,
                                isAccepting = acceptingServiceId == service.serviceId,
                                acceptEnabled = acceptingServiceId == null || acceptingServiceId == service.serviceId,
                                onAccept = { onAccept(service) }
                            )
                        }
                    }
                }
                is HomeViewModel.RequestsUiState.Error -> {
                    item {
                        Text(
                            text = state.message,
                            color = Color(0xFFEF4444),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = onRetry) {
                            Text(stringResource(R.string.retry), color = Color(0xFF2563EB))
                        }
                    }
                }
                HomeViewModel.RequestsUiState.Idle -> {
                    item { IncomingRequestsHeader(newCount = 0) }
                    item {
                        Text(
                            text = stringResource(R.string.waiting_for_requests),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB9C0CB)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MyServicesTabContent(
    partnerServicesState: HomeViewModel.PartnerServicesUiState,
    onRetry: () -> Unit
) {
    when (val state = partnerServicesState) {
        HomeViewModel.PartnerServicesUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF2563EB))
            }
        }
        is HomeViewModel.PartnerServicesUiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = state.message, color = Color(0xFFEF4444))
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.retry), color = Color(0xFF2563EB))
                }
            }
        }
        is HomeViewModel.PartnerServicesUiState.Success -> {
            val activeStatuses = setOf("ACCEPTED", "STARTED")
            val activeServices = state.services.filter { it.status.uppercase() in activeStatuses }
            val historyServices = state.services.filter { it.status.uppercase() !in activeStatuses }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.services_active),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (activeServices.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.services_no_active),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB9C0CB)
                        )
                    }
                } else {
                    items(activeServices, key = { it.serviceId }) { service ->
                        PartnerServiceCard(service)
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item {
                    Text(
                        text = stringResource(R.string.services_history),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (historyServices.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.services_no_history),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB9C0CB)
                        )
                    }
                } else {
                    items(historyServices, key = { it.serviceId }) { service ->
                        PartnerServiceCard(service)
                    }
                }
            }
        }
        HomeViewModel.PartnerServicesUiState.Idle -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.services_idle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB9C0CB)
                )
            }
        }
    }
}

@Composable
private fun PartnerServiceCard(service: ServiceResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2430))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Service #${service.serviceId}",
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            ServiceStatusBadge(service.status)
            Text(
                text = "${stringResource(R.string.label_client)}: ${service.clientName.ifBlank { "Client #${service.clientId}" }}",
                color = Color(0xFFB9C0CB)
            )
            Text(
                text = "${stringResource(R.string.label_location)}: ${service.startLocationDescription ?: "N/A"}",
                color = Color(0xFFB9C0CB)
            )
            Text(
                text = "${stringResource(R.string.label_hours)}: ${service.agreedHours}h",
                color = Color(0xFFB9C0CB)
            )
            Text(
                text = "${stringResource(R.string.label_rate)}: ${"%.0f".format(service.hourlyRate)} COP/h",
                color = Color(0xFFB9C0CB)
            )
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
        "STARTED" -> Color(0xFF4E342E) to Color(0xFFFFCC80)
        "COMPLETED" -> Color(0xFF0D47A1) to Color(0xFFBBDEFB)
        "CANCELLED" -> Color(0xFFB71C1C) to Color(0xFFFFCDD2)
        else -> Color(0xFF37474F) to Color(0xFFECEFF1)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ServiceRequestCard(
    service: ServiceResponse,
    isAccepting: Boolean,
    acceptEnabled: Boolean,
    onAccept: () -> Unit
) {
    val location = service.startLocationDescription?.ifBlank { "Location not specified" } ?: "Location not specified"
    val duration = "${service.agreedHours}h"
    val price = "${"%.0f".format(service.hourlyRate)} COP/h"
    val clientDisplayName = service.clientName.ifBlank { "Client #${service.clientId}" }

    RequestCard(
        clientName = clientDisplayName,
        location = location,
        elapsedTime = service.requestedAt?.take(10) ?: "-",
        duration = duration,
        price = price,
        onDecline = { /* TODO: decline endpoint */ },
        onAccept = onAccept,
        isAccepting = isAccepting,
        acceptEnabled = acceptEnabled
    )
}

@Composable
private fun HeaderSection(partnerName: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = buildAnnotatedString {
                pushStyle(SpanStyle(color = Color(0xFF9DA5B3), fontSize = 16.sp))
                append(stringResource(R.string.welcome_back) + "\n")
                pop()
                pushStyle(SpanStyle(color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold))
                append(partnerName)
                pop()
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Box {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF374151)), contentAlignment = Alignment.Center) {
                Text(partnerName.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Box(modifier = Modifier.align(Alignment.TopEnd).size(10.dp).clip(CircleShape).background(Color(0xFFF04438)))
        }
    }
}

@Composable
private fun AvailabilityCard(isOnline: Boolean, onToggleOnline: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2430))) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.availability_status), style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.availability_explanation), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFB9C0CB))
                }
                Switch(checked = isOnline, onCheckedChange = onToggleOnline)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (isOnline) Color(0xFF22C55E) else Color(0xFFEF4444)))
                Text(text = if (isOnline) stringResource(R.string.status_online) else stringResource(R.string.status_offline), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD1D5DB))
            }
        }
    }
}

@Composable
private fun StatsGrid() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        StatsCard(modifier = Modifier.weight(1f).aspectRatio(1f), title = stringResource(R.string.tours_today), value = "0")
        StatsCard(modifier = Modifier.weight(1f).aspectRatio(1f), title = stringResource(R.string.your_rating), value = "-")
    }
}

@Composable
private fun StatsCard(modifier: Modifier, title: String, value: String) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2430))) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color(0xFFD1D5DB))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun IncomingRequestsHeader(newCount: Int) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.incoming_requests), style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.SemiBold)
        Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF2563EB)).padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text(stringResource(R.string.new_requests_count, newCount), style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}
