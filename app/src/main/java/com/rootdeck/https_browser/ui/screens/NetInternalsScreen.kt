package com.rootdeck.https_browser.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rootdeck.https_browser.BrowserApplication
import com.rootdeck.https_browser.pagefile.MemoryPressureMonitor
import com.rootdeck.https_browser.ui.theme.ChromeBlue
import com.rootdeck.https_browser.ui.theme.GoogleGreen
import com.rootdeck.https_browser.ui.theme.GoogleRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetInternalsScreen(
    openTabCount: Int,
    blockedTrackerCount: Int,
    onTrimMemoryNow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onDismiss() }

    val app = BrowserApplication.instance
    var memoryStats by remember { mutableStateOf(app.memoryMonitor.getMemoryStats()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Net Internals (chrome://net-internals)", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            System.gc()
                            onTrimMemoryNow()
                            memoryStats = app.memoryMonitor.getMemoryStats()
                        }
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = "Trim Memory")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("net_internals_screen")
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Process & Memory Health", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ChromeBlue)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        StatRow("Total Device RAM", "${memoryStats.totalMemMb} MB")
                        StatRow("Available RAM", "${memoryStats.availMemMb} MB")
                        StatRow("Low Memory State", if (memoryStats.isLowMemory) "CRITICAL" else "NORMAL", if (memoryStats.isLowMemory) GoogleRed else GoogleGreen)
                        StatRow("Used App Heap", "${memoryStats.usedHeapMb} MB")
                        StatRow("Max Heap Limit", "${memoryStats.maxHeapMb} MB")
                        StatRow("Native Chromium Heap", "${memoryStats.nativeHeapMb} MB")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text("Tab Pagefile Subsystem", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ChromeBlue)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        StatRow("Active Open Tabs", "$openTabCount tabs")
                        StatRow("Pagefile Compression", "GZIP (2MB max/tab)")
                        StatRow("Memory Target PSS", "< 180 MB PSS on 1GB RAM")
                        StatRow("Renderer Limit", "1 process (sandboxed limit)")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text("Security & DNS Blackhole", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ChromeBlue)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        StatRow("Blocked Trackers & Ads", "$blockedTrackerCount intercepted", GoogleRed)
                        StatRow("Cleartext Policy", "Localhost exception enabled")
                        StatRow("SSL Certificate Verification", "TLS 1.2 / TLS 1.3 Strict")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text("Active CommandLine Flags", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ChromeBlue)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        app.engineConfig.toCommandLineFlags().forEach { flag ->
                            Text(
                                text = flag,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = valueColor, fontFamily = FontFamily.Monospace)
    }
}
