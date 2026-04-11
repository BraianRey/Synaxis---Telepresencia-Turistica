package com.sismptm.partner.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
 * Streaming screen for the Partner (broadcaster).
 * Key fix: the EglBase is owned by the ViewModel (not created here),
 * so the SurfaceViewRenderer is initialized with the exact same EGL context
 * that the PeerConnectionFactory uses. Mismatch between EGL contexts was
 * causing the native WebRTC crash (CloseStatus 1006).
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

    // Always present: base black background — prevents null-render / blank screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            !hasPermissions -> StreamingPermissionDeniedScreen {
                launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
            }

            !showIdDialog -> StreamingContent(
                onBack = onBack,
                viewModel = viewModel,
                partnerId = partnerIdInput
            )

            else -> {
                // Permissions granted but dialog not yet confirmed
                Text(
                    text = "Configure your stream...",
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // Configuration dialog shown on top of the black background
        if (hasPermissions && showIdDialog) {
            AlertDialog(
                onDismissRequest = { /* require explicit action */ },
                title = { Text("Partner Configuration") },
                text = {
                    Column {
                        Text("Enter your Partner ID.\nThe Client app must target this same ID to receive your stream.")
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = partnerIdInput,
                            onValueChange = { partnerIdInput = it.trim() },
                            label = { Text("Partner ID") },
                            placeholder = { Text("Ex: PARTNER_01") },
                            singleLine = true
                        )
                        if (partnerIdInput.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Client must connect to: $partnerIdInput",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { if (partnerIdInput.isNotBlank()) showIdDialog = false },
                        enabled = partnerIdInput.isNotBlank()
                    ) {
                        Text("Start Streaming")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onBack) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun StreamingPermissionDeniedScreen(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF12151B))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Camera & Microphone Required", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Grant permissions to start streaming.", color = Color(0xFFB9C0CB), fontSize = 14.sp)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))) {
                Text("Grant Permissions")
            }
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

        // SurfaceViewRenderer initialized with the ViewModel's EglBase
        // CRITICAL: must use the same EglBase as the PeerConnectionFactory
        AndroidView(
            factory = { _ ->
                SurfaceViewRenderer(context).also { renderer ->
                    // initStreaming uses viewModel.eglBase to init renderer + factory
                    viewModel.initStreaming(renderer, partnerId)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Partner ID label (bottom-right)
        Text(
            text = "Broadcasting as: $partnerId",
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )

        // Connection state indicator (top-right)
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
                PeerConnection.PeerConnectionState.CONNECTED  -> Color.Green
                PeerConnection.PeerConnectionState.CONNECTING -> Color.Yellow
                else -> Color.Red
            }
            Icon(Icons.Default.SignalCellularAlt, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = connectionState.name, color = Color.White, fontSize = 12.sp)
        }

        // Command history (top-left)
        if (commands.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(24.dp)
                    .width(200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Commands received:", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                commands.asReversed().forEach { InstructionItem(it) }
            }
        }

        // Back button (bottom-left)
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Text("←", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
        Text(instruction, color = Color.White, modifier = Modifier.padding(8.dp), fontSize = 13.sp)
    }
}

private fun playInstructionAudio(context: Context, instruction: String) {
    val audioResId = when (instruction.lowercase()) {
        "up", "forward"    -> R.raw.up
        "down", "backward" -> R.raw.down
        "left"             -> R.raw.left
        "right"            -> R.raw.right
        else               -> null
    }
    audioResId?.let {
        try {
            MediaPlayer.create(context, it)?.apply {
                setOnCompletionListener { mp -> mp.release() }
                start()
            }
        } catch (e: Exception) { /* no audio resource in debug — safe to ignore */ }
    }
}
