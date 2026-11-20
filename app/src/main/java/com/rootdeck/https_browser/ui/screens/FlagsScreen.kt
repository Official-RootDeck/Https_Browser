package com.rootdeck.https_browser.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rootdeck.https_browser.ui.theme.ChromeBlue
import com.rootdeck.https_browser.ui.theme.GoogleRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlagsScreen(
    onRelaunchEngine: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onDismiss() }

    var processPerSite by remember { mutableStateOf(true) }
    var singleRendererLimit by remember { mutableStateOf(true) }
    var disableGpuSandbox by remember { mutableStateOf(true) }
    var remoteDebugging by remember { mutableStateOf(true) }
    var jsHeapCap128 by remember { mutableStateOf(true) }
    var trackerBlocker by remember { mutableStateOf(true) }
    var aggressivePagefile by remember { mutableStateOf(true) }
    var hasChanges by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Experiments (chrome://flags)", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (hasChanges) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Your changes will take effect after restart.",
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                hasChanges = false
                                onRelaunchEngine()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ChromeBlue),
                            modifier = Modifier.testTag("relaunch_engine_button")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Relaunch")
                        }
                    }
                }
            }
        },
        modifier = modifier.testTag("flags_screen")
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GoogleRed.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = null, tint = GoogleRed)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "WARNING: EXPERIMENTAL FEATURES AHEAD! By enabling these features, you could compromise browser security or performance.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            item {
                FlagToggleItem(
                    title = "--process-per-site",
                    description = "Shares a single renderer process for same-origin tabs to minimize RSS memory footprint.",
                    checked = processPerSite,
                    onCheckedChange = {
                        processPerSite = it
                        hasChanges = true
                    }
                )
            }

            item {
                FlagToggleItem(
                    title = "--renderer-process-limit=1",
                    description = "Restricts Chromium engine to a maximum of 1 renderer process for 1GB RAM budget.",
                    checked = singleRendererLimit,
                    onCheckedChange = {
                        singleRendererLimit = it
                        hasChanges = true
                    }
                )
            }

            item {
                FlagToggleItem(
                    title = "--remote-debugging-port=9222",
                    description = "Enables Chromium remote DevTools debugging protocol on localhost:9222 for live DOM inspection.",
                    checked = remoteDebugging,
                    onCheckedChange = {
                        remoteDebugging = it
                        hasChanges = true
                    }
                )
            }

            item {
                FlagToggleItem(
                    title = "--js-flags=--max-old-space-size=128",
                    description = "Cuts V8 heap maximum memory size to 128MB to prevent Out-Of-Memory kills on Android 5.1/1GB devices.",
                    checked = jsHeapCap128,
                    onCheckedChange = {
                        jsHeapCap128 = it
                        hasChanges = true
                    }
                )
            }

            item {
                FlagToggleItem(
                    title = "DNS Blackhole Tracker Blocking",
                    description = "Intercepts and blocks telemetry, ad networks, and tracking scripts before network dispatch.",
                    checked = trackerBlocker,
                    onCheckedChange = {
                        trackerBlocker = it
                        hasChanges = true
                    }
                )
            }

            item {
                FlagToggleItem(
                    title = "Aggressive Tab Pagefile Eviction",
                    description = "Automatically dumps background tab WebContents to GZIP-compressed disk pagefiles upon memory pressure.",
                    checked = aggressivePagefile,
                    onCheckedChange = {
                        aggressivePagefile = it
                        hasChanges = true
                    }
                )
            }
        }
    }
}

@Composable
private fun FlagToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
        supportingContent = { Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
}
