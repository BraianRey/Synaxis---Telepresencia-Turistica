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
import java.math.BigDecimal

/** Area ID → name mapping */
private val AREA_NAMES = mapOf(1L to "Popayán", 2L to "Cali", 3L to "Medellín", 4L to "Bogotá")

@Composable
fun HomeScreen(onLogout: () -> Unit, homeViewModel: HomeViewModel = viewModel()) {
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
        HomeContent(onLogout = onLogout, homeViewModel = homeViewModel)
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
fun HomeContent(onLogout: () -> Unit, homeViewModel: HomeViewModel = viewModel()) {
    var isOnline by remember { mutableStateOf(false) }
    var showAreaDialog by remember { mutableStateOf(SessionManager.areaId == 0L) }

    val requestsState by homeViewModel.requestsState.collectAsState()
    val acceptedTour by homeViewModel.acceptedTour.collectAsState()
    val acceptingServiceId by homeViewModel.acceptingServiceId.collectAsState()
    val acceptErrorMessage by homeViewModel.acceptErrorMessage.collectAsState()

    // Load requests when areaId becomes set AND partner is online; poll every 10s.
    LaunchedEffect(SessionManager.areaId, isOnline) {
        if (SessionManager.areaId != 0L && isOnline) {
            homeViewModel.loadAvailableRequests()          // first load with spinner
            while (true) {
                delay(10_000)
                homeViewModel.loadAvailableRequests(silent = true)  // silent refresh
            }
        }
    }

    if (showAreaDialog) {
        AreaSelectorDialog(
            onAreaSelected = { areaId ->
                SessionManager.areaId = areaId
                showAreaDialog = false
                homeViewModel.loadAvailableRequests()
            }
        )
    }

    if (acceptedTour != null) {
        AcceptedTourDialog(
            service = acceptedTour!!,
            onDismiss = { homeViewModel.clearAcceptedTour() }
        )
    }

    if (acceptErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { homeViewModel.clearAcceptError() },
            title = { Text("Could not accept request") },
            text = { Text(acceptErrorMessage!!) },
            confirmButton = {
                TextButton(onClick = { homeViewModel.clearAcceptError() }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF12151B))) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { HeaderSection(partnerName = SessionManager.partnerName.ifBlank { "Partner" }) }
            item { AvailabilityCard(isOnline = isOnline, onToggleOnline = { isOnline = it }) }
            item {
                OutlinedButton(
                    onClick = { LocationService.sendCurrentLocationOnce() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB))
                ) { Text(text = stringResource(R.string.send_location)) }
            }
            item { StatsGrid() }

            // ── Incoming requests section ──────────────────────────────────
            if (!isOnline) {
                // Partner is OFFLINE → show message, hide requests
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
                                text = "You are currently offline",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Toggle your availability status to start receiving tour requests.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFB9C0CB),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
            // Partner is ONLINE → show live requests
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
                                onAccept = { homeViewModel.acceptTour(service) }
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
                        TextButton(onClick = { homeViewModel.loadAvailableRequests() }) {
                            Text("Retry", color = Color(0xFF2563EB))
                        }
                    }
                }
                HomeViewModel.RequestsUiState.Idle -> {
                    item { IncomingRequestsHeader(newCount = 0) }
                    item {
                        Text(
                            text = "Select your area to see incoming requests.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB9C0CB)
                        )
                        TextButton(onClick = { showAreaDialog = true }) {
                            Text("Change area", color = Color(0xFF2563EB))
                        }
                    }
                }
            }
            } // close else (isOnline)
        }

        Button(
            onClick = { SessionManager.clearSession(); onLogout() },
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2430), contentColor = Color.White)
        ) { Text(text = stringResource(R.string.logout)) }
    }
}

/** Dialog for the partner to select their working area. */
@Composable
private fun AreaSelectorDialog(onAreaSelected: (Long) -> Unit) {
    val areas = listOf(1L to "Popayán", 2L to "Cali", 3L to "Medellín", 4L to "Bogotá")

    AlertDialog(
        onDismissRequest = { /* Required */ },
        title = { Text("Select your area", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Choose the city where you offer tours.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF677489)
                )
                areas.forEach { (id, name) ->
                    OutlinedButton(
                        onClick = { onAreaSelected(id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(name) }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun ServiceRequestCard(
    service: ServiceResponse,
    isAccepting: Boolean,
    acceptEnabled: Boolean,
    onAccept: () -> Unit
) {
    val areaName = AREA_NAMES[service.areaId] ?: "Area ${service.areaId}"
    val location = service.startLocationDescription?.ifBlank { areaName } ?: areaName
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
private fun AcceptedTourDialog(service: ServiceResponse, onDismiss: () -> Unit) {
    val areaName = AREA_NAMES[service.areaId] ?: "Area ${service.areaId}"
    val meetingPoint = service.startLocationDescription?.ifBlank { "-" } ?: "-"
    val requestedAt = service.requestedAt?.replace("T", " ") ?: "-"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.tour_accepted_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.tour_accepted_message))
                Spacer(modifier = Modifier.height(6.dp))
                Text("${stringResource(R.string.tour_detail_service_id)}: ${service.serviceId}")
                Text("${stringResource(R.string.tour_detail_client)}: #${service.clientId}")
                Text("${stringResource(R.string.tour_detail_area)}: $areaName")
                Text("${stringResource(R.string.tour_detail_meeting_point)}: $meetingPoint")
                Text("${stringResource(R.string.tour_detail_duration)}: ${service.agreedHours}h")
                Text("${stringResource(R.string.tour_detail_hourly_rate)}: ${"%.0f".format(service.hourlyRate)} COP/h")
                Text("${stringResource(R.string.tour_detail_requested_at)}: $requestedAt")
                Text("${stringResource(R.string.tour_detail_status)}: ACCEPTED")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
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