package com.sismptm.partner.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.sismptm.partner.R
import com.sismptm.partner.location.LocationService
import com.sismptm.partner.ui.components.RequestCard

private data class PartnerRequest(
    val id: String,
    val clientName: String,
    val location: String,
    val elapsedTime: String,
    val duration: String,
    val price: String
)

/**
 * Main screen for the partner app, handling location permissions and content display.
 * @param onLogout Callback for logout action.
 */
@Composable
fun HomeScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    if (!hasLocationPermission) {
        PermissionDeniedScreen {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    } else {
        HomeContent(onLogout = onLogout)
    }
}

/**
 * Screen displayed when location permissions are denied.
 * @param onRetry Callback to retry permission request.
 */
@Composable
fun PermissionDeniedScreen(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF12151B))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.gps_permission_required),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(id = R.string.gps_permission_explanation),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFB9C0CB),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text(stringResource(id = R.string.grant_permissions))
            }
        }
    }
}

/**
 * Main content of the Home screen, including availability toggle and requests list.
 * @param onLogout Callback for logout action.
 */
@Composable
fun HomeContent(onLogout: () -> Unit) {
    val context = LocalContext.current
    var isOnline by remember { mutableStateOf(false) }

    LaunchedEffect(isOnline) {
        if (isOnline) {
            LocationService.init(context)
            LocationService.startLocationUpdates()
        } else {
            LocationService.stopLocationUpdates()
        }
    }

    val requests = remember {
        listOf(
            PartnerRequest("r1", "Ana Gonzalez", "Centro Historico", "2 min ago", "60 min", "$30.000 COP"),
            PartnerRequest("r2", "Luis Herrera", "San Blas", "5 min ago", "90 min", "$45.000 COP"),
            PartnerRequest("r3", "Maria Torres", "Sacsayhuaman", "11 min ago", "120 min", "$70.000 COP")
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF12151B))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { HeaderSection(partnerName = "Partner Name") }
            item { AvailabilityCard(isOnline = isOnline, onToggleOnline = { isOnline = it }) }
            item { StatsGrid() }
            item { IncomingRequestsHeader(newCount = requests.size) }

            if (requests.isEmpty()) {
                item {
                    Text(
                        text = stringResource(id = R.string.no_requests_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB9C0CB)
                    )
                }
            } else {
                items(requests, key = { it.id }) { request ->
                    RequestCard(
                        clientName = request.clientName,
                        location = request.location,
                        elapsedTime = request.elapsedTime,
                        duration = request.duration,
                        price = request.price,
                        onDecline = { },
                        onAccept = { }
                    )
                }
            }
        }

        Button(
            onClick = onLogout,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E2430),
                contentColor = Color.White
            )
        ) {
            Text(text = stringResource(id = R.string.logout))
        }
    }
}

/**
 * Header section with partner name and profile icon.
 * @param partnerName Name of the logged-in partner.
 */
@Composable
private fun HeaderSection(partnerName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = buildAnnotatedString {
                pushStyle(SpanStyle(color = Color(0xFF9DA5B3), fontSize = 16.sp))
                append(stringResource(id = R.string.welcome_back) + "\n")
                pop()
                pushStyle(SpanStyle(color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold))
                append(partnerName)
                pop()
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Box {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF374151)),
                contentAlignment = Alignment.Center
            ) {
                Text("PN", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF04438))
            )
        }
    }
}

/**
 * Card to toggle the partner's availability status.
 * @param isOnline Current availability status.
 * @param onToggleOnline Callback when status is toggled.
 */
@Composable
private fun AvailabilityCard(isOnline: Boolean, onToggleOnline: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2430))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(id = R.string.availability_status), style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(id = R.string.availability_explanation), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFB9C0CB))
                }
                Switch(checked = isOnline, onCheckedChange = onToggleOnline)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) Color(0xFF22C55E) else Color(0xFFEF4444))
                )
                Text(
                    text = if (isOnline) stringResource(id = R.string.status_online) else stringResource(id = R.string.status_offline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD1D5DB)
                )
            }
        }
    }
}

/**
 * Grid displaying partner statistics.
 */
@Composable
private fun StatsGrid() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        StatsCard(modifier = Modifier.weight(1f).aspectRatio(1f), title = stringResource(id = R.string.tours_today), value = "4")
        StatsCard(modifier = Modifier.weight(1f).aspectRatio(1f), title = stringResource(id = R.string.your_rating), value = "4.9")
    }
}

/**
 * Card displaying a single statistic.
 * @param modifier Layout modifier.
 * @param title Title of the stat.
 * @param value Value of the stat.
 */
@Composable
private fun StatsCard(modifier: Modifier, title: String, value: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2430))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color(0xFFD1D5DB))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Header for the incoming requests section.
 * @param newCount Number of new requests.
 */
@Composable
private fun IncomingRequestsHeader(newCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.incoming_requests),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2563EB))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(stringResource(id = R.string.new_requests_count, newCount), style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}
