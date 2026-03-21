package com.sismptm.partner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sismptm.partner.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onLogout: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(id = R.string.home_title)) },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text(stringResource(id = R.string.logout))
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
            Text(
                text = stringResource(id = R.string.welcome_home),
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}
