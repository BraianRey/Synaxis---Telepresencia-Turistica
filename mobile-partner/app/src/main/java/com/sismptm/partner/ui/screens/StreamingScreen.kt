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
import androidx.compose.ui.draw.alpha
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
 * Global reference to the currently active MediaPlayer to manage audio playback.
 */
private var activeMediaPlayer: MediaPlayer? = null

/**
 * Main entry point for the Partner's streaming interface.
 */
@Composable
fun StreamingScreen(
    serviceId: Long,
    onBack: () -> Unit,
    viewModel: StreamingViewModel = viewModel()
) {
    val context = LocalContext.current
    val partnerId = serviceId.toString() // Use Service ID as the channel ID

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!hasPermissions) {
            StreamingPermissionDeniedScreen {
                launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
            }
        } else {
            StreamingContent(
                onBack = onBack,
                viewModel = viewModel,
                partnerId = partnerId
            )
        }
    }
}

/**
 * Placeholder screen shown when required permissions are not granted.
 */
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
            Button(onClick = onRetry) {
                Text("Grant Permissions")
            }
        }
    }
}

/**
 * Core content of the streaming session, including the video feed and command feedback.
 */
@Composable
fun StreamingContent(
    onBack: () -> Unit,
    viewModel: StreamingViewModel,
    partnerId: String
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val commands by viewModel.commands.collectAsState()
    val lastCommandEvent by viewModel.lastCommandEvent.collectAsState()

    /**
     * Cleanup effect to ensure MediaPlayer resources are released when leaving the screen.
     */
    DisposableEffect(Unit) {
        onDispose {
            activeMediaPlayer?.let {
                try { if (it.isPlaying) it.stop() } catch (e: Exception) {}
                it.release()
            }
            activeMediaPlayer = null
        }
    }

    /**
     * Triggers audio feedback whenever a new command event is received.
     */
    LaunchedEffect(lastCommandEvent) {
        lastCommandEvent?.let { playInstructionAudio(context, it.text) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        AndroidView(
            factory = { _ ->
                SurfaceViewRenderer(context).also { renderer ->
                    viewModel.initStreaming(renderer, partnerId)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Text(
            text = "Streaming Channel: #$partnerId",
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )

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

        if (commands.isNotEmpty()) {
            val displayCommands = commands.asReversed().take(3)
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(24.dp)
                    .width(200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Latest Commands:", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                displayCommands.forEachIndexed { index, command ->
                    val alphaValue = when(index) {
                        0 -> 1.0f
                        1 -> 0.6f
                        2 -> 0.3f
                        else -> 0f
                    }
                    InstructionItem(command, alphaValue)
                }
            }
        }

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

/**
 * Individual command item with varying alpha based on its chronological order.
 */
@Composable
fun InstructionItem(instruction: String, alphaValue: Float) {
    Surface(
        modifier = Modifier.fillMaxWidth().alpha(alphaValue),
        color = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(instruction, color = Color.White, modifier = Modifier.padding(8.dp), fontSize = 13.sp)
    }
}

/**
 * Handles audio playback for directional instructions, ensuring only one sound plays at a time.
 */
private fun playInstructionAudio(context: Context, instruction: String) {
    val lang = com.sismptm.partner.utils.SessionManager.language
    val audioName = "${instruction.lowercase()}_$lang"
    val audioResId = context.resources.getIdentifier(audioName, "raw", context.packageName)

    if (audioResId == 0) return

    try {
        activeMediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
    } catch (e: Exception) {}

    try {
        activeMediaPlayer = MediaPlayer.create(context, audioResId)?.apply {
            setOnCompletionListener { mp ->
                mp.release()
                if (activeMediaPlayer == mp) activeMediaPlayer = null
            }
            start()
        }
    } catch (e: Exception) {
        activeMediaPlayer = null
    }
}
