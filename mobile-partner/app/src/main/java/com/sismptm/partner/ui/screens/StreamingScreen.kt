package com.sismptm.partner.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.sismptm.partner.R
import kotlinx.coroutines.delay

/**
 * Screen for Live Streaming via WebRTC.
 * Includes a video preview using CameraX, connection quality indicator,
 * and a history of received movement instructions with audio feedback.
 */
@Composable
fun StreamingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    
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

    if (!hasPermissions) {
        StreamingPermissionDeniedScreen {
            launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    } else {
        StreamingContent(onBack = onBack)
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
            Text(
                text = stringResource(id = R.string.camera_mic_permission_required),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.camera_mic_permission_explanation),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFB9C0CB),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text(stringResource(id = R.string.grant_permissions))
            }
        }
    }
}

@Composable
fun StreamingContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    // History of instructions (max 3)
    var instructions by remember { mutableStateOf(listOf<String>()) }
    
    // SIMULATION: Receiving instructions from server (every 10 seconds for testing)
    LaunchedEffect(Unit) {
        val possibleInstructions = listOf("up", "down", "left", "right")
        while (true) {
            delay(10000) 
            val newInstruction = possibleInstructions.random()
            instructions = (instructions + newInstruction).takeLast(3)
            
            // Audio playback based on instruction
            playInstructionAudio(context, newInstruction)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // UI Overlay
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f)))

        // 2. Connection Quality
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.SignalCellularAlt, null, modifier = Modifier.size(20.dp), tint = Color.Green)
            Text(stringResource(R.string.connection_quality), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // 3. Instruction History (Localized)
        Column(
            modifier = Modifier.align(Alignment.TopStart).padding(24.dp).width(160.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            instructions.asReversed().forEachIndexed { index, instruction ->
                val alpha by animateFloatAsState(targetValue = when (index) { 0 -> 1.0f; 1 -> 0.6f; else -> 0.3f })
                InstructionItem(instruction = instruction, modifier = Modifier.alpha(alpha))
            }
            if (instructions.isEmpty()) {
                Text(stringResource(R.string.waiting_instructions), color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
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
fun InstructionItem(instruction: String, modifier: Modifier = Modifier) {
    val textId = when (instruction) {
        "up" -> R.string.instruction_up
        "down" -> R.string.instruction_down
        "left" -> R.string.instruction_left
        "right" -> R.string.instruction_right
        else -> R.string.waiting_instructions
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = stringResource(id = textId),
            color = Color.White,
            modifier = Modifier.padding(8.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

/**
 * Plays the corresponding audio file from res/raw or res/raw-es.
 * Android automatically picks the correct folder based on app language.
 */
private fun playInstructionAudio(context: Context, instruction: String) {
    val audioResId = when (instruction) {
        "up" -> R.raw.up
        "down" -> R.raw.down
        "left" -> R.raw.left
        "right" -> R.raw.right
        else -> null
    }

    audioResId?.let {
        try {
            val mediaPlayer = MediaPlayer.create(context, it)
            mediaPlayer.setOnCompletionListener { mp -> mp.release() }
            mediaPlayer.start()
        } catch (e: Exception) {
            println("Error playing audio: ${e.message}")
        }
    }
}
