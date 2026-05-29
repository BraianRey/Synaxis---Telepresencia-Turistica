package com.sismptm.client.ui.features.reservation

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sismptm.client.R
import com.sismptm.client.ui.features.map.MapLocation
import com.sismptm.client.ui.features.tour.ServiceViewModel
import com.sismptm.client.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import java.time.Instant
import java.time.ZoneId
import java.time.OffsetDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReserveServiceScreen(
    location: MapLocation,
    description: String,
    onBack: () -> Unit,
    onReservationCreated: (Long) -> Unit,
    serviceViewModel: ServiceViewModel = viewModel()
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var selectedTime by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val createState by serviceViewModel.createServiceState.collectAsState()

    LaunchedEffect(createState) {
        if (createState is com.sismptm.client.ui.features.tour.CreateServiceUiState.Success) {
            val serviceId = (createState as com.sismptm.client.ui.features.tour.CreateServiceUiState.Success).serviceId
            onReservationCreated(serviceId)
            serviceViewModel.resetState()
        }
    }

    val isLoading = createState is com.sismptm.client.ui.features.tour.CreateServiceUiState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Reserve Service",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Location summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Location",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lat: ${String.format("%.4f", location.lat)}, Lon: ${String.format("%.4f", location.lon)}",
                    fontSize = 13.sp,
                    color = TextTertiary
                )
                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Description",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        color = TextTertiary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Date selection
        Text(
            text = "Select Date",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        calendar.set(year, month, dayOfMonth)
                        selectedDate = calendar.time
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    datePicker.minDate = System.currentTimeMillis()
                }.show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryAccent)
        ) {
            Text(
                text = selectedDate?.let {
                    SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(it)
                } ?: "Pick a date",
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Time selection
        Text(
            text = "Select Time",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        selectedTime = hourOfDay to minute
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        selectedDate = calendar.time
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                ).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryAccent)
        ) {
            Text(
                text = selectedTime?.let { (hour, minute) ->
                    String.format("%02d:%02d", hour, minute)
                } ?: "Pick a time",
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Summary
        if (selectedDate != null && selectedTime != null) {
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryAccent.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Reservation Summary",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateFormat.format(calendar.time),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryAccent
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (createState is com.sismptm.client.ui.features.tour.CreateServiceUiState.Error) {
            val msg = (createState as com.sismptm.client.ui.features.tour.CreateServiceUiState.Error).message
            Card(
                colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.1f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = msg,
                    color = Error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Button(
            onClick = {
                val instant = Instant.ofEpochMilli(calendar.timeInMillis)
                val zoneId = ZoneId.systemDefault()
                val offsetDateTime = instant.atZone(zoneId).toOffsetDateTime()
                val scheduledAt = offsetDateTime.toString()
                serviceViewModel.createService(location, description, scheduledAt)
            },
            enabled = selectedDate != null && selectedTime != null && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = TextPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Confirm Reservation",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
