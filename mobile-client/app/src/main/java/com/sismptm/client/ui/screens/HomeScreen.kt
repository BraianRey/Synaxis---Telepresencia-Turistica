package com.sismptm.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sismptm.client.data.remote.ServiceResponse
import com.sismptm.client.data.remote.TokenManager
import com.sismptm.client.R
@Composable
fun HomeScreen(
    onNavigateToPartnerSearch: () -> Unit,
    onOpenServiceWaiting: (Long) -> Unit,
    onLogout: () -> Unit,
    homeViewModel: HomeViewModel = viewModel()
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val servicesState by homeViewModel.servicesState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color(0xFF1A1A1A),
                tonalElevation = 0.dp,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Outlined.Explore, contentDescription = "Explore") },
                    label = { Text(stringResource(R.string.home_explore)) },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                        selectedIconColor = Color(0xFFFFFFFF),
                        unselectedIconColor = Color(0xFF666666),
                        selectedTextColor = Color(0xFFFFFFFF),
                        unselectedTextColor = Color(0xFF666666)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Outlined.ConfirmationNumber, contentDescription = "Tours") },
                    label = { Text("Tours") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                        selectedIconColor = Color(0xFFFFFFFF),
                        unselectedIconColor = Color(0xFF666666),
                        selectedTextColor = Color(0xFFFFFFFF),
                        unselectedTextColor = Color(0xFF666666)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Outlined.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                        selectedIconColor = Color(0xFFFFFFFF),
                        unselectedIconColor = Color(0xFF666666),
                        selectedTextColor = Color(0xFFFFFFFF),
                        unselectedTextColor = Color(0xFF666666)
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF1A1A1A))
        ) {
            when (selectedTab) {
                0 -> ExploreTabContent(uiState, onNavigateToPartnerSearch)
                1 -> ToursTabContent(
                    servicesState = servicesState,
                    onRefresh = { homeViewModel.loadClientServices() },
                    onOpenWaiting = onOpenServiceWaiting
                )
                2 -> ProfileTab(onLogout)
            }
        }
    }
}

@Composable
private fun ExploreTabContent(
    uiState: HomeUiState,
    onNavigateToPartnerSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        HomeHeader(uiState.userName)

        // Search Bar
        SearchBar()

        Spacer(modifier = Modifier.height(16.dp))

        // Map Placeholder
        MapPlaceholder(uiState.mapPins, onNavigateToPartnerSearch)

        Spacer(modifier = Modifier.height(24.dp))

        // Destinations Section
        DestinationsSection(uiState.destinations)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun HomeHeader(userName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(R.string.home_greeting, userName),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFFFFF)
            )
            Text(
                text = stringResource(R.string.home_subtitle),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFCCCCCC)
            )
        }

        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF2C2C2C)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "Avatar",
                tint = Color(0xFF666666),
                modifier = Modifier.size(28.dp)
            )
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
                color = Color(0xFF888888),
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = Color(0xFF888888)
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Outlined.Mic,
                contentDescription = "Microphone",
                tint = Color(0xFF888888)
            )
        },
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color(0xFF444444),
            unfocusedContainerColor = Color(0xFF2C2C2C),
            focusedContainerColor = Color(0xFF333333),
            unfocusedTextColor = Color(0xFFDDDDDD),
            focusedTextColor = Color(0xFFFFFFFF)
        )
    )
}

@Composable
private fun MapPlaceholder(mapPins: List<MapPin>, onNavigateToPartnerSearch: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE8DCC8))
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val width = maxWidth
            val height = maxHeight
            val lineColor = Color(0xFFD4C9B0)

            Divider(
                color = lineColor,
                thickness = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .offset(y = height * 0.30f)
            )
            Divider(
                color = lineColor,
                thickness = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .offset(y = height * 0.60f)
            )
            Divider(
                color = lineColor,
                thickness = 1.dp,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .align(Alignment.TopStart)
                    .offset(x = width * 0.45f)
            )
            Divider(
                color = lineColor,
                thickness = 1.dp,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .align(Alignment.TopStart)
                    .offset(x = width * 0.70f)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = "Your location",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(32.dp)
                )
            }

            mapPins.forEachIndexed { index, pin ->
                val pinModifier = when (index) {
                    0 -> Modifier
                        .align(Alignment.TopStart)
                        .offset(x = width * 0.15f, y = height * 0.20f)
                    1 -> Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = -(width * 0.15f), y = height * 0.25f)
                    2 -> Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = -(width * 0.12f), y = -(height * 0.10f))
                    else -> Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = width * 0.12f, y = -(height * 0.08f))
                }
                PinIndicator(pin.city, pin.activeGuides, modifier = pinModifier)
            }

            SmallFloatingActionButton(
                onClick = onNavigateToPartnerSearch,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 48.dp),
                containerColor = Color(0xFF2196F3),
                contentColor = Color(0xFFFFFFFF)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Explore,
                    contentDescription = "Request tour",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PinIndicator(city: String, guides: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x4400CC44))
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A1A))
            )
            Icon(
                imageVector = Icons.Outlined.Videocam,
                contentDescription = city,
                tint = Color(0xFF00CC44),
                modifier = Modifier.size(16.dp)
            )
        }
        Card(
            modifier = Modifier
                .padding(top = 4.dp)
                .wrapContentSize(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xCC000000)
            )
        ) {
            Text(
                text = stringResource(R.string.home_city_guides, city, guides),
                fontSize = 10.sp,
                color = Color.White,
                modifier = Modifier.padding(6.dp)
            )
        }
    }
}

@Composable
private fun DestinationsSection(destinations: List<Destination>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = stringResource(R.string.home_destinations_title),
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp,
            color = Color(0xFFFFFFFF)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(destinations) { destination ->
                DestinationCard(destination)
            }
        }
    }
}

@Composable
private fun DestinationCard(destination: Destination) {
    val gradientColors = when (destination.id % 4) {
        0 -> listOf(Color(0xFF1B4332), Color(0xFF0D1F17))
        1 -> listOf(Color(0xFF1A237E), Color(0xFF0D1129))
        2 -> listOf(Color(0xFF4A148C), Color(0xFF1A0533))
        3 -> listOf(Color(0xFF7B3F00), Color(0xFF2D1700))
        else -> listOf(Color(0xFF1B4332), Color(0xFF0D1F17))
    }

    Card(
        modifier = Modifier
            .width(160.dp)
            .height(200.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(gradientColors))
        ) {
            // Top text: city bold
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = destination.city,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFFFFF)
                )
                Text(
                    text = destination.country,
                    fontSize = 12.sp,
                    color = Color(0xFFAAAAAA)
                )
            }
            // Bottom text: place + partners
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = destination.placeName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFFFFF)
                )
                Text(
                    text = stringResource(R.string.home_active_partners, destination.activePartners),
                    fontSize = 11.sp,
                    color = Color(0xFF00CC44)
                )
            }
        }
    }
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
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "My services",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text("Refresh", color = Color.White)
            }

            when (servicesState) {
                HomeViewModel.ClientServicesUiState.Idle,
                HomeViewModel.ClientServicesUiState.Loading -> {
                    CircularProgressIndicator(color = Color(0xFF2196F3))
                    Text("Loading services...", color = Color(0xFFCCCCCC))
                }

                is HomeViewModel.ClientServicesUiState.Error -> {
                    Text(
                        text = servicesState.message,
                        color = Color(0xFFFF8A80)
                    )
                }

                is HomeViewModel.ClientServicesUiState.Success -> {
                    if (servicesState.services.isEmpty()) {
                        Text(
                            text = "No service requests yet.",
                            color = Color(0xFFCCCCCC)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            servicesState.services.forEach { service ->
                                ClientServiceCard(service = service, onOpenWaiting = onOpenWaiting)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientServiceCard(
    service: ServiceResponse,
    onOpenWaiting: (Long) -> Unit
) {
    val isActive = service.status.uppercase() in setOf("REQUESTED", "ACCEPTED", "STARTED")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2430)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Service #${service.serviceId}", color = Color.White, fontWeight = FontWeight.SemiBold)
            ServiceStatusBadge(status = service.status)
            Text("Area ID: ${service.areaId}", color = Color(0xFFB9C0CB))
            Text("Hours: ${service.agreedHours}", color = Color(0xFFB9C0CB))
            Text("Hourly rate: ${service.hourlyRate} COP", color = Color(0xFFB9C0CB))
            if (isActive) {
                Button(
                    onClick = { onOpenWaiting(service.serviceId) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Open waiting screen")
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
private fun ProfileTab(onLogout: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = Color(0xFF666666),
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.home_profile),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF)
            )
            Spacer(Modifier.height(32.dp))
            OutlinedButton(
                onClick = {
                    TokenManager.clearSession()
                    onLogout()
                },
                border = BorderStroke(1.dp, Color(0xFF666666)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = Color(0xFFAAAAAA),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.home_sign_out),
                    color = Color(0xFFAAAAAA)
                )
            }
        }
    }
}
