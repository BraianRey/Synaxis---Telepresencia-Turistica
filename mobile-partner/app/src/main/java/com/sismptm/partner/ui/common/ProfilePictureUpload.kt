package com.sismptm.partner.ui.common

import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sismptm.partner.ui.theme.PrimaryAccent

@Composable
fun ProfilePictureUpload(
    onPhotoClick: () -> Unit,
    selectedImageUri: Uri? = null,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = PrimaryAccent,
                    shape = CircleShape
                )
                .clickable(onClick = onPhotoClick),
            contentAlignment = Alignment.Center
        ) {
            if (selectedImageUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(selectedImageUri)
                        .crossfade(300)
                        .build(),
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add photo",
                    modifier = Modifier.size(48.dp),
                    tint = PrimaryAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (selectedImageUri != null) "Change photo" else "Add photo",
            fontSize = 14.sp,
            color = PrimaryAccent,
            fontWeight = FontWeight.Medium
        )
    }
}
