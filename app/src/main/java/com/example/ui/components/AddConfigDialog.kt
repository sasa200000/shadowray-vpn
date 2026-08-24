package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AppLanguage
import com.example.model.ProxyConfig
import com.example.model.ProxyProtocol
import com.example.parser.ConfigParser
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.NeonCyan
import com.example.util.LocalizationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddConfigDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onImportText: (String) -> Unit,
    onAddManualConfig: (ProxyConfig) -> Unit,
    onAddSubscription: (String, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val clipboardManager = LocalClipboardManager.current

    // Tab 0: Clipboard / Raw Text
    var rawInputText by remember { mutableStateOf("") }

    // Tab 1: QR Text
    var qrInputText by remember { mutableStateOf("") }

    // Tab 2: Manual Builder States
    var manualProtocol by remember { mutableStateOf(ProxyProtocol.VLESS) }
    var manualName by remember { mutableStateOf("") }
    var manualServer by remember { mutableStateOf("") }
    var manualPort by remember { mutableStateOf("443") }
    var manualUuidOrPass by remember { mutableStateOf("") }
    var manualSecurity by remember { mutableStateOf("tls") }
    var manualNetwork by remember { mutableStateOf("ws") }
    var manualSni by remember { mutableStateOf("") }
    var manualPath by remember { mutableStateOf("/") }
    var manualPbk by remember { mutableStateOf("") }
    var manualSid by remember { mutableStateOf("") }
    var manualExpandedProtocol by remember { mutableStateOf(false) }

    // Tab 3: Subscription
    var subTitle by remember { mutableStateOf("") }
    var subUrl by remember { mutableStateOf("") }

    val tabTitles = listOf(
        Pair(LocalizationHelper.getString("tab_clipboard", language), Icons.Default.ContentPaste),
        Pair(LocalizationHelper.getString("tab_qr", language), Icons.Default.QrCode),
        Pair(LocalizationHelper.getString("tab_manual", language), Icons.Default.Build),
        Pair(LocalizationHelper.getString("tab_subscription", language), Icons.Default.RssFeed)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = LocalizationHelper.getString("btn_add_config", language),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Row
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = NeonCyan
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    tabTitles.forEachIndexed { index, pair ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = pair.first,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            icon = { Icon(pair.second, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            selectedContentColor = NeonCyan,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable content area
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTab) {
                        0 -> {
                            // Clipboard / Text input
                            Text(
                                text = "Paste single or multiple vless://, vmess://, trojan://, ss:// URIs:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = rawInputText,
                                onValueChange = { rawInputText = it },
                                placeholder = { Text("vless://...\nvmess://...\ntrojan://...") },
                                minLines = 5,
                                maxLines = 8,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_raw_configs"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        clipboardManager.getText()?.let {
                                            rawInputText = it.text
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Paste Clipboard", color = MaterialTheme.colorScheme.onSurface)
                                }

                                Button(
                                    onClick = {
                                        if (rawInputText.isNotBlank()) {
                                            onImportText(rawInputText)
                                            onDismiss()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    enabled = rawInputText.isNotBlank(),
                                    modifier = Modifier.weight(1f).testTag("btn_confirm_import")
                                ) {
                                    Text("Import", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        1 -> {
                            // QR Scanner / Input
                            Text(
                                text = "Scan or paste QR Code content string:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = qrInputText,
                                onValueChange = { qrInputText = it },
                                placeholder = { Text("vless://... or vmess://...") },
                                minLines = 3,
                                maxLines = 5,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (qrInputText.isNotBlank()) {
                                        onImportText(qrInputText)
                                        onDismiss()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald),
                                enabled = qrInputText.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Process & Save QR Config", color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold)
                            }
                        }

                        2 -> {
                            // Manual Builder
                            ExposedDropdownMenuBox(
                                expanded = manualExpandedProtocol,
                                onExpandedChange = { manualExpandedProtocol = !manualExpandedProtocol }
                            ) {
                                OutlinedTextField(
                                    value = manualProtocol.name,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Protocol") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = manualExpandedProtocol) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = manualExpandedProtocol,
                                    onDismissRequest = { manualExpandedProtocol = false }
                                ) {
                                    listOf(ProxyProtocol.VLESS, ProxyProtocol.VMESS, ProxyProtocol.TROJAN, ProxyProtocol.SHADOWSOCKS).forEach { proto ->
                                        DropdownMenuItem(
                                            text = { Text(proto.name) },
                                            onClick = {
                                                manualProtocol = proto
                                                manualExpandedProtocol = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = manualName,
                                onValueChange = { manualName = it },
                                label = { Text(LocalizationHelper.getString("manual_name", language)) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = manualServer,
                                    onValueChange = { manualServer = it },
                                    label = { Text(LocalizationHelper.getString("manual_server", language)) },
                                    modifier = Modifier.weight(2f)
                                )
                                OutlinedTextField(
                                    value = manualPort,
                                    onValueChange = { manualPort = it },
                                    label = { Text(LocalizationHelper.getString("manual_port", language)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = manualUuidOrPass,
                                onValueChange = { manualUuidOrPass = it },
                                label = { Text(LocalizationHelper.getString("manual_uuid", language)) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = manualSecurity,
                                    onValueChange = { manualSecurity = it },
                                    label = { Text(LocalizationHelper.getString("manual_security", language)) },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = manualNetwork,
                                    onValueChange = { manualNetwork = it },
                                    label = { Text("Transport (ws/grpc/tcp)") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = manualSni,
                                onValueChange = { manualSni = it },
                                label = { Text(LocalizationHelper.getString("manual_sni", language)) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = manualPath,
                                onValueChange = { manualPath = it },
                                label = { Text(LocalizationHelper.getString("manual_path", language)) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (manualSecurity.equals("reality", ignoreCase = true)) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = manualPbk,
                                    onValueChange = { manualPbk = it },
                                    label = { Text("Reality Public Key (pbk)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = manualSid,
                                    onValueChange = { manualSid = it },
                                    label = { Text("Reality Short ID (sid)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val portInt = manualPort.toIntOrNull() ?: 443
                                    val config = ProxyConfig(
                                        name = manualName.ifBlank { "${manualProtocol.name}-${manualServer}" },
                                        protocol = manualProtocol,
                                        rawUri = "",
                                        server = manualServer.trim(),
                                        port = portInt,
                                        passwordOrUuid = manualUuidOrPass.trim(),
                                        security = manualSecurity.trim(),
                                        network = manualNetwork.trim(),
                                        sni = manualSni.trim(),
                                        path = manualPath.trim(),
                                        pbk = manualPbk.trim(),
                                        sid = manualSid.trim()
                                    )
                                    val uri = ConfigParser.serializeToUri(config)
                                    val finalConfig = config.copy(rawUri = uri)
                                    onAddManualConfig(finalConfig)
                                    onDismiss()
                                },
                                enabled = manualServer.isNotBlank() && manualUuidOrPass.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    LocalizationHelper.getString("manual_save", language),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        3 -> {
                            // Subscription Link
                            Text(
                                text = "Enter subscription URL providing auto-updating server configurations:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = subTitle,
                                onValueChange = { subTitle = it },
                                label = { Text(LocalizationHelper.getString("sub_name_label", language)) },
                                placeholder = { Text("e.g. VIP Subscription") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = subUrl,
                                onValueChange = { subUrl = it },
                                label = { Text(LocalizationHelper.getString("sub_url_label", language)) },
                                placeholder = { Text("https://example.com/api/v1/client/subscribe?token=...") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (subUrl.isNotBlank()) {
                                        onAddSubscription(subTitle.ifBlank { "Subscription" }, subUrl.trim())
                                        onDismiss()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald),
                                enabled = subUrl.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    LocalizationHelper.getString("subs_add_btn", language),
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
