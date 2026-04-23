package com.sismptm.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LocationDescriptionSheet(
    viewModel: MapViewModel,
    onConfirm: (location: MapLocation, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val description by viewModel.locationDescription.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
            .clickable { onDismiss() }
    )

    // Bottom sheet
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF1A1A1A),
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
                text = "Describe the location",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }

        selectedLocation?.let {
            Text(
                text = "Lat: ${String.format("%.4f", it.lat)} | Lon: ${String.format("%.4f", it.lon)}",
                fontSize = 12.sp,
                color = Color(0xFFAAAAAA),
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
            placeholder = { Text("E.g., Coffee shop near the park...", color = Color(0xFF888888)) },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFF444444),
                focusedBorderColor = Color(0xFF2196F3),
                unfocusedContainerColor = Color(0xFF2C2C2C),
                focusedContainerColor = Color(0xFF333333),
                unfocusedTextColor = Color(0xFFDDDDDD),
                focusedTextColor = Color.White
            )
        )

        Button(
            onClick = {
                selectedLocation?.let { loc ->
                    onConfirm(loc, description)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
            enabled = selectedLocation != null
        ) {
            Text("Confirm location", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}