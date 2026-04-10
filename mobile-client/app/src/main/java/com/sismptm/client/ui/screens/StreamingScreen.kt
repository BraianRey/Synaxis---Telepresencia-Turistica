package com.sismptm.client.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
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
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

/**
 * Streaming screen — Client (viewer) side.
 *
 * ## Architecture
 *
 * **SurfaceViewRenderer Initialization**: Is initialized with [viewModel.eglBase] (same EglBase
 * used by PeerConnectionFactory). This ensures consistent EGL context between WebRTC and rendering.
 * Critically, initialization is synchronous in `remember {}` to guarantee the renderer is ready
 * BEFORE the video track's onTrack callback fires.
 *
 * **Video Reception**: Remote video is received via `onTrack()` callback (Unified Plan semantics) →
 * VideoTrack.addSink() instead of the deprecated onAddStream → MediaStream pattern.
 *
 * **Connection Flow**:
 * 1. User enters Partner ID and clicks Connect
 * 2. startConnection() initiates Offer request
 * 3. Partner sends Offer via signaling server
 * 4. Client creates Answer and sends back
 * 5. ICE candidates exchanged (buffered until descriptions set)
 * 6. Connection established
 * 7. Partner sends remote video track → onTrack fires
 * 8. Renderer.addSink(track) starts displaying video
 *
 * @param onBack Callback when user clicks back/end session
 * @param viewModel The [StreamingViewModel] managing connection state and lifecycle
 */
@Composable
fun StreamingScreen(onBack: () -> Unit, viewModel: StreamingViewModel = viewModel()) {
    val context = LocalContext.current
    var showConnectionDialog by remember { mutableStateOf(true) }
    var targetPartnerId by remember { mutableStateOf("PARTNER_01") }

    // Renderer created once and reused — Factory lambda should NOT recreate it
    val remoteRenderer = remember {
        SurfaceViewRenderer(context).apply {
            // Initialize synchronously in remember {} so it's ready before any video arrives
            init(viewModel.eglBase.eglBaseContext, null)
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            setEnableHardwareScaler(true)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            remoteRenderer.release()
            // NOTE: eglBase is released by the ViewModel's onCleared()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Remote video surface
        AndroidView(factory = { remoteRenderer }, modifier = Modifier.fillMaxSize())

        // Top bar: back + settings
        Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                    onClick = onBack,
                    colors =
                            IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.3f)
                            )
            ) {
                Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                ConnectionStatusChip(isConnected = !showConnectionDialog)
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                        onClick = { showConnectionDialog = true },
                        colors =
                                IconButtonDefaults.iconButtonColors(
                                        containerColor = Color.Black.copy(alpha = 0.3f)
                                )
                ) {
                    Icon(
                            Icons.Default.Settings,
                            contentDescription = "Reconnect",
                            tint = Color.White
                    )
                }
            }
        }

        // Directional controls + end session button
        Column(
                modifier =
                        Modifier.align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DirectionalControls { direction -> viewModel.sendCommand(direction) }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                    onClick = onBack,
                    colors =
                            ButtonDefaults.buttonColors(
                                    containerColor = Color.Red.copy(alpha = 0.8f)
                            ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 32.dp).width(200.dp)
            ) {
                Text(
                        stringResource(R.string.end_session),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                )
            }
        }

        // Connection setup dialog
        if (showConnectionDialog) {
            AlertDialog(
                    onDismissRequest = { showConnectionDialog = false },
                    title = { Text("Connect to Partner") },
                    text = {
                        Column {
                            Text(
                                    "Enter the Partner ID to connect.\nThis must match the ID the Partner entered in their app:"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                    value = targetPartnerId,
                                    onValueChange = { targetPartnerId = it.trim() },
                                    label = { Text("Partner ID") },
                                    placeholder = { Text("e.g. PARTNER_01") },
                                    singleLine = true
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                                onClick = {
                                    showConnectionDialog = false
                                    // KEY: pass the renderer as the sink for the remote video track
                                    viewModel.startConnection(targetPartnerId) { videoTrack ->
                                        videoTrack.addSink(remoteRenderer)
                                    }
                                },
                                enabled = targetPartnerId.isNotBlank()
                        ) { Text("Connect") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConnectionDialog = false }) { Text("Cancel") }
                    }
            )
        }
    }
}

/**
 * Displays connection status indicator (WiFi or WiFi Off icon with text).
 *
 * @param isConnected True if WebRTC connection is established and stable
 */
@Composable
fun ConnectionStatusChip(isConnected: Boolean) {
    Surface(color = Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(20.dp)) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                    imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = if (isConnected) Color(0xFF4CAF50) else Color(0xFFFF5722),
                    modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                    text =
                            if (isConnected) stringResource(R.string.connection_status_good)
                            else stringResource(R.string.connection_status_poor),
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Directional control buttons layout (arrow pad).
 *
 * Arranges UP button above, LEFT/RIGHT buttons in middle row, DOWN button below. Sends "UP",
 * "DOWN", "LEFT", "RIGHT" commands when clicked.
 *
 * @param onDirectionClick Callback with command string when a direction is clicked
 */
@Composable
fun DirectionalControls(onDirectionClick: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ControlButton(Icons.Default.KeyboardArrowUp, stringResource(R.string.control_up)) {
            onDirectionClick("UP")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            ControlButton(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    stringResource(R.string.control_left)
            ) { onDirectionClick("LEFT") }
            Spacer(modifier = Modifier.width(48.dp))
            ControlButton(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    stringResource(R.string.control_right)
            ) { onDirectionClick("RIGHT") }
        }
        ControlButton(Icons.Default.KeyboardArrowDown, stringResource(R.string.control_down)) {
            onDirectionClick("DOWN")
        }
    }
}

/**
 * Individual directional control button with pressed animation.
 *
 * Animates scale and background color when pressed for tactile feedback.
 *
 * @param icon The arrow icon to display (UP/DOWN/LEFT/RIGHT)
 * @param contentDescription Accessibility label for the button
 * @param onClick Callback when button is clicked
 */
@Composable
fun ControlButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.85f else 1f, label = "Scale")
    val bgColor by
            animateColorAsState(
                    targetValue = if (isPressed) Color.White.copy(0.5f) else Color.White.copy(0.2f),
                    label = "Color"
            )
    FilledIconButton(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier = Modifier.size(64.dp).graphicsLayer(scaleX = scale, scaleY = scale),
            colors =
                    IconButtonDefaults.filledIconButtonColors(
                            containerColor = bgColor,
                            contentColor = Color.White
                    )
    ) { Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(36.dp)) }
}
