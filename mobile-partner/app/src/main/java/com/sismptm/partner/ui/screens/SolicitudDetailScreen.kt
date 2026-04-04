package com.sismptm.partner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

/**
 * Screen displaying details of an incoming tour request.
 * Allows partners to view request details and accept or reject the request.
 *
 * @param onAccept Callback triggered when partner accepts the tour request.
 * @param onReject Callback triggered when partner rejects the tour request.
 * @param onBack Callback triggered when user navigates back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitudDetailScreen(
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Request Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Tour Request",
                    style = MaterialTheme.typography.headlineMedium
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Client: María García",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Location: Puracé National Natural Park",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Description: 4-hour guided tour",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Offered Price: $150,000 COP",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reject")
                    }

                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Accept")
                    }
                }

                OutlinedButton(onClick = onBack) {
                    Text("Back")
                }
            }
        }
    }
}