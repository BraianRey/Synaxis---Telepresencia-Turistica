package com.sismptm.partner.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sismptm.partner.R

/**
 * Reusable component to display a tour request.
 */
@Composable
fun RequestCard(
    clientName: String,
    location: String,
    elapsedTime: String,
    duration: String,
    price: String,
    onDecline: () -> Unit,
    onAccept: () -> Unit,
    isAccepting: Boolean = false,
    acceptEnabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2430))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF374151)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = clientName.take(1).uppercase(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = clientName, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = stringResource(id = R.string.wants_tour, location), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFB9C0CB))
                }

                Text(text = elapsedTime, style = MaterialTheme.typography.labelMedium, color = Color(0xFF9DA5B3))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailChip(text = duration)
                DetailChip(text = price)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDecline,
                    enabled = !isAccepting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
                ) {
                    Text(text = stringResource(id = R.string.decline), fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onAccept,
                    enabled = acceptEnabled,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White)
                ) {
                    if (isAccepting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(text = stringResource(id = R.string.accept), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailChip(text: String) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF2B3444)).padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = Color(0xFFE5E7EB))
    }
}
