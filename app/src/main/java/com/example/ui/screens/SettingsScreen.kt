package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.DnsProvider
import com.example.model.RoutingMode
import com.example.parser.ConfigParser
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.viewmodel.VpnViewModel
import com.example.util.LocalizationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: VpnViewModel,
    onNavigateToAppFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val settings by viewModel.settings.collectAsState()
    val allConfigs by viewModel.allConfigs.collectAsState()

    var dnsExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = LocalizationHelper.getString("nav_settings", settings.language),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Routing Rules Section
        SettingsSection(
            title = LocalizationHelper.getString("settings_routing_title", settings.language),
            icon = Icons.Default.AltRoute,
            accentColor = NeonCyan
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RoutingModeOption(
                    title = LocalizationHelper.getString("routing_global", settings.language),
                    subtitle = "Route all traffic through active VPN proxy",
                    isSelected = settings.routingMode == RoutingMode.GLOBAL,
                    onSelect = { viewModel.updateSettings(settings.copy(routingMode = RoutingMode.GLOBAL)) }
                )

                RoutingModeOption(
                    title = LocalizationHelper.getString("routing_bypass_iran", settings.language),
                    subtitle = "Direct connection for domestic Iran and local networks",
                    isSelected = settings.routingMode == RoutingMode.BYPASS_IRAN_LAN,
                    onSelect = { viewModel.updateSettings(settings.copy(routingMode = RoutingMode.BYPASS_IRAN_LAN)) }
                )

                RoutingModeOption(
                    title = LocalizationHelper.getString("routing_custom_apps", settings.language),
                    subtitle = "${settings.bypassedPackages.size} apps bypassed from VPN tunnel",
                    isSelected = settings.routingMode == RoutingMode.CUSTOM_APP_LIST,
                    onSelect = { viewModel.updateSettings(settings.copy(routingMode = RoutingMode.CUSTOM_APP_LIST)) }
                )

                // Per-App Split Tunneling Selector Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(onClick = onNavigateToAppFilter)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Apps, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = LocalizationHelper.getString("settings_app_filter", settings.language),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DNS Server Settings Section
        SettingsSection(
            title = LocalizationHelper.getString("settings_dns_title", settings.language),
            icon = Icons.Default.Dns,
            accentColor = ElectricEmerald
        ) {
            Column {
                ExposedDropdownMenuBox(
                    expanded = dnsExpanded,
                    onExpandedChange = { dnsExpanded = !dnsExpanded }
                ) {
                    OutlinedTextField(
                        value = settings.dnsProvider.title,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dnsExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = dnsExpanded,
                        onDismissRequest = { dnsExpanded = false }
                    ) {
                        DnsProvider.values().forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider.title) },
                                onClick = {
                                    viewModel.updateSettings(settings.copy(dnsProvider = provider))
                                    dnsExpanded = false
                                }
                            )
                        }
                    }
                }

                if (settings.dnsProvider == DnsProvider.CUSTOM) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = settings.customDns1,
                        onValueChange = { viewModel.updateSettings(settings.copy(customDns1 = it)) },
                        label = { Text("Primary DNS Server") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security & Advanced Tunnels
        SettingsSection(
            title = "Security & Network Advanced",
            icon = Icons.Default.Security,
            accentColor = Color(0xFFFFD600)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingSwitchItem(
                    title = LocalizationHelper.getString("settings_kill_switch", settings.language),
                    subtitle = LocalizationHelper.getString("settings_kill_switch_desc", settings.language),
                    checked = settings.killSwitchEnabled,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(killSwitchEnabled = it)) }
                )

                SettingSwitchItem(
                    title = "Auto-Connect on Launch",
                    subtitle = "Automatically establish VPN when app is opened",
                    checked = settings.autoConnectOnLaunch,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(autoConnectOnLaunch = it)) }
                )

                SettingSwitchItem(
                    title = "Bypass Local LAN",
                    subtitle = "Allow access to home devices (printers, local routers)",
                    checked = settings.bypassLan,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(bypassLan = it)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language & Appearance
        SettingsSection(
            title = "App Customization",
            icon = Icons.Default.Tune,
            accentColor = Color(0xFFA855F7)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Language selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = LocalizationHelper.getString("settings_language", settings.language),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row {
                        FilterButton(
                            text = "فارسی",
                            isSelected = settings.language == AppLanguage.PERSIAN,
                            onClick = { viewModel.updateSettings(settings.copy(language = AppLanguage.PERSIAN)) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FilterButton(
                            text = "English",
                            isSelected = settings.language == AppLanguage.ENGLISH,
                            onClick = { viewModel.updateSettings(settings.copy(language = AppLanguage.ENGLISH)) }
                        )
                    }
                }

                // Theme Mode selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, contentDescription = null, tint = Color(0xFFFFD600), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = LocalizationHelper.getString("settings_theme", settings.language),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row {
                        FilterButton(
                            text = "Dark",
                            isSelected = settings.themeMode == AppThemeMode.DARK,
                            onClick = { viewModel.updateSettings(settings.copy(themeMode = AppThemeMode.DARK)) }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        FilterButton(
                            text = "Light",
                            isSelected = settings.themeMode == AppThemeMode.LIGHT,
                            onClick = { viewModel.updateSettings(settings.copy(themeMode = AppThemeMode.LIGHT)) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Backup / Export
        SettingsSection(
            title = "Backup & Export",
            icon = Icons.Default.ContentCopy,
            accentColor = NeonCyan
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable {
                        val exported = allConfigs.joinToString("\n") { config ->
                            if (config.rawUri.isNotBlank()) config.rawUri else ConfigParser.serializeToUri(config)
                        }
                        clipboardManager.setText(AnnotatedString(exported))
                        Toast.makeText(context, "${allConfigs.size} configs copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Export All Configurations",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Copies all ${allConfigs.size} server links as text to clipboard",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            content()
        }
    }
}

@Composable
private fun RoutingModeOption(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) NeonCyan.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (isSelected) NeonCyan else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onSelect)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NeonCyan
            )
        )
    }
}

@Composable
private fun FilterButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) NeonCyan else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}
