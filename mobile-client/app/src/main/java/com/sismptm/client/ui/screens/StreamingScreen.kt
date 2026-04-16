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
 * Main streaming UI component for the client side.
 * 
 * This screen provides the video surface for remote stream playback, connection status
 * indicators, and interactive controls to send commands back to the partner.
 * It manages the lifecycle of the WebRTC video renderer and coordinates with 
 * the StreamingViewModel to establish and maintain the media session.
 *
 * @param onBack Navigation callback to return to the previous screen.
 * @param viewModel The state holder managing the WebRTC connection logic.
 */
@Composable
fun StreamingScreen(onBack: () -> Unit, viewModel: StreamingViewModel = viewModel()) {
    val context = LocalContext.current
    var showConnectionDialog by remember { mutableStateOf(true) }
    var targetPartnerId by remember { mutableStateOf("PARTNER_01") }

    /**
     * SurfaceViewRenderer used to display the remote video track.
     * Initialized synchronously to be ready for incoming media frames.
     */
    val remoteRenderer = remember {
        SurfaceViewRenderer(context).apply {
            init(viewModel.eglBase.eglBaseContext, null)
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            setEnableHardwareScaler(true)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            remoteRenderer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        /**
         * Integration of the native WebRTC SurfaceView into the Compose hierarchy.
         */
        AndroidView(factory = { remoteRenderer }, modifier = Modifier.fillMaxSize())

        /**
         * Top overlay bar containing navigation and session settings.
         */
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

        /**
         * Interactive control overlay for session management and remote commands.
         */
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

        /**
         * Modal dialog for establishing a connection with a specific partner.
         */
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
 * Visual indicator of the current WebRTC connection health.
 *
 * @param isConnected Whether the media connection is active.
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
 * Layout containing directional input buttons for remote interaction.
 *
 * @param onDirectionClick Callback triggered with the direction name when a button is pressed.
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
 * Animated button for individual control inputs with visual feedback.
 *
 * @param icon The vector icon to display.
 * @param contentDescription Accessibility label for the icon.
 * @param onClick Callback triggered on button activation.
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
