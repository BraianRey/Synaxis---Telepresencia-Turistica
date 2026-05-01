package com.sismptm.client.ui.features.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.sismptm.client.R
import com.sismptm.client.ui.theme.AvailableBadgeBg
import com.sismptm.client.ui.theme.AvailableBadgeText
import com.sismptm.client.ui.theme.Background
import com.sismptm.client.ui.theme.CardBackground
import com.sismptm.client.ui.theme.DividerBorder
import com.sismptm.client.ui.theme.FilterChipActiveBg
import com.sismptm.client.ui.theme.FilterChipInactiveBg
import com.sismptm.client.ui.theme.PrimaryAccent
import com.sismptm.client.ui.theme.StarColor
import com.sismptm.client.ui.theme.TextPrimary
import com.sismptm.client.ui.theme.TextSecondary
import java.text.Normalizer

private data class PartnerUi(
    val id: String,
    val name: String,
    val city: String,
    val rating: Double,
    val reviewCount: Int,
    val tags: List<String>,
    val responseTime: String,
    val isAvailable: Boolean
)

private val demoPartners = listOf(
    PartnerUi("1", "Sofia Garcia", "Popayan, Colombia", 4.9, 127, listOf("🎥 Live tours", "🏛 Historic", "⏱ ~45 min"), "2 min", true),
    PartnerUi("2", "Miguel Torres", "Cali, Colombia", 4.7, 89, listOf("🎥 Live tours", "🏛 Historic"), "5 min", false),
    PartnerUi("3", "Brayan Meneses", "Medellin, Colombia", 4.5, 56, listOf("🎥 Live tours", "⏱ ~30 min"), "10 min", true),
    PartnerUi("4", "Samuel De Luque", "Bogota, Colombia", 4.8, 203, listOf("🎥 Live tours", "🏛 Historic", "⏱ ~60 min"), "3 min", true)
)

private val filterOptions = listOf("All", "Available now", "Top rated")

/**
 * Screen for searching and displaying available tour partners.
 * Allows users to search by city/location and request tours as a global (unassigned) action.
 *
 * @param onCancelSearch Callback triggered when user navigates back from search.
 * @param onRequestTour Callback triggered when user requests a global tour.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerSearchScreen(
    onCancelSearch: () -> Unit = {},
    onRequestTour: () -> Unit = {}
) {
    BackHandler { onCancelSearch() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(filterOptions.first()) }

    val filteredPartners = remember(searchQuery, selectedFilter) {
        val normalizedQuery = searchQuery.normalizeSearchToken()
        val base = if (normalizedQuery.isBlank()) {
            demoPartners // For demo, show all if empty
        } else {
            demoPartners.filter {
                it.name.normalizeSearchToken().contains(normalizedQuery) ||
                    it.city.normalizeSearchToken().contains(normalizedQuery)
            }
        }

        when (selectedFilter) {
            "Available now" -> base.filter { it.isAvailable }
            "Top rated" -> base.filter { it.rating >= 4.8 }
            else -> base
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Background)
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
                        placeholder = { Text("Bogotá, Colombia", color = TextSecondary, fontSize = 14.sp) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .clickable { onCancelSearch() }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        trailingIcon = {
                            Text(
                                text = stringResource(R.string.search_cancel),
                                color = PrimaryAccent,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1A1A1A),
                modifier = Modifier.height(56.dp)
            ) {
                val items = listOf("Explore", "Favorites", "Tours", "Messages", "Account")
                val icons = listOf(
                    Icons.Default.Home,
                    Icons.Default.Favorite,
                    Icons.Default.Star,
                    Icons.Default.MailOutline,
                    Icons.Default.Person
                )
                val selectedIndex = 0 // Explore is active
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = icons[index],
                                contentDescription = item,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item,
                                fontSize = 12.sp
                            )
                        },
                        selected = index == selectedIndex,
                        onClick = { /* Handle navigation */ },
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryAccent,
                            selectedTextColor = PrimaryAccent,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Filter chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filterOptions) { option ->
                        FilterChip(
                            selected = selectedFilter == option,
                            onClick = { selectedFilter = option },
                            label = {
                                Text(
                                    text = option,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(32.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = if (selectedFilter == option) FilterChipActiveBg else FilterChipInactiveBg,
                                labelColor = if (selectedFilter == option) TextPrimary else TextSecondary
                            ),
                            border = if (selectedFilter != option) androidx.compose.foundation.BorderStroke(1.dp, DividerBorder) else null
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onRequestTour,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.search_request_tour),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // Results header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.search_partners_found, filteredPartners.size),
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = stringResource(R.string.search_sort_by),
                        color = PrimaryAccent,
                        fontSize = 13.sp
                    )
                }
            }

            // Partner cards
            items(filteredPartners, key = { it.id }) { partner ->
                PartnerCard(partner = partner)
            }
        }
    }
}

private fun String.normalizeSearchToken(): String {
    return Normalizer.normalize(trim(), Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .lowercase()
}

/**
 * Individual partner card component displaying partner information.
 * Shows partner avatar, name, location, rating, response time, and tags.
 *
 * @param partner The partner UI model containing partner information.
 */
@Composable
private fun PartnerCard(partner: PartnerUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Avatar
                Box {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Gray), // Placeholder for avatar
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Partner avatar",
                            tint = TextSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    // Green dot for online
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Details
                Column(modifier = Modifier.weight(1f)) {
                    // Name
                    Text(
                        text = partner.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // City
                    Text(
                        text = partner.city,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Stars and rating
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star",
                            tint = StarColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = partner.rating.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "(${partner.reviewCount})",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    // Tags
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        partner.tags.forEach { tag ->
                            Text(
                                text = tag,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Response time
                    Text(
                        text = stringResource(R.string.search_response_time, partner.responseTime),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                // Available badge
                if (partner.isAvailable) {
                    Box(
                        modifier = Modifier
                            .background(AvailableBadgeBg, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.search_available),
                            color = AvailableBadgeText,
                            fontSize = 11.sp
                        )
                    }
                }
            }

        }
    }
}


