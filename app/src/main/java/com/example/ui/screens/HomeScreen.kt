package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PingStatus
import com.example.model.VpnState
import com.example.ui.components.ConnectPowerButton
import com.example.ui.components.SpeedWaveGraph
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StatusConnecting
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.StatusError
import com.example.ui.viewmodel.VpnViewModel
import com.example.util.Formatters
import com.example.util.LocalizationHelper

@Composable
fun HomeScreen(
    viewModel: VpnViewModel,
    onToggleConnect: () -> Unit,
    onNavigateToConfigs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vpnState by viewModel.vpnState.collectAsState()
    val vpnStats by viewModel.vpnStats.collectAsState()
    val speedHistory by viewModel.speedHistory.collectAsState()
    val selectedConfig by viewModel.selectedConfig.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val isConnected = vpnState is VpnState.Connected

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Active Server Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .clickable(onClick = onNavigateToConfigs)
                .testTag("active_server_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = LocalizationHelper.getString("current_server", settings.language),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = selectedConfig?.name ?: LocalizationHelper.getString("no_config_selected", settings.language),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (selectedConfig != null) {
                        Text(
                            text = "${selectedConfig!!.protocol} • ${selectedConfig!!.server}:${selectedConfig!!.port}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = NeonCyan
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Switch Server",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Big Glowing Power Button
        ConnectPowerButton(
            vpnState = vpnState,
            language = settings.language,
            onToggle = onToggleConnect,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.autoPickFastestServer(context) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFFFD600), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Fastest Ping", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            }

            OutlinedButton(
                onClick = {
                    selectedConfig?.let { viewModel.testSinglePing(it) }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (selectedConfig?.lastPingMs ?: -1 > 0) "${selectedConfig?.lastPingMs} ms" else "Test Ping",
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Live Traffic Metrics 2x2 Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatMetricBox(
                title = LocalizationHelper.getString("download_speed", settings.language),
                value = if (isConnected) Formatters.formatSpeed(vpnStats.downloadSpeedBytesPerSec) else "0.0 KB/s",
                subtitle = "Total: ${Formatters.formatDataSize(vpnStats.totalDownloadedBytes)}",
                icon = Icons.Default.ArrowDownward,
                accentColor = NeonCyan,
                modifier = Modifier.weight(1f)
            )

            StatMetricBox(
                title = LocalizationHelper.getString("upload_speed", settings.language),
                value = if (isConnected) Formatters.formatSpeed(vpnStats.uploadSpeedBytesPerSec) else "0.0 KB/s",
                subtitle = "Total: ${Formatters.formatDataSize(vpnStats.totalUploadedBytes)}",
                icon = Icons.Default.ArrowUpward,
                accentColor = ElectricEmerald,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatMetricBox(
                title = LocalizationHelper.getString("duration", settings.language),
                value = if (isConnected) Formatters.formatDuration(vpnStats.connectedDurationSeconds) else "00:00",
                subtitle = if (isConnected) "Active Tunnel" else "Ready",
                icon = Icons.Default.Timer,
                accentColor = Color(0xFFFFD600),
                modifier = Modifier.weight(1f)
            )

            StatMetricBox(
                title = LocalizationHelper.getString("ip_address", settings.language),
                value = if (isConnected) vpnStats.publicIp else "---.---.---.---",
                subtitle = if (isConnected) vpnStats.countryName else "Offline",
                icon = Icons.Default.Dns,
                accentColor = Color(0xFFA855F7),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Dynamic Speed Graph
        SpeedWaveGraph(
            speedHistory = speedHistory,
            currentDownSpeed = vpnStats.downloadSpeedBytesPerSec,
            currentUpSpeed = vpnStats.uploadSpeedBytesPerSec,
            language = settings.language
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatMetricBox(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
