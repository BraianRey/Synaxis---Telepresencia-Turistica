package com.sismptm.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailScreen(
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Detalles del Servicio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
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
                    text = "Detalles del Tour",
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
                            text = "Ubicación: Parque Nacional Natural Puracé",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Duración: 4 horas",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Precio: $150.000 COP",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Itinerario: Salida 8am, almuerzo incluido",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Button(onClick = onConfirm) {
                    Text("Confirmar")
                }

                OutlinedButton(onClick = onBack) {
                    Text("Volver")
                }
            }
        }
    }
}