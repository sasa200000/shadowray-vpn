package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.VpnState
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StatusConnecting
import com.example.ui.theme.StatusDisconnected
import com.example.util.LocalizationHelper

@Composable
fun ConnectPowerButton(
    vpnState: VpnState,
    language: AppLanguage,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = vpnState is VpnState.Connected
    val isConnecting = vpnState is VpnState.Connecting || vpnState is VpnState.Disconnecting

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_animation")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnected || isConnecting) 1.22f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isConnecting) 800 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isConnecting) 800 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isConnecting) 1200 else 8000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val activeColor = when {
        isConnected -> ElectricEmerald
        isConnecting -> StatusConnecting
        else -> NeonCyan
    }

    val glowColor = when {
        isConnected -> Color(0xFF00E676)
        isConnecting -> Color(0xFFFFD600)
        else -> Color(0xFF00E5FF)
    }

    val buttonText = when {
        isConnected -> LocalizationHelper.getString("tap_to_disconnect", language)
        isConnecting -> LocalizationHelper.getString("status_connecting", language)
        else -> LocalizationHelper.getString("tap_to_connect", language)
    }

    val statusSubtitle = when {
        isConnected -> LocalizationHelper.getString("status_connected", language)
        isConnecting -> LocalizationHelper.getString("status_connecting", language)
        else -> LocalizationHelper.getString("status_disconnected", language)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(220.dp)
        ) {
            // Outer Pulsing Glow Wave
            Canvas(modifier = Modifier.size(210.dp)) {
                drawCircle(
                    color = glowColor.copy(alpha = pulseAlpha),
                    radius = (size.minDimension / 2f) * pulseScale
                )
            }

            // Rotating Cyber Orbit Ring
            Canvas(modifier = Modifier.size(190.dp)) {
                val strokeWidth = 3.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f

                if (isConnected || isConnecting) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                activeColor.copy(alpha = 0.1f),
                                activeColor,
                                activeColor.copy(alpha = 0.1f)
                            )
                        ),
                        startAngle = ringRotation,
                        sweepAngle = 260f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                } else {
                    drawCircle(
                        color = Color(0xFF26334D),
                        radius = radius,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // Main Clickable Inner Power Circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(150.dp)
                    .shadow(
                        elevation = if (isConnected) 24.dp else 12.dp,
                        shape = CircleShape,
                        spotColor = glowColor,
                        ambientColor = glowColor
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (isConnected) {
                                listOf(Color(0xFF00522B), Color(0xFF072115), Color(0xFF04140D))
                            } else if (isConnecting) {
                                listOf(Color(0xFF5A4800), Color(0xFF282000), Color(0xFF141000))
                            } else {
                                listOf(Color(0xFF192A4A), Color(0xFF101C33), Color(0xFF0A1120))
                            }
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggle
                    )
                    .testTag("vpn_power_button")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.Shield else Icons.Default.PowerSettingsNew,
                        contentDescription = "VPN Power",
                        tint = activeColor,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = statusSubtitle,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = activeColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = buttonText,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
