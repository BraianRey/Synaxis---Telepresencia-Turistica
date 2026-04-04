package com.sismptm.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    onNavigateToPartnerSearch: () -> Unit,
    homeViewModel: HomeViewModel = viewModel()
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Outlined.Explore, contentDescription = "Explorar") },
                    label = { Text("Explorar") },
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
                    icon = { Icon(Icons.Outlined.Person, contentDescription = "Perfil") },
                    label = { Text("Perfil") },
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
                1 -> ComingSoonTab()
                2 -> ComingSoonTab()
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
                text = "¡Hola, $userName!",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFFFFF)
            )
            Text(
                text = "¿A dónde quieres viajar hoy?",
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                text = "Buscar ciudad o destino...",
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
            focusedBorderColor = MaterialTheme.colorScheme.primary,
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
                    .padding(12.dp),
                containerColor = Color(0xFF2196F3),
                contentColor = Color(0xFFFFFFFF)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Explore,
                    contentDescription = "Solicitar recorrido",
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
                text = "$city: $guides guías",
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
            text = "DESTINOS DISPONIBLES AHORA",
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp,
            color = Color(0xFFFFFFFF)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
            // Texto superior: ciudad bold
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
            // Texto inferior: lugar + socios
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
                    text = "${destination.activePartners} socios activos",
                    fontSize = 11.sp,
                    color = Color(0xFF00CC44)
                )
            }
        }
    }
}

@Composable
private fun ComingSoonTab() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Próximamente",
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
