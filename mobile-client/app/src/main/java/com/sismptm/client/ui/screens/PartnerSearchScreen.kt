package com.sismptm.client.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.Normalizer

private data class PartnerUi(
    val id: String,
    val name: String,
    val location: String,
    val rating: Double,
    val responseTimeMin: Int,
    val isOnline: Boolean
)

private val demoPartners = listOf(
    PartnerUi("1", "Sofia Garcia", "Lima, Peru", 4.9, 8, true),
    PartnerUi("2", "Miguel Torres", "Madrid, Espana", 4.7, 12, true),
    PartnerUi("3", "Ava Thompson", "London, UK", 4.5, 15, false),
    PartnerUi("4", "Daniel Kim", "Seoul, South Korea", 4.8, 10, true)
)

private val filterOptions = listOf("All", "Top Rated")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerSearchScreen(
    onCancelSearch: () -> Unit = {},
    onRequestTour: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(filterOptions.first()) }

    val filteredPartners = remember(searchQuery, selectedFilter) {
        val normalizedQuery = searchQuery.normalizeSearchToken()
        val base = if (normalizedQuery.isBlank()) {
            emptyList()
        } else {
            demoPartners.filter {
                it.name.normalizeSearchToken().contains(normalizedQuery) ||
                    it.location.normalizeSearchToken().contains(normalizedQuery)
            }
        }

        if (selectedFilter == "Top Rated") base.filter { it.rating >= 4.8 } else base
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Buscar ciudad o partner", color = Color(0xFFB0B0B0)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search icon",
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

                    TextButton(
                        onClick = {
                            searchQuery = ""
                            onCancelSearch()
                        }
                    ) {
                        Text(text = "Cancel", color = Color(0xFF1E88E5))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filterOptions) { option ->
                        FilterChip(
                            selected = selectedFilter == option,
                            onClick = { selectedFilter = option },
                            label = { Text(option) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Ejemplo explicito del cambio de estado solicitado.
        Crossfade(
            targetState = searchQuery.isBlank(),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "partner_search_state"
        ) { isEmptySearch ->
            if (isEmptySearch) {
                EmptyPartnerState()
            } else if (filteredPartners.isEmpty()) {
                NoPartnersFoundState(searchQuery = searchQuery)
            } else {
                PartnerResultsList(partners = filteredPartners, onRequestTour = onRequestTour)
            }
        }
    }
}

private fun String.normalizeSearchToken(): String {
    return Normalizer.normalize(trim(), Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .lowercase()
}

@Composable
private fun EmptyPartnerState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location icon",
                tint = Color(0xFFBAC4D6),
                modifier = Modifier.size(76.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Descubre servicios alrededor del mundo",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1F2A37)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ingresa una ciudad en la barra superior para encontrar expertos disponibles en tu zona.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF7C8798)
            )
        }
    }
}

@Composable
private fun NoPartnersFoundState(searchQuery: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No encontramos partners para \"${searchQuery.trim()}\"",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1F2A37)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Prueba con otra ciudad o cambia el filtro para ver mas resultados.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF7C8798)
            )
        }
    }
}

@Composable
private fun PartnerResultsList(partners: List<PartnerUi>, onRequestTour: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(partners, key = { it.id }) { partner ->
            PartnerCard(partner = partner, onRequestTour = onRequestTour)
        }
    }
}

@Composable
private fun PartnerCard(partner: PartnerUi, onRequestTour: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE4EAF4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Partner avatar",
                            tint = Color(0xFF72839B),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    if (partner.isOnline) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(Color(0xFF2ECC71))
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = partner.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = partner.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF677489)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = partner.rating.toString(),
                            color = Color(0xFF374151),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Response time: ${partner.responseTimeMin} min",
                        color = Color(0xFF10B981),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Button(
                onClick = onRequestTour,
                modifier = Modifier.align(Alignment.BottomEnd),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Request Tour")
            }
        }
    }
}

