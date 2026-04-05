package com.sismptm.client.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.Normalizer

// ── Area data ──────────────────────────────────────────────────────────────────
private data class AreaUi(val id: Long, val name: String, val country: String)

private val availableAreas = listOf(
    AreaUi(1, "Popayán",  "Colombia"),
    AreaUi(2, "Cali",     "Colombia"),
    AreaUi(3, "Medellín", "Colombia"),
    AreaUi(4, "Bogotá",   "Colombia")
)

private val filterOptions = listOf("All", "Top Rated")

private fun String.normalizeToken(): String =
    Normalizer.normalize(trim(), Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .lowercase()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerSearchScreen(
    onCancelSearch: () -> Unit = {},
    requestTourViewModel: RequestTourViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(filterOptions.first()) }
    var showRequestDialog by remember { mutableStateOf(false) }
    var selectedArea by remember { mutableStateOf<AreaUi?>(null) }

    val uiState by requestTourViewModel.uiState.collectAsState()

    // Filter areas based on search query
    val filteredAreas = remember(searchQuery) {
        val token = searchQuery.normalizeToken()
        if (token.isBlank()) emptyList()
        else availableAreas.filter {
            it.name.normalizeToken().contains(token) ||
            it.country.normalizeToken().contains(token)
        }
    }

    // Determine currently matched area (for pre-filling the dialog)
    val matchedArea = remember(searchQuery) {
        val token = searchQuery.normalizeToken()
        if (token.isBlank()) null
        else availableAreas.firstOrNull { it.name.normalizeToken().contains(token) }
    }

    // Dismiss success dialog and reset
    LaunchedEffect(uiState) {
        if (uiState is RequestTourViewModel.RequestUiState.Success) {
            showRequestDialog = false
            requestTourViewModel.resetState()
        }
    }

    if (showRequestDialog && selectedArea != null) {
        RequestTourDialog(
            area = selectedArea!!,
            uiState = uiState,
            onDismiss = {
                showRequestDialog = false
                requestTourViewModel.resetState()
            },
            onConfirm = { hours, rate, description ->
                requestTourViewModel.requestTour(
                    areaId = selectedArea!!.id,
                    agreedHours = hours,
                    hourlyRate = rate,
                    locationDescription = description
                )
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFFF6F7FB),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // ── Search row ────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("Buscar ciudad", color = Color(0xFFB0B0B0))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFFC7C7C7)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF2C2C2E),
                            unfocusedContainerColor = Color(0xFF2C2C2E),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { searchQuery = ""; onCancelSearch() }) {
                        Text("Cancel", color = Color(0xFF1E88E5))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Filter chips + Request Tour button ────────────────────────
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(filterOptions) { option ->
                        FilterChip(
                            selected = selectedFilter == option,
                            onClick = { selectedFilter = option },
                            label = { Text(option) }
                        )
                    }
                    item {
                        Button(
                            onClick = {
                                selectedArea = matchedArea ?: availableAreas.first()
                                showRequestDialog = true
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E88E5)
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("Request Tour", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                searchQuery.isBlank() -> CitySuggestionsState(
                    onCityClick = { area ->
                        searchQuery = area.name
                        selectedArea = area
                        showRequestDialog = true
                    }
                )
                filteredAreas.isEmpty() -> NoCitiesFoundState(searchQuery)
                else -> CityResultsList(
                    areas = filteredAreas,
                    onRequestTour = { area ->
                        selectedArea = area
                        showRequestDialog = true
                    }
                )
            }
        }
    }
}

// ── Empty state: show city suggestions ────────────────────────────────────────
@Composable
private fun CitySuggestionsState(onCityClick: (AreaUi) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Available destinations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2A37)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select a city to find available tour guides or request a tour.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF7C8798)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(availableAreas, key = { it.id }) { area ->
            CityCard(area = area, onClick = { onCityClick(area) })
        }
    }
}

// ── No results state ──────────────────────────────────────────────────────────
@Composable
private fun NoCitiesFoundState(query: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No encontramos resultados para \"${query.trim()}\"",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1F2A37)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ciudades disponibles: Popayán, Cali, Medellín, Bogotá.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF7C8798)
            )
        }
    }
}

// ── City results list ─────────────────────────────────────────────────────────
@Composable
private fun CityResultsList(areas: List<AreaUi>, onRequestTour: (AreaUi) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(areas, key = { it.id }) { area ->
            CityCard(area = area, onClick = { onRequestTour(area) })
        }
    }
}

// ── City card ─────────────────────────────────────────────────────────────────
@Composable
private fun CityCard(area: AreaUi, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFF1E88E5),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = area.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2A37)
                )
                Text(
                    text = area.country,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF677489)
                )
            }
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Request Tour", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ── Request Tour Dialog ────────────────────────────────────────────────────────
@Composable
private fun RequestTourDialog(
    area: AreaUi,
    uiState: RequestTourViewModel.RequestUiState,
    onDismiss: () -> Unit,
    onConfirm: (hours: Int, rate: Double, description: String) -> Unit
) {
    var hours by remember { mutableStateOf("1") }
    var rate by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val isLoading = uiState is RequestTourViewModel.RequestUiState.Loading

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "Request Tour · ${area.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Fill in the details for your tour request. A guide in ${area.name} will accept it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF677489)
                )

                OutlinedTextField(
                    value = hours,
                    onValueChange = { if (it.length <= 2) hours = it.filter { c -> c.isDigit() } },
                    label = { Text("Hours (1–8)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Hourly rate (COP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Meeting point (optional)") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState is RequestTourViewModel.RequestUiState.Error) {
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isLoading && hours.isNotBlank() && rate.isNotBlank(),
                onClick = {
                    val h = hours.toIntOrNull()?.coerceIn(1, 8) ?: 1
                    val r = rate.toDoubleOrNull() ?: 0.0
                    if (r > 0) onConfirm(h, r, description)
                }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Send Request")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!isLoading) onDismiss() }) {
                Text("Cancel")
            }
        }
    )
}
