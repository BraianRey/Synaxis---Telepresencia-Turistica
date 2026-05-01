package com.sismptm.partner.ui.features.streaming

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.MediaPlayer
import android.util.Log
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sismptm.partner.R
import com.sismptm.partner.core.session.SessionManager
import org.webrtc.PeerConnection
import org.webrtc.SurfaceViewRenderer
import java.util.Locale

private var activeMediaPlayer: MediaPlayer? = null

/**
 * Interface for live streaming, displaying incoming directional commands and providing 
 * audio feedback to the partner.
 */
@Composable
fun StreamingScreen(
    serviceId: Long,
    onBack: () -> Unit,
    viewModel: StreamingViewModel = viewModel()
) {
    val context = LocalContext.current
    val partnerId = serviceId.toString()

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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (!hasPermissions) {
            StreamingPermissionDeniedScreen {
                launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
            }
        } else {
            StreamingContent(onBack = onBack, viewModel = viewModel, partnerId = partnerId)
        }
    }
}

@Composable
private fun StreamingPermissionDeniedScreen(onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF12151B)).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Camera & Microphone Required", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Button(onClick = onRetry) { Text("Grant Access") }
        }
    }
}

@Composable
private fun StreamingContent(
    onBack: () -> Unit,
    viewModel: StreamingViewModel,
    partnerId: String
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val commands by viewModel.commands.collectAsState()
    val lastCommandEvent by viewModel.lastCommandEvent.collectAsState()

    // Persistent renderer to avoid recreation during recompositions
    val surfaceViewRenderer = remember {
        SurfaceViewRenderer(context).also { renderer ->
            viewModel.initStreaming(renderer, partnerId)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activeMediaPlayer?.let {
                try { if (it.isPlaying) it.stop() } catch (e: Exception) {}
                it.release()
            }
            activeMediaPlayer = null
            // Release renderer when leaving the screen
            surfaceViewRenderer.release()
        }
    }

    LaunchedEffect(lastCommandEvent) {
        lastCommandEvent?.let { playInstructionAudio(context, it.text) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { surfaceViewRenderer },
            modifier = Modifier.fillMaxSize(),
            update = { /* Renderer is managed via remember and viewModel */ }
        )

        ConnectionStatusBadge(
            state = connectionState,
            modifier = Modifier.align(Alignment.TopEnd).padding(24.dp)
        )

        if (commands.isNotEmpty()) {
            CommandOverlay(
                displayCommands = commands.asReversed().take(3),
                modifier = Modifier.align(Alignment.TopStart).padding(24.dp)
            )
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.BottomStart).padding(24.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Text("←", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}

@Composable
private fun ConnectionStatusBadge(state: PeerConnection.PeerConnectionState, modifier: Modifier) {
    val tint = when (state) {
        PeerConnection.PeerConnectionState.CONNECTED  -> Color.Green
        PeerConnection.PeerConnectionState.CONNECTING -> Color.Yellow
        else -> Color.Red
    }
    Row(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.SignalCellularAlt, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = state.name, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun CommandOverlay(displayCommands: List<String>, modifier: Modifier) {
    Column(modifier = modifier.width(200.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Composable
private fun InstructionItem(instruction: String, alphaValue: Float) {
    val context = LocalContext.current
    val currentConfig = LocalConfiguration.current
    val lang = SessionManager.language
    
    val displayId = when (instruction.lowercase().trim()) {
        "up" -> R.string.instruction_up
        "down" -> R.string.instruction_down
        "left" -> R.string.instruction_left
        "right" -> R.string.instruction_right
        else -> null
    }

    val displayText = remember(instruction, lang) {
        if (displayId != null) {
            val config = Configuration(currentConfig)
            config.setLocale(Locale.forLanguageTag(lang))
            context.createConfigurationContext(config).getString(displayId)
        } else {
            instruction
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().alpha(alphaValue),
        color = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(displayText, color = Color.White, modifier = Modifier.padding(8.dp), fontSize = 13.sp)
    }
}

private fun playInstructionAudio(context: Context, instruction: String) {
    val lang = SessionManager.language
    val audioName = instruction.lowercase().trim()
    
    val config = Configuration(context.resources.configuration)
    config.setLocale(Locale.forLanguageTag(lang))
    val localizedContext = context.createConfigurationContext(config)
    
    val audioResId = localizedContext.resources.getIdentifier(audioName, "raw", context.packageName)

    if (audioResId == 0) return

    try {
        activeMediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
    } catch (e: Exception) {}

    try {
        activeMediaPlayer = MediaPlayer.create(localizedContext, audioResId)?.apply {
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
