package com.sismptm.client.ui.features.map

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sismptm.client.R
import com.sismptm.client.ui.theme.BlueSecondary
import com.sismptm.client.ui.theme.CardBackground
import com.sismptm.client.ui.theme.InputBackground
import com.sismptm.client.ui.theme.InputBackgroundFocused
import com.sismptm.client.ui.theme.InputBorderFocused
import com.sismptm.client.ui.theme.InputTextActive
import com.sismptm.client.ui.theme.PurpleAccent
import com.sismptm.client.ui.theme.TextTertiary
import java.util.Locale

@Composable
fun LocationDescriptionSheet(
    viewModel: MapViewModel,
    reserveMode: Boolean = false,
    onConfirm: (location: MapLocation, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val description by viewModel.locationDescription.collectAsState()

    // Bottom sheet content only — backdrop handled by parent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = TextTertiary,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .padding(20.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.describe_location_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = Color.White
                )
            }
        }

        selectedLocation?.let {
            Text(
                text = "Lat: ${String.format(Locale.US, "%.4f", it.lat)} | " +
                    "Lon: ${String.format(Locale.US, "%.4f", it.lon)}",
                fontSize = 12.sp,
                color = TextTertiary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

            OutlinedTextField(
            value = description,
            onValueChange = { viewModel.onDescriptionChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp)
                .padding(bottom = 16.dp),
            placeholder = {
                Text(
                    stringResource(R.string.location_placeholder_example),
                    color = TextTertiary
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = InputBorderFocused,
                focusedBorderColor = BlueSecondary,
                unfocusedContainerColor = CardBackground,
                focusedContainerColor = InputBackgroundFocused,
                unfocusedTextColor = InputTextActive,
                focusedTextColor = Color.White
            )
        )

        Button(
            onClick = {
                Log.d(
                "LocationSheet",
                "[UI] Confirm button clicked | location=$selectedLocation | desc='$description'"
            )
                selectedLocation?.let { loc ->
                    Log.d("LocationSheet", "[ACTION] Calling onConfirm with loc=$loc")
                    onConfirm(loc, description)
                } ?: Log.w(
                    "LocationSheet",
                    "[WARNING] selectedLocation is NULL — button should be disabled"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (reserveMode) PurpleAccent else BlueSecondary
            ),
            enabled = selectedLocation != null
        ) {
            Text(
                text = if (reserveMode) "Continue to Schedule"
                else stringResource(R.string.confirm_location),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
