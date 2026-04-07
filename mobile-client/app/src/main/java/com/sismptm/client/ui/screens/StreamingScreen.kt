package com.sismptm.client.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.sismptm.client.R

/**
 * Screen that displays the live stream filling the screen with interactive controls.
 * Provides visual feedback on button presses to enhance the user experience.
 * @param onBack Callback to navigate back to the previous screen.
 */
@Composable
fun StreamingScreen(
    onBack: () -> Unit
) {
    var isConnectionGood by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "STREAM READY FOR WebRTC",
                color = Color.White.copy(alpha = 0.2f),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

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
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            ConnectionStatusOverlay(isGood = isConnectionGood)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DirectionalControls(
                onDirectionClick = { direction ->
                    println("Control pressed: $direction")
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            val endSessionInteractionSource = remember { MutableInteractionSource() }
            val isEndPressed by endSessionInteractionSource.collectIsPressedAsState()
            val endScale by animateFloatAsState(if (isEndPressed) 0.95f else 1f, label = "EndScale")

            Button(
                onClick = onBack,
                interactionSource = endSessionInteractionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = if (isEndPressed) 1f else 0.8f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .width(200.dp)
                    .graphicsLayer(scaleX = endScale, scaleY = endScale)
            ) {
                Text(
                    text = stringResource(R.string.end_session),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Visual indicator for the network connection quality overlaid on video content.
 */
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

/**
 * Directional control pad with tactile visual feedback.
 */
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

/**
 * Enhanced control button with scale and color animations for better feedback.
 */
@Composable
fun ControlButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(targetValue = if (isPressed) 0.85f else 1f, label = "ControlScale")
    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f),
        label = "ControlBgColor"
    )

    FilledIconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = backgroundColor,
            contentColor = Color.White
        )
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(36.dp))
    }
}
