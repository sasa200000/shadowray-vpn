package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ProxyConfig
import com.example.parser.ConfigParser
import com.example.ui.components.AddConfigDialog
import com.example.ui.components.ConfigCard
import com.example.ui.components.QrCodeViewDialog
import com.example.ui.theme.NeonCyan
import com.example.ui.viewmodel.ConfigFilter
import com.example.ui.viewmodel.VpnViewModel
import com.example.util.LocalizationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigsScreen(
    viewModel: VpnViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val configs by viewModel.filteredConfigs.collectAsState()
    val selectedConfigId by viewModel.selectedConfigId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val isTestingAll by viewModel.isTestingAll.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var qrConfigToShow by remember { mutableStateOf<ProxyConfig?>(null) }
    var sortByPingAsc by remember { mutableStateOf(false) }

    val displayList = remember(configs, sortByPingAsc) {
        if (sortByPingAsc) {
            configs.sortedWith(
                compareBy(
                    { if (it.lastPingMs > 0) 0 else 1 },
                    { it.lastPingMs }
                )
            )
        } else {
            configs
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = NeonCyan,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_config")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Config")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text(LocalizationHelper.getString("search_placeholder", settings.language), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_configs_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips Horizontal Scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ConfigFilter.values().forEach { filter ->
                    val filterName = when (filter) {
                        ConfigFilter.ALL -> LocalizationHelper.getString("chip_all", settings.language)
                        ConfigFilter.VLESS -> "VLESS"
                        ConfigFilter.VMESS -> "VMess"
                        ConfigFilter.TROJAN -> "Trojan"
                        ConfigFilter.SHADOWSOCKS -> "Shadowsocks"
                        ConfigFilter.FAVORITES -> LocalizationHelper.getString("chip_favorites", settings.language)
                    }

                    FilterChip(
                        selected = activeFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filterName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = NeonCyan
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = activeFilter == filter,
                            borderColor = if (activeFilter == filter) NeonCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons (Test All & Sort)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.testAllPings() },
                    enabled = !isTestingAll,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    if (isTestingAll) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = NeonCyan)
                    } else {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        LocalizationHelper.getString("btn_test_all_pings", settings.language),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = { sortByPingAsc = !sortByPingAsc }
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort by Ping",
                        tint = if (sortByPingAsc) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Configs List
            if (displayList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No configurations found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Import configs from clipboard or add manually",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayList, key = { it.id }) { config ->
                        ConfigCard(
                            config = config,
                            isSelected = config.id == selectedConfigId,
                            language = settings.language,
                            onSelect = { viewModel.selectConfig(config) },
                            onPingTest = { viewModel.testSinglePing(config) },
                            onToggleFavorite = { viewModel.toggleFavorite(config) },
                            onShowQr = { qrConfigToShow = config },
                            onCopyUri = {
                                val uri = if (config.rawUri.isNotBlank()) config.rawUri else ConfigParser.serializeToUri(config)
                                clipboardManager.setText(AnnotatedString(uri))
                                Toast.makeText(context, LocalizationHelper.getString("copied_toast", settings.language), Toast.LENGTH_SHORT).show()
                            },
                            onShare = {
                                val uri = if (config.rawUri.isNotBlank()) config.rawUri else ConfigParser.serializeToUri(config)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, uri)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Config"))
                            },
                            onDelete = { viewModel.deleteConfig(config.id) }
                        )
                    }
                }
            }
        }

        // Add Config Dialog
        if (showAddDialog) {
            AddConfigDialog(
                language = settings.language,
                onDismiss = { showAddDialog = false },
                onImportText = { text -> viewModel.importConfigsFromText(text, context) },
                onAddManualConfig = { manualConfig -> viewModel.addManualConfig(manualConfig, context) },
                onAddSubscription = { title, url -> viewModel.addSubscription(title, url, context) }
            )
        }

        // QR Code Modal Dialog
        qrConfigToShow?.let { config ->
            val uri = if (config.rawUri.isNotBlank()) config.rawUri else ConfigParser.serializeToUri(config)
            QrCodeViewDialog(
                config = config,
                language = settings.language,
                onDismiss = { qrConfigToShow = null },
                onCopy = {
                    clipboardManager.setText(AnnotatedString(uri))
                    Toast.makeText(context, LocalizationHelper.getString("copied_toast", settings.language), Toast.LENGTH_SHORT).show()
                },
                onShare = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, uri)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Config"))
                }
            )
        }
    }
}
