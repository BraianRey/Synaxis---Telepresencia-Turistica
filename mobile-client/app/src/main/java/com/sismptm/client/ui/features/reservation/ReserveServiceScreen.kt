package com.sismptm.client.ui.features.reservation

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.sismptm.client.ui.theme.Background
import com.sismptm.client.ui.theme.CardBackground
import com.sismptm.client.ui.theme.Error
import com.sismptm.client.ui.theme.PrimaryAccent
import com.sismptm.client.ui.theme.TextPrimary
import com.sismptm.client.ui.theme.TextSecondary
import com.sismptm.client.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    ReserveServiceCreateEffect(
        createState = createState,
        serviceViewModel = serviceViewModel,
        onReservationCreated = onReservationCreated
    )

    val isLoading = createState is com.sismptm.client.ui.features.tour.CreateServiceUiState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        ReserveServiceHeader(onBack = onBack)

        Spacer(modifier = Modifier.height(24.dp))

        ReserveServiceLocationSummary(location = location, description = description)

        Spacer(modifier = Modifier.height(32.dp))

        ReserveServiceDatePickerSection(
            selectedDate = selectedDate,
            onOpenDatePicker = {
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
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        ReserveServiceTimePickerSection(
            selectedTime = selectedTime,
            onOpenTimePicker = {
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
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        ReserveServiceSummaryCard(
            selectedDate = selectedDate,
            selectedTime = selectedTime,
            calendar = calendar
        )

        Spacer(modifier = Modifier.weight(1f))

        ReserveServiceErrorCard(createState = createState)

        ReserveServiceConfirmButton(
            isLoading = isLoading,
            enabled = selectedDate != null && selectedTime != null && !isLoading,
            onConfirm = {
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
                isoFormat.timeZone = java.util.TimeZone.getDefault()
                val scheduledAt = isoFormat.format(calendar.time)
                serviceViewModel.createService(location, description, scheduledAt)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ReserveServiceCreateEffect(
    createState: com.sismptm.client.ui.features.tour.CreateServiceUiState,
    serviceViewModel: ServiceViewModel,
    onReservationCreated: (Long) -> Unit
) {
    LaunchedEffect(createState) {
        if (createState is com.sismptm.client.ui.features.tour.CreateServiceUiState.Success) {
            val serviceId = createState.serviceId
            onReservationCreated(serviceId)
            serviceViewModel.resetState()
        }
    }
}

@Composable
private fun ReserveServiceHeader(onBack: () -> Unit) {
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
}

@Composable
private fun ReserveServiceLocationSummary(location: MapLocation, description: String) {
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
                text = "Lat: ${String.format(Locale.US, "%.4f", location.lat)}, " +
                    "Lon: ${String.format(Locale.US, "%.4f", location.lon)}",
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
}

@Composable
private fun ReserveServiceDatePickerSection(
    selectedDate: Date?,
    onOpenDatePicker: () -> Unit
) {
    Text(
        text = "Select Date",
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(
        onClick = onOpenDatePicker,
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
}

@Composable
private fun ReserveServiceTimePickerSection(
    selectedTime: Pair<Int, Int>?,
    onOpenTimePicker: () -> Unit
) {
    Text(
        text = "Select Time",
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(
        onClick = onOpenTimePicker,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryAccent)
    ) {
        Text(
            text = selectedTime?.let { (hour, minute) ->
                String.format(Locale.US, "%02d:%02d", hour, minute)
            } ?: "Pick a time",
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ReserveServiceSummaryCard(
    selectedDate: Date?,
    selectedTime: Pair<Int, Int>?,
    calendar: Calendar
) {
    if (selectedDate == null || selectedTime == null) return

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

@Composable
private fun ReserveServiceErrorCard(createState: com.sismptm.client.ui.features.tour.CreateServiceUiState) {
    if (createState !is com.sismptm.client.ui.features.tour.CreateServiceUiState.Error) return

    Card(
        colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Text(
            text = createState.message,
            color = Error,
            fontSize = 13.sp,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun ReserveServiceConfirmButton(
    isLoading: Boolean,
    enabled: Boolean,
    onConfirm: () -> Unit
) {
    Button(
        onClick = onConfirm,
        enabled = enabled,
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
}
