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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.sismptm.client.core.network.NetworkConfig
import com.sismptm.client.core.session.SessionManager
import com.sismptm.client.ui.theme.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sismptm.client.R
import com.sismptm.client.data.remote.api.dto.ServiceResponse
import com.sismptm.client.domain.model.HomeUiState
import com.sismptm.client.ui.features.tour.ServiceViewModel
import com.sismptm.client.ui.features.profile.ProfileScreen
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
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

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
                    onNavigateToPartnerSearch = onNavigateToPartnerSearch,
                    onNavigateToMapService = onNavigateToMapService,
                    onNavigateToReserveMap = onNavigateToReserveMap,
                    onAvatarClick = { selectedTab = 2 }
                )
                1 -> ToursTabContent(
                    servicesState = servicesState,
                    onRefresh = { homeViewModel.loadClientServices() },
                    onOpenWaiting = onOpenServiceWaiting
                )
                2 -> ProfileScreen(
                    onLogout,
                    picDirectory = uiState.picDirectory,
                    onUpdatePhoto = { uri -> homeViewModel.updateProfilePicture(context, uri) }
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
    @Suppress("UnusedParameter")
    onNavigateToPartnerSearch: () -> Unit,
    onNavigateToMapService: () -> Unit,
    onNavigateToReserveMap: () -> Unit,
    onAvatarClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        HomeHeader(uiState.userName, uiState.picDirectory, onAvatarClick)
        SearchBar()
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onNavigateToMapService,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 20.dp),
            border = BorderStroke(1.dp, PrimaryAccent),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = stringResource(R.string.request_service),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryAccent
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onNavigateToReserveMap,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = stringResource(R.string.reserve_service_btn),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
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
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun HomeHeader(userName: String, picDirectory: String? = null, onAvatarClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background)
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(R.string.home_greeting, userName),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = stringResource(R.string.home_subtitle),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(CardBackground)
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            if (picDirectory != null) {
                val imageUrl = NetworkConfig.BASE_URL + picDirectory
                android.util.Log.d("HomeDebug", "Loading profile image from: $imageUrl")
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(300)
                        .build(),
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Avatar",
                    tint = TextTertiary,
                    modifier = Modifier.size(28.dp)
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
            .height(48.dp),
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
        shape = RoundedCornerShape(28.dp),
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.home_services_active),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        if (activeServices.isEmpty()) {
            Text(text = stringResource(R.string.home_services_no_active), color = TextSecondary)
        } else {
            activeServices.forEach { service ->
                ClientServiceCard(service = service, onOpenWaiting = onOpenWaiting)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.home_services_history),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        if (historyServices.isEmpty()) {
            Text(text = stringResource(R.string.home_services_no_history), color = TextSecondary)
        } else {
            historyServices.forEach { service ->
                ClientServiceCard(service = service, onOpenWaiting = onOpenWaiting)
            }
        }
    }
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
    val shouldHighlight = isTimeToStart || isReady

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (shouldHighlight) CardBackgroundLight else CardBackgroundDark),
        shape = RoundedCornerShape(14.dp),
        border = if (shouldHighlight) BorderStroke(2.dp, PrimaryAccent) else null
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
            Text(dateText, color = Color.White, fontWeight = FontWeight.SemiBold)
            
            // LOGIC: Show READY if time arrived but server still in WAITING_FOR_START
            val displayStatus = if (service.status.uppercase() == "WAITING_FOR_START" && isTimeToStart) "READY / TIME ARRIVED" else service.status
            ServiceStatusBadge(status = displayStatus)

            if (!service.scheduledAt.isNullOrBlank()) {
                val scheduledText = try {
                    val instant = java.time.Instant.parse(service.scheduledAt)
                    val zoned = instant.atZone(java.time.ZoneId.systemDefault())
                    val fmt = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withLocale(java.util.Locale.getDefault())
                    "Scheduled: ${fmt.format(zoned)}"
                } catch (e: Exception) {
                    "Scheduled"
                }
                Text(scheduledText, color = PurpleAccent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
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
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = android.R.drawable.ic_dialog_alert),
                    placeholder = painterResource(id = android.R.drawable.ic_menu_gallery)
                )
            }
            Text("${stringResource(R.string.location_prefix)}${service.startLocationDescription ?: stringResource(R.string.not_specified)}", color = TextTertiary)
            Text(stringResource(R.string.hours_prefix) + service.agreedHours, color = TextTertiary)
            Text(stringResource(R.string.hourly_rate_prefix) + stringResource(R.string.currency_format, service.hourlyRate ?: 0.0), color = TextTertiary)
            if (isActive) {
                Button(
                    onClick = { onOpenWaiting(service.serviceId) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                ) {
                    Text(stringResource(R.string.open_waiting_screen))
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
private fun ProfileTab(onLogout: () -> Unit, picDirectory: String? = null) {
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
                if (picDirectory != null) {
                    val imageUrl = NetworkConfig.BASE_URL + picDirectory
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(300)
                            .build(),
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
        Text(
            text = stringResource(R.string.active_services),
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp,
            color = TextPrimary
        )
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
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
                            colors = listOf(Color.Transparent, Color(0xCC000000)),
                            startY = 80f
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
                    .padding(10.dp)
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
        containerColor = Background,
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
                indicatorColor = Color.Transparent
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
                indicatorColor = Color.Transparent
            )
        )
    }
}
