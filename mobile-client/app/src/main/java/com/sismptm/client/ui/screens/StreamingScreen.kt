package com.sismptm.client.ui.screens

import android.view.ViewGroup
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sismptm.client.R
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

/**
 * Screen that handles P2P streaming using WebRTC and ViewModel.
 */
@Composable
fun StreamingScreen(
    onBack: () -> Unit,
    viewModel: StreamingViewModel = viewModel()
) {
    val context = LocalContext.current
    var isConnectionGood by remember { mutableStateOf(true) }
    var showConnectionDialog by remember { mutableStateOf(true) }
    var partnerAddress by remember { mutableStateOf("") }
    
    val eglBase = remember { EglBase.create() }
    val remoteRenderer = remember { SurfaceViewRenderer(context) }

    DisposableEffect(Unit) {
        remoteRenderer.init(eglBase.eglBaseContext, null)
        remoteRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        remoteRenderer.setEnableHardwareScaler(true)
        
        onDispose {
            remoteRenderer.release()
            eglBase.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { remoteRenderer },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay UI
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.3f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Row {
                ConnectionStatusOverlay(isGood = isConnectionGood)
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showConnectionDialog = true },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Config", tint = Color.White)
                }
            }
        }

        // Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DirectionalControls(
                onDirectionClick = { direction ->
                    // Send command via ViewModel (DataChannel P2P)
                    viewModel.sendCommand(direction)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 32.dp).width(200.dp)
            ) {
                Text(stringResource(R.string.end_session), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        if (showConnectionDialog) {
            AlertDialog(
                onDismissRequest = { showConnectionDialog = false },
                title = { Text("P2P Signaling Setup") },
                text = {
                    Column {
                        Text("Enter Partner ID to initiate connection:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = partnerAddress,
                            onValueChange = { partnerAddress = it },
                            placeholder = { Text("e.g. PARTNER_01") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { 
                        showConnectionDialog = false
                        viewModel.startConnection(eglBase, partnerAddress) { stream ->
                            if (stream.videoTracks.isNotEmpty()) {
                                stream.videoTracks[0].addSink(remoteRenderer)
                            }
                        }
                    }) {
                        Text("Connect")
                    }
                }
            )
        }
    }
}

@Composable
fun ConnectionStatusOverlay(isGood: Boolean) {
    Surface(
        color = Color.Black.copy(alpha = 0.3f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = if (isGood) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = null,
                tint = if (isGood) Color(0xFF4CAF50) else Color.Red,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(if (isGood) R.string.connection_status_good else R.string.connection_status_poor),
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun DirectionalControls(onDirectionClick: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ControlButton(Icons.Default.KeyboardArrowUp, stringResource(R.string.control_up)) { onDirectionClick("UP") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            ControlButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.control_left)) { onDirectionClick("LEFT") }
            Spacer(modifier = Modifier.width(48.dp))
            ControlButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.control_right)) { onDirectionClick("RIGHT") }
        }
        ControlButton(Icons.Default.KeyboardArrowDown, stringResource(R.string.control_down)) { onDirectionClick("DOWN") }
    }
}

@Composable
fun ControlButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.85f else 1f, label = "Scale")
    val bgColor by animateColorAsState(targetValue = if (isPressed) Color.White.copy(0.5f) else Color.White.copy(0.2f), label = "Color")

    FilledIconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier.size(64.dp).graphicsLayer(scaleX = scale, scaleY = scale),
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = bgColor, contentColor = Color.White)
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(36.dp))
    }
}
