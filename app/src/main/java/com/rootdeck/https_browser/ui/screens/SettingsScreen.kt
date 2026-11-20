package com.rootdeck.https_browser.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rootdeck.https_browser.ui.theme.ChromeBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    trackingProtectionEnabled: Boolean,
    onToggleTrackingProtection: (Boolean) -> Unit,
    onClearBrowsingData: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onDismiss() }

    var showClearDialog by remember { mutableStateOf(false) }
    var selectedSearchEngine by remember { mutableStateOf("Google") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("settings_screen")
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Text(
                    text = "Basics",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChromeBlue,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Search engine") },
                    supportingContent = { Text(selectedSearchEngine) },
                    leadingContent = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.clickable {
                        selectedSearchEngine = when (selectedSearchEngine) {
                            "Google" -> "DuckDuckGo"
                            "DuckDuckGo" -> "Bing"
                            else -> "Google"
                        }
                    }
                )
            }

            item {
                HorizontalDivider()
                Text(
                    text = "Privacy & Security",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChromeBlue,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Tracking Protection") },
                    supportingContent = { Text("Block ad telemetry and analytics tracker domains with local DNS Blackhole") },
                    leadingContent = { Icon(Icons.Default.Security, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = trackingProtectionEnabled,
                            onCheckedChange = onToggleTrackingProtection
                        )
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Clear browsing data") },
                    supportingContent = { Text("Clear history, cookies, and cache") },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                    modifier = Modifier.clickable { showClearDialog = true }
                )
            }

            item {
                HorizontalDivider()
                Text(
                    text = "Low-Memory Architecture",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChromeBlue,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Tab Pagefile Compression") },
                    supportingContent = { Text("GZIP compressed serialization with LRU disk eviction active (2MB budget)") },
                    leadingContent = { Icon(Icons.Default.Memory, contentDescription = null) },
                    trailingContent = {
                        Text("Active", color = ChromeBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("V8 Heap Limit") },
                    supportingContent = { Text("--js-flags=--max-old-space-size=128 (Optimized for 1GB devices)") },
                    leadingContent = { Icon(Icons.Default.Speed, contentDescription = null) }
                )
            }

            item {
                HorizontalDivider()
                Text(
                    text = "About Chrome",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChromeBlue,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Application") },
                    supportingContent = { Text("Chrome M124 Standalone Chromium Engine\nAndroid 5.1+ (API 22 - 36 compatible)") },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
                )
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear browsing data?") },
            text = { Text("This will clear all browsing history, cookies, cached pagefiles, and site data.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearBrowsingData()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
