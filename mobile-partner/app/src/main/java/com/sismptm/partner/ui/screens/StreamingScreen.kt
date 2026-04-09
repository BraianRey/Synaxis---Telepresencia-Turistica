package com.sismptm.partner.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sismptm.partner.R
import org.webrtc.PeerConnection
import org.webrtc.SurfaceViewRenderer

/**
 * Screen for Live Streaming via WebRTC.
 */
@Composable
fun StreamingScreen(
    onBack: () -> Unit,
    viewModel: StreamingViewModel = viewModel()
) {
    val context = LocalContext.current
    var showIdDialog by remember { mutableStateOf(true) }
    var partnerIdInput by remember { mutableStateOf("PARTNER_01") }
    
    var hasPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions[Manifest.permission.CAMERA] == true &&
                         permissions[Manifest.permission.RECORD_AUDIO] == true
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    if (showIdDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Configure Identifier") },
            text = {
                Column {
                    Text("Enter the ID for this Partner (Robot):")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = partnerIdInput,
                        onValueChange = { partnerIdInput = it },
                        placeholder = { Text("Ex: PARTNER_01") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showIdDialog = false }) {
                    Text("Start")
                }
            }
        )
    }

    if (!hasPermissions) {
        StreamingPermissionDeniedScreen {
            launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    } else if (!showIdDialog) {
        StreamingContent(onBack = onBack, viewModel = viewModel, partnerId = partnerIdInput)
    }
}

@Composable
fun StreamingPermissionDeniedScreen(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF12151B)).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Permissions required", color = Color.White)
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
fun StreamingContent(
    onBack: () -> Unit,
    viewModel: StreamingViewModel,
    partnerId: String
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val commands by viewModel.commands.collectAsState()
    val lastCommand by viewModel.lastCommand.collectAsState()

    LaunchedEffect(lastCommand) {
        lastCommand?.let { playInstructionAudio(context, it) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                SurfaceViewRenderer(ctx).apply {
                    viewModel.initStreaming(this, partnerId)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay with current ID
        Text(
            text = "ID: $partnerId",
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )

        // Connection State
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tint = when (connectionState) {
                PeerConnection.PeerConnectionState.CONNECTED -> Color.Green
                PeerConnection.PeerConnectionState.CONNECTING -> Color.Yellow
                else -> Color.Red
            }
            Icon(Icons.Default.SignalCellularAlt, null, modifier = Modifier.size(20.dp), tint = tint)
            Text(" Signal", color = Color.White, fontSize = 12.sp)
        }

        // Instruction History
        Column(
            modifier = Modifier.align(Alignment.TopStart).padding(24.dp).width(160.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            commands.asReversed().forEach { instruction ->
                InstructionItem(instruction = instruction)
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.BottomStart).padding(24.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Text("←", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun InstructionItem(instruction: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(instruction, color = Color.White, modifier = Modifier.padding(8.dp))
    }
}

private fun playInstructionAudio(context: Context, instruction: String) {
    val audioResId = when (instruction.lowercase()) {
        "up", "forward" -> R.raw.up
        "down", "backward" -> R.raw.down
        "left" -> R.raw.left
        "right" -> R.raw.right
        else -> null
    }
    audioResId?.let {
        try {
            val mediaPlayer = MediaPlayer.create(context, it)
            mediaPlayer.setOnCompletionListener { mp -> mp.release() }
            mediaPlayer.start()
        } catch (e: Exception) { }
    }
}
