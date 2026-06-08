package com.sismptm.client.ui.features.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import com.sismptm.client.R
import com.sismptm.client.core.network.NetworkConfig
import com.sismptm.client.core.session.SessionManager
import com.sismptm.client.ui.theme.Background
import com.sismptm.client.ui.theme.CardBackground
import com.sismptm.client.ui.theme.PrimaryAccent
import com.sismptm.client.ui.theme.TextPrimary
import com.sismptm.client.ui.theme.TextTertiary

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    picDirectory: String? = null,
    onUpdatePhoto: (android.net.Uri) -> Unit
) {
    val name = SessionManager.userName.ifBlank { stringResource(R.string.not_specified) }
    val email = SessionManager.userEmail.ifBlank { stringResource(R.string.not_specified) }
    val role = SessionManager.userRole.ifBlank { stringResource(R.string.not_specified) }
    val initial = name.take(1).uppercase()
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let(onUpdatePhoto)
    }
    
    var isEditing by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf(SessionManager.language) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.my_profile),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        val picDirToShow = picDirectory ?: SessionManager.picDirectory
        avatarSection(initial, picDirToShow) {
            imagePickerLauncher.launch("image/*")
        }
        Spacer(modifier = Modifier.height(32.dp))
        infoCardsSection(name, email, role)
        Spacer(modifier = Modifier.height(12.dp))
        
        if (isEditing) {
            editModeSection(
                selectedLanguage = selectedLanguage,
                onLanguageSelected = { selectedLanguage = it },
                onCancel = {
                    selectedLanguage = SessionManager.language
                    isEditing = false
                },
                onSave = {
                    SessionManager.updateLanguage(selectedLanguage)
                    isEditing = false
                }
            )
        } else {
            viewModeSection(
                onEditClick = { isEditing = true }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        logoutButton(onLogout)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun avatarSection(initial: String, picDirectory: String?, onPhotoClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(PrimaryAccent.copy(alpha = 0.2f))
            .clickable(onClick = onPhotoClick),
        contentAlignment = Alignment.Center
    ) {
        if (picDirectory != null) {
            val imageUrl = NetworkConfig.BASE_URL + picDirectory
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(300)
                    .build(),
                contentDescription = stringResource(R.string.change_profile_photo),
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = initial,
                color = PrimaryAccent,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = stringResource(R.string.change_profile_photo),
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun infoCardsSection(name: String, email: String, role: String) {
    profileInfoCard(icon = Icons.Default.Person, label = stringResource(R.string.full_name), value = name)
    Spacer(modifier = Modifier.height(12.dp))
    profileInfoCard(icon = Icons.Default.Email, label = stringResource(R.string.email), value = email)
    Spacer(modifier = Modifier.height(12.dp))
    profileInfoCard(icon = Icons.Default.Work, label = stringResource(R.string.role), value = role)
}

@Composable
private fun editModeSection(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    editableLanguageCard(
        selectedLanguage = selectedLanguage,
        onLanguageSelected = onLanguageSelected
    )
    
    Spacer(modifier = Modifier.height(32.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            border = BorderStroke(1.dp, TextTertiary)
        ) {
            Text(stringResource(R.string.cancel), color = TextPrimary)
        }
        
        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
        ) {
            Text(stringResource(R.string.save_changes), color = TextPrimary)
        }
    }
}

@Composable
private fun viewModeSection(onEditClick: () -> Unit) {
    profileInfoCard(
        icon = Icons.Default.Settings, 
        label = stringResource(R.string.preferred_language), 
        value = SessionManager.language.uppercase()
    )
    
    Spacer(modifier = Modifier.height(32.dp))
    Button(
        onClick = onEditClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
    ) {
        Text(stringResource(R.string.edit), color = TextPrimary)
    }
}

@Composable
private fun logoutButton(onLogout: () -> Unit) {
    OutlinedButton(
        onClick = {
            SessionManager.clearSession()
            onLogout()
        },
        border = BorderStroke(1.dp, TextTertiary),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth(0.6f)
            .height(48.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.home_sign_out),
            color = TextTertiary
        )
    }
}

@Composable
private fun profileInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PrimaryAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                Text(text = value, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun editableLanguageCard(selectedLanguage: String, onLanguageSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf(
        "en" to stringResource(R.string.language_english),
        "es" to stringResource(R.string.language_spanish)
    )
    val selectedLabel = languages.find { it.first == selectedLanguage }?.second ?: selectedLanguage

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PrimaryAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = PrimaryAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.preferred_language),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                    Text(
                        text = selectedLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(CardBackground)
            ) {
                languages.forEach { (code, label) ->
                    DropdownMenuItem(
                        text = { Text(label, color = TextPrimary) },
                        onClick = {
                            onLanguageSelected(code)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
