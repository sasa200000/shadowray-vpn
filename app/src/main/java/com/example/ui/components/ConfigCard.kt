package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.PingStatus
import com.example.model.ProxyConfig
import com.example.model.ProxyProtocol
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StatusConnecting
import com.example.ui.theme.StatusError
import com.example.util.Formatters
import com.example.util.LocalizationHelper

@Composable
fun ConfigCard(
    config: ProxyConfig,
    isSelected: Boolean,
    language: AppLanguage,
    onSelect: () -> Unit,
    onPingTest: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowQr: () -> Unit,
    onCopyUri: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val borderColor = if (isSelected) NeonCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val cardBg = if (isSelected) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val (pingColor, pingLabel) = when (config.pingStatus) {
        PingStatus.TESTING -> Pair(StatusConnecting, "Testing…")
        PingStatus.TIMEOUT -> Pair(StatusError, "Timeout")
        PingStatus.ERROR -> Pair(StatusError, "Fail")
        PingStatus.SUCCESS -> {
            when {
                config.lastPingMs in 1..200 -> Pair(ElectricEmerald, "${config.lastPingMs} ms")
                config.lastPingMs in 201..500 -> Pair(StatusConnecting, "${config.lastPingMs} ms")
                config.lastPingMs > 500 -> Pair(StatusError, "${config.lastPingMs} ms")
                else -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, "-- ms")
            }
        }
        PingStatus.IDLE -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, if (config.lastPingMs > 0) "${config.lastPingMs} ms" else "-- ms")
    }

    val protocolBg = when (config.protocol) {
        ProxyProtocol.VLESS -> NeonCyan.copy(alpha = 0.18f)
        ProxyProtocol.VMESS -> ElectricViolet.copy(alpha = 0.18f)
        ProxyProtocol.TROJAN -> ElectricEmerald.copy(alpha = 0.18f)
        ProxyProtocol.SHADOWSOCKS -> Color(0xFFFF9100).copy(alpha = 0.18f)
        ProxyProtocol.WIREGUARD -> Color(0xFF00B0FF).copy(alpha = 0.18f)
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    }

    val protocolTextColor = when (config.protocol) {
        ProxyProtocol.VLESS -> NeonCyan
        ProxyProtocol.VMESS -> ElectricViolet
        ProxyProtocol.TROJAN -> ElectricEmerald
        ProxyProtocol.SHADOWSOCKS -> Color(0xFFFF9100)
        ProxyProtocol.WIREGUARD -> Color(0xFF00B0FF)
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect)
            .padding(12.dp)
            .testTag("config_card_${config.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio selector
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = NeonCyan,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Main Info Column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Name & Favorite row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = config.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Server Address & Port
                Text(
                    text = "${config.server}:${config.port}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Badges Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Protocol Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(protocolBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = config.displayProtocolBadge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = protocolTextColor
                        )
                    }

                    // Security tag if TLS/Reality
                    if (config.isReality) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "REALITY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = Color(0xFFA78BFA)
                            )
                        }
                    } else if (config.isTls) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "TLS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = Color(0xFF34D399)
                            )
                        }
                    }

                    // Transport type
                    if (config.network.isNotBlank() && config.network != "tcp") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = config.network.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Ping Badge & Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(pingColor.copy(alpha = 0.15f))
                    .clickable(onClick = onPingTest)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (config.pingStatus == PingStatus.TESTING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = StatusConnecting
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(pingColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = pingLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = pingColor
                        )
                    }
                }
            }

            // Favorite star
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (config.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (config.isFavorite) Color(0xFFFFD600) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // More Options Dropdown
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(LocalizationHelper.getString("qr_code_title", language)) },
                        onClick = {
                            showMenu = false
                            onShowQr()
                        },
                        leadingIcon = { Icon(Icons.Default.QrCode2, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(LocalizationHelper.getString("copy_config", language)) },
                        onClick = {
                            showMenu = false
                            onCopyUri()
                        },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(LocalizationHelper.getString("share_config", language)) },
                        onClick = {
                            showMenu = false
                            onShare()
                        },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(LocalizationHelper.getString("ping", language)) },
                        onClick = {
                            showMenu = false
                            onPingTest()
                        },
                        leadingIcon = { Icon(Icons.Default.NetworkCheck, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(LocalizationHelper.getString("delete", language), color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}
