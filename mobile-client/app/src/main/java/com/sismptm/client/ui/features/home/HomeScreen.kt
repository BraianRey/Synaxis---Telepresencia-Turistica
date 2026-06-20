package com.sismptm.client.ui.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

import com.sismptm.client.core.session.SessionManager
import com.sismptm.client.ui.theme.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sismptm.client.R
import com.sismptm.client.data.remote.api.dto.ServiceResponse
import com.sismptm.client.domain.model.HomeUiState
import com.sismptm.client.ui.features.tour.ServiceViewModel
import com.sismptm.client.ui.features.profile.ProfileScreen
import com.sismptm.client.ui.features.profile.ProfileViewModel
import android.graphics.Bitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.time.Instant

@Composable
fun HomeScreen(
    onNavigateToPartnerSearch: () -> Unit,
    onOpenServiceWaiting: (Long) -> Unit,
    onNavigateToMapService: () -> Unit,
    onNavigateToReserveMap: () -> Unit,
    onLogout: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
    @Suppress("UnusedParameter")
    serviceViewModel: ServiceViewModel = viewModel()
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val servicesState by homeViewModel.servicesState.collectAsStateWithLifecycle()
    val activeServicesState by homeViewModel.activeServicesState.collectAsStateWithLifecycle()
    val photoUploadState by homeViewModel.photoUploadState.collectAsStateWithLifecycle()
    val profileViewModel: ProfileViewModel = viewModel()
    val profileBitmap by profileViewModel.profilePictureBitmap.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(selectedTab) {
        // Start polling when Explore or Tours tab is active; stop otherwise.
        if (selectedTab == 0 || selectedTab == 1) {
            homeViewModel.startPollingServices()
        } else {
            homeViewModel.stopPollingServices()
        }
    }

    LaunchedEffect(servicesState) {
        val state = servicesState
        if (state is HomeViewModel.ClientServicesUiState.Success) {
            state.services.forEach { service ->
                if (service.status.uppercase() in setOf("ACCEPTED", "WAITING_FOR_START") && !service.scheduledAt.isNullOrBlank()) {
                    com.sismptm.client.manager.notification.AlarmScheduler.scheduleServiceAlarm(
                        context,
                        service.serviceId,
                        service.scheduledAt
                    )
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
        ) {
            when (selectedTab) {
                0 -> ExploreTabContent(
                    uiState = uiState,
                    servicesState = servicesState,
                    activeServicesState = activeServicesState,
                    profileBitmap = profileBitmap,
                    onNavigateToPartnerSearch = onNavigateToPartnerSearch,
                    onNavigateToMapService = onNavigateToMapService,
                    onNavigateToReserveMap = onNavigateToReserveMap,
                    onOpenWaiting = onOpenServiceWaiting,
                    onAvatarClick = { selectedTab = 2 }
                )
                1 -> ToursTabContent(
                    servicesState = servicesState,
                    onRefresh = { homeViewModel.loadClientServices() },
                    onOpenWaiting = onOpenServiceWaiting
                )
                2 -> ProfileScreen(
                    onLogout,
                    onUpdatePhoto = { uri -> homeViewModel.updateProfilePicture(context, uri) },
                    onTakePhoto = { bitmap -> homeViewModel.updateProfilePicture(context, bitmap) },
                    photoUploadState = photoUploadState,
                    profileViewModel = profileViewModel
                )
            }
        }
    }
}

@Composable
private fun ExploreTabContent(
    uiState: HomeUiState,
    servicesState: HomeViewModel.ClientServicesUiState,
    activeServicesState: HomeViewModel.ClientServicesUiState,
    profileBitmap: Bitmap?,
    @Suppress("UnusedParameter")
    onNavigateToPartnerSearch: () -> Unit,
    onNavigateToMapService: () -> Unit,
    onNavigateToReserveMap: () -> Unit,
    onOpenWaiting: (Long) -> Unit,
    onAvatarClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 90.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        HomeHeader(uiState.userName, profileBitmap, onAvatarClick)
        Spacer(modifier = Modifier.height(18.dp))
        SearchBar()
        Spacer(modifier = Modifier.height(20.dp))
        HomeActionsCard(
            onNavigateToMapService = onNavigateToMapService,
            onNavigateToReserveMap = onNavigateToReserveMap
        )
        Spacer(modifier = Modifier.height(26.dp))

        val activeWithImage = if (activeServicesState is HomeViewModel.ClientServicesUiState.Success) {
            activeServicesState.services
                .filter { it.locationReferenceImageUrl != null }
                .take(5)
        } else emptyList()

        if (activeWithImage.isNotEmpty()) {
            RecentPlacesSection(
                services = activeWithImage,
                onServiceClick = onNavigateToMapService
            )
            Spacer(modifier = Modifier.height(28.dp))
        }

        ClientServicesSections(
            activeServices = if (servicesState is HomeViewModel.ClientServicesUiState.Success) {
                servicesState.services.filter { it.status.uppercase() in setOf("REQUESTED", "ACCEPTED", "WAITING_FOR_START", "READY", "STARTED", "IN_PROGRESS") }
            } else emptyList(),
            historyServices = if (servicesState is HomeViewModel.ClientServicesUiState.Success) {
                servicesState.services.filter { it.status.uppercase() !in setOf("REQUESTED", "ACCEPTED", "WAITING_FOR_START", "READY", "STARTED", "IN_PROGRESS") }
            } else emptyList(),
            onOpenWaiting = onOpenWaiting
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun HomeActionsCard(
    onNavigateToMapService: () -> Unit,
    onNavigateToReserveMap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateToMapService,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, PrimaryAccent),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryAccent, containerColor = CardBackground)
            ) {
                Text(
                    text = stringResource(R.string.request_service),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onNavigateToReserveMap,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.reserve_service_btn),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(userName: String, profileBitmap: Bitmap? = null, onAvatarClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = buildAnnotatedString {
                pushStyle(SpanStyle(color = TextTertiary, fontSize = 16.sp))
                append(stringResource(R.string.welcome_back))
                append("\n")
                pop()
                pushStyle(SpanStyle(color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold))
                append(userName)
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
                .clickable { onAvatarClick() },
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
                Text(
                    text = userName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SearchBar() {
    OutlinedTextField(
        value = "",
        onValueChange = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(52.dp),
        placeholder = {
            Text(
                text = stringResource(R.string.home_search_hint),
                color = TextTertiary,
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = TextTertiary
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Outlined.Mic,
                contentDescription = "Microphone",
                tint = TextTertiary
            )
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = InputBorderFocused,
            unfocusedContainerColor = CardBackground,
            focusedContainerColor = InputBackgroundFocused,
            unfocusedTextColor = InputTextActive,
            focusedTextColor = TextPrimary
        )
    )
}

@Composable
private fun ToursTabContent(
    servicesState: HomeViewModel.ClientServicesUiState,
    onRefresh: () -> Unit,
    onOpenWaiting: (Long) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.my_services),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Button(
                    onClick = { onRefresh() },
                    modifier = Modifier
                        .width(100.dp)
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Refresh", fontSize = 12.sp)
                }
            }

            when (servicesState) {
                HomeViewModel.ClientServicesUiState.Idle,
                HomeViewModel.ClientServicesUiState.Loading -> {
                    CircularProgressIndicator(color = PrimaryAccent)
                    Text(stringResource(R.string.loading_services), color = TextSecondary)
                }
                is HomeViewModel.ClientServicesUiState.Error -> {
                    Text(text = servicesState.message, color = ErrorLight)
                }
                is HomeViewModel.ClientServicesUiState.Success -> {
                    if (servicesState.services.isEmpty()) {
                        Text(text = stringResource(R.string.no_service_requests_yet), color = TextSecondary)
                    } else {
                        val activeStatuses = setOf("REQUESTED", "ACCEPTED", "WAITING_FOR_START", "READY", "STARTED", "IN_PROGRESS")
                        val activeServices = servicesState.services.filter { it.status.uppercase() in activeStatuses }
                            .sortedWith(compareByDescending<ServiceResponse> { service ->
                                val isReady = service.status.uppercase() in setOf("READY", "STARTED", "IN_PROGRESS")
                                var isTimeToStart = false
                                if (!service.scheduledAt.isNullOrBlank()) {
                                    try {
                                        val instant = Instant.parse(service.scheduledAt)
                                        isTimeToStart = instant.isBefore(Instant.now()) || instant.equals(Instant.now())
                                    } catch (e: Exception) {}
                                }
                                isReady || isTimeToStart
                            }.thenBy { it.scheduledAt ?: "ZZZZ" })
                        val historyServices = servicesState.services.filter { it.status.uppercase() !in activeStatuses }

                        ClientServicesSections(
                            activeServices = activeServices,
                            historyServices = historyServices,
                            onOpenWaiting = onOpenWaiting
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientServicesSections(
    activeServices: List<ServiceResponse>,
    historyServices: List<ServiceResponse>,
    onOpenWaiting: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SectionHeader(text = stringResource(R.string.home_services_active))
        if (activeServices.isEmpty()) {
            Text(text = stringResource(R.string.home_services_no_active), color = TextTertiary, modifier = Modifier.padding(horizontal = 20.dp))
        } else {
            activeServices.forEach { service ->
                ClientServiceCard(service = service, onOpenWaiting = onOpenWaiting)
            }
        }

        SectionHeader(text = stringResource(R.string.home_services_history))
        if (historyServices.isEmpty()) {
            Text(text = stringResource(R.string.home_services_no_history), color = TextTertiary, modifier = Modifier.padding(horizontal = 20.dp))
        } else {
            historyServices.forEach { service ->
                ClientServiceCard(service = service, onOpenWaiting = onOpenWaiting)
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
    )
}

@Composable
private fun ClientServiceCard(
    service: ServiceResponse,
    onOpenWaiting: (Long) -> Unit
) {
    val isActive = service.status.uppercase() in setOf("REQUESTED", "ACCEPTED", "WAITING_FOR_START", "READY", "STARTED", "IN_PROGRESS")
    var isTimeToStart by remember { mutableStateOf(false) }
    if (!service.scheduledAt.isNullOrBlank()) {
        try {
            val instant = Instant.parse(service.scheduledAt)
            isTimeToStart = instant.isBefore(Instant.now()) || instant.equals(Instant.now())
        } catch (e: Exception) {}
    }

    val isReady = service.status.uppercase() in setOf("READY", "STARTED", "IN_PROGRESS")
    val shouldHighlight = isTimeToStart || isReady || service.status.uppercase() == "ACCEPTED"

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundDark),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = if (shouldHighlight) BorderStroke(2.dp, PrimaryAccent) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val dateText = run {
                val iso = service.endedAt ?: service.startedAt
                if (iso == null) {
                    "${stringResource(R.string.service_prefix)}${service.serviceId}"
                } else {
                    try {
                        val instant = java.time.Instant.parse(iso)
                        val zoned = instant.atZone(java.time.ZoneId.systemDefault())
                        val fmt = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy").withLocale(java.util.Locale.getDefault())
                        fmt.format(zoned)
                    } catch (e: Exception) {
                        "${stringResource(R.string.service_prefix)}${service.serviceId}"
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(dateText, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                val displayStatus = if (service.status.uppercase() == "WAITING_FOR_START" && isTimeToStart) "READY" else service.status
                ServiceStatusBadge(status = displayStatus)
            }

            Text(
                text = service.startLocationDescription ?: stringResource(R.string.not_specified),
                color = TextSecondary,
                fontSize = 13.sp,
                maxLines = 1
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.hours_prefix) + service.agreedHours, color = TextTertiary, fontSize = 12.sp)
                Text(text = stringResource(R.string.hourly_rate_prefix) + stringResource(R.string.currency_format, service.hourlyRate ?: 0.0), color = TextTertiary, fontSize = 12.sp)
            }

            if (!service.scheduledAt.isNullOrBlank()) {
                val scheduledText = try {
                    val instant = java.time.Instant.parse(service.scheduledAt)
                    val zoned = instant.atZone(java.time.ZoneId.systemDefault())
                    val fmt = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withLocale(java.util.Locale.getDefault())
                    "Scheduled: ${fmt.format(zoned)}"
                } catch (e: Exception) {
                    "Scheduled"
                }
                Text(scheduledText, color = TextTertiary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
            }

            service.locationReferenceImageUrl?.let { imageUrl ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .addHeader("User-Agent", "TourPresence/1.0 (Android; academic project)")
                        .crossfade(400)
                        .build(),
                    contentDescription = "Service location image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = android.R.drawable.ic_dialog_alert),
                    placeholder = painterResource(id = android.R.drawable.ic_menu_gallery)
                )
            }

            if (isActive) {
                Button(
                    onClick = { onOpenWaiting(service.serviceId) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.open_waiting_screen), fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ServiceStatusBadge(status: String) {
    val normalized = status.uppercase()
    val label = if (normalized == "REQUESTED") "CREATED" else normalized
    val (bg, fg) = when {
        normalized.contains("READY") || normalized.contains("ARRIVED") -> Color(0xFF1B5E20) to Color(0xFFA5D6A7)
        normalized == "REQUESTED" -> Color(0xFF263238) to Color(0xFF90CAF9)
        normalized == "ACCEPTED" -> Color(0xFF1B5E20) to Color(0xFFA5D6A7)
        normalized == "WAITING_FOR_START" -> Color(0xFFF9A825) to Color(0xFFFFFDE7)
        normalized == "READY" -> Success to SuccessBackground
        normalized == "STARTED" || normalized == "IN_PROGRESS" -> Color(0xFF4E342E) to Color(0xFFFFCC80)
        normalized == "COMPLETED" -> Color(0xFF0D47A1) to Color(0xFFBBDEFB)
        normalized == "CANCELLED" -> Color(0xFFB71C1C) to Color(0xFFFFCDD2)
        else -> Color(0xFF37474F) to Color(0xFFECEFF1)
    }
    Surface(color = bg, shape = RoundedCornerShape(999.dp)) {
        Text(
            text = label,
            color = fg,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ProfileTab(onLogout: () -> Unit, profileBitmap: Bitmap? = null) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(CardBackground),
                contentAlignment = Alignment.Center
            ) {
                if (profileBitmap != null) {
                    Image(
                        bitmap = profileBitmap.asImageBitmap(),
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.home_profile),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(32.dp))
            OutlinedButton(
                onClick = {
                    SessionManager.clearSession()
                    onLogout()
                },
                border = BorderStroke(1.dp, TextTertiary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.home_sign_out),
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
private fun RecentPlacesSection(
    services: List<ServiceResponse>,
    onServiceClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        SectionHeader(text = stringResource(R.string.active_services))
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(services) { service ->
                RecentPlaceCard(service = service, onClick = onServiceClick)
            }
        }
    }
}

@Composable
private fun RecentPlaceCard(
    service: ServiceResponse,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(200.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundDark)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(service.locationReferenceImageUrl)
                    .addHeader("User-Agent", "TourPresence/1.0 (Android; academic project)")
                    .crossfade(400)
                    .build(),
                contentDescription = service.startLocationDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xB3000000)),
                            startY = 90f
                        )
                    )
            )
            Text(
                text = service.startLocationDescription ?: "",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
        }
    }
}

/**
 * Bottom navigation bar with 3 tabs: Explore, Tours, and Profile.
 */
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
                    imageVector = if (selectedTab == 0) Icons.Filled.Explore else Icons.Outlined.Explore,
                    contentDescription = stringResource(R.string.nav_explore)
                )
            },
            label = { Text(stringResource(R.string.nav_explore)) },
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
                    imageVector = if (selectedTab == 1) Icons.Filled.ViewList else Icons.Outlined.ViewList,
                    contentDescription = stringResource(R.string.nav_tours)
                )
            },
            label = { Text(stringResource(R.string.nav_tours)) },
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryAccent,
                selectedTextColor = PrimaryAccent,
                unselectedIconColor = TextTertiary,
                unselectedTextColor = TextTertiary,
                indicatorColor = PrimaryAccent.copy(alpha = 0.18f)
            )
        )

        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = if (selectedTab == 2) Icons.Filled.Person else Icons.Outlined.Person,
                    contentDescription = stringResource(R.string.nav_profile)
                )
            },
            label = { Text(stringResource(R.string.nav_profile)) },
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryAccent,
                selectedTextColor = PrimaryAccent,
                unselectedIconColor = TextTertiary,
                unselectedTextColor = TextTertiary,
                indicatorColor = PrimaryAccent.copy(alpha = 0.18f)
            )
        )
    }
}
