package com.sismptm.partner.ui.features.tour

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

/**
 * Screen displaying the details of an active tour service.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Service Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "Service in Progress", style = MaterialTheme.typography.headlineMedium)

                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Client: María García", style = MaterialTheme.typography.bodyLarge)
                        Text(text = "Status: ACTIVE", color = MaterialTheme.colorScheme.primary)
                        Text(text = "Route: Historic Downtown Tour", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
                    Text("Complete Service")
                }
            }
        }
    }
}

