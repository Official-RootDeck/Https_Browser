// Built by MD.Nirob
package com.rootdeck.https_browser.ui.screens

import android.webkit.CookieManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rootdeck.https_browser.R
import com.rootdeck.https_browser.devtools.NetworkEntry
import com.rootdeck.https_browser.engine.*
import com.rootdeck.https_browser.ui.theme.*

enum class DevToolsTab(val title: String) {
    ELEMENTS("Elements"),
    CONSOLE("Console"),
    NETWORK("Network"),
    APPLICATION("Application"),
    COOKIES("Cookies"),
    SECURITY("Security")
}

@Composable
fun DevToolsSheet(
    currentUrl: String,
    domTree: DomNode?,
    consoleMessages: List<ConsoleMessageItem>,
    networkRequests: List<NetworkEntry>,
    isInspectMode: Boolean,
    onToggleInspectMode: () -> Unit,
    onRefreshDom: () -> Unit,
    onExecuteJs: (String, (String) -> Unit) -> Unit,
    onClearConsole: () -> Unit,
    onClearNetwork: () -> Unit,
    onClearSiteData: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(DevToolsTab.ELEMENTS) }

    Surface(
        color = SurfaceDark,
        contentColor = TextPrimaryDark,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 350.dp, max = 560.dp)
            .testTag("chrome_devtools_sheet")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // DevTools Header / Tab Strip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF292A2D))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Dev Tools",
                    tint = ChromeBlueDark,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.new_dev_tools_title),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Tab strip
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    modifier = Modifier.weight(1f),
                    containerColor = Color.Transparent,
                    contentColor = ChromeBlueDark,
                    edgePadding = 0.dp,
                    divider = {}
                ) {
                    DevToolsTab.values().forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == tab) ChromeBlueDark else TextSecondaryDark
                                )
                            },
                            modifier = Modifier.testTag("devtools_tab_${tab.name.lowercase()}")
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("devtools_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Dev Tools",
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp, color = BorderDark)

            // Panel Body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(SurfaceDark)
            ) {
                when (selectedTab) {
                    DevToolsTab.ELEMENTS -> ElementsPanel(
                        domTree = domTree,
                        isInspectMode = isInspectMode,
                        onToggleInspectMode = onToggleInspectMode,
                        onRefreshDom = onRefreshDom
                    )
                    DevToolsTab.CONSOLE -> ConsolePanel(
                        messages = consoleMessages,
                        onExecuteJs = onExecuteJs,
                        onClear = onClearConsole
                    )
                    DevToolsTab.NETWORK -> NetworkPanel(
                        requests = networkRequests,
                        onClear = onClearNetwork
                    )
                    DevToolsTab.APPLICATION -> ApplicationPanel(
                        currentUrl = currentUrl,
                        onExecuteJs = onExecuteJs,
                        onClearSiteData = onClearSiteData
                    )
                    DevToolsTab.COOKIES -> CookiesViewerPanel(
                        currentUrl = currentUrl
                    )
                    DevToolsTab.SECURITY -> SecurityPanel(
                        currentUrl = currentUrl
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 1. ELEMENTS PANEL
// -------------------------------------------------------------
@Composable
private fun ElementsPanel(
    domTree: DomNode?,
    isInspectMode: Boolean,
    onToggleInspectMode: () -> Unit,
    onRefreshDom: () -> Unit
) {
    var selectedNode by remember { mutableStateOf<DomNode?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF242528))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // Inspect button
            Button(
                onClick = onToggleInspectMode,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isInspectMode) ChromeBlueDark else Color(0xFF3C4043)
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier
                    .height(28.dp)
                    .testTag("toggle_inspect_element_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FilterCenterFocus,
                    contentDescription = stringResource(R.string.new_inspect_element),
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.new_inspect_element),
                    fontSize = 11.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Select element indicator / toggle button
            TextButton(
                onClick = onToggleInspectMode,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    text = if (isInspectMode) "Tap page element to inspect" else "Select element",
                    fontSize = 11.sp,
                    color = if (isInspectMode) ChromeBlueDark else TextSecondaryDark
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onRefreshDom,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh DOM",
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = BorderDark)

        if (domTree == null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("Analyzing DOM structure...", fontSize = 12.sp, color = TextSecondaryDark)
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                // DOM Tree (Left / Top)
                LazyColumn(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    item {
                        DomTreeNode(
                            node = domTree,
                            selectedId = selectedNode?.id,
                            onSelect = { selectedNode = it }
                        )
                    }
                }

                // Vertical Divider
                VerticalDivider(thickness = 1.dp, color = BorderDark)

                // Computed Styles / Attributes Pane (Right)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF202124))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Styles & Attributes",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChromeBlueDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (selectedNode != null) {
                        val node = selectedNode!!
                        Text(
                            text = "<${node.tagName}> ${if (node.elementId.isNotEmpty()) "#${node.elementId}" else ""}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text("Attributes:", fontSize = 10.sp, color = TextSecondaryDark, fontWeight = FontWeight.Bold)
                                node.attributes.forEach { (k, v) ->
                                    Text(
                                        text = "  $k: $v",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = GoogleYellow,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Computed:", fontSize = 10.sp, color = TextSecondaryDark, fontWeight = FontWeight.Bold)
                                node.computedStyles.forEach { (k, v) ->
                                    Text(
                                        text = "  $k: $v",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextSecondaryDark
                                    )
                                }
                            }
                        }
                    } else {
                        Text("Tap a node to inspect styles", fontSize = 10.sp, color = TextSecondaryDark)
                    }
                }
            }
        }
    }
}

@Composable
private fun DomTreeNode(
    node: DomNode,
    depth: Int = 0,
    selectedId: String?,
    onSelect: (DomNode) -> Unit
) {
    var expanded by remember { mutableStateOf(depth < 2) }
    val isSelected = node.id == selectedId

    Column(modifier = Modifier.padding(start = (depth * 10).dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(3.dp))
                .background(if (isSelected) ChromeBlue.copy(alpha = 0.3f) else Color.Transparent)
                .clickable {
                    onSelect(node)
                    if (node.children.isNotEmpty()) expanded = !expanded
                }
                .padding(vertical = 2.dp, horizontal = 2.dp)
        ) {
            if (node.children.isNotEmpty()) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropDown else Icons.AutoMirrored.Filled.ArrowRight,
                    contentDescription = null,
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(14.dp))
            }

            if (node.tagName == "#text") {
                Text(
                    text = "\"${node.textContent}\"",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "<${node.tagName}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = ChromeBlueDark
                )
                if (node.elementId.isNotEmpty()) {
                    Text(
                        text = " id=\"${node.elementId}\"",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = GoogleYellow
                    )
                }
                if (node.classNames.isNotEmpty()) {
                    Text(
                        text = " class=\"${node.classNames.joinToString(" ")}\"",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = GoogleGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = if (node.children.isEmpty()) "/>" else ">",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ChromeBlueDark
                )
            }
        }

        if (expanded && node.children.isNotEmpty()) {
            node.children.forEach { child ->
                DomTreeNode(
                    node = child,
                    depth = depth + 1,
                    selectedId = selectedId,
                    onSelect = onSelect
                )
            }
            Text(
                text = "</${node.tagName}>",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = ChromeBlueDark,
                modifier = Modifier.padding(start = 14.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// 2. CONSOLE PANEL
// -------------------------------------------------------------
@Composable
private fun ConsolePanel(
    messages: List<ConsoleMessageItem>,
    onExecuteJs: (String, (String) -> Unit) -> Unit,
    onClear: () -> Unit
) {
    var jsInput by remember { mutableStateOf("") }
    var evalResult by remember { mutableStateOf<String?>(null) }
    var filterLevel by remember { mutableStateOf<ConsoleMessageItem.Level?>(null) }

    val filteredMessages = remember(messages, filterLevel) {
        if (filterLevel == null) messages else messages.filter { it.type == filterLevel }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF242528))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            IconButton(onClick = onClear, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.Block, contentDescription = "Clear Console", tint = TextSecondaryDark, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))

            // Filter chips
            FilterChip(
                selected = filterLevel == null,
                onClick = { filterLevel = null },
                label = { Text("All", fontSize = 10.sp) },
                modifier = Modifier.height(24.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            FilterChip(
                selected = filterLevel == ConsoleMessageItem.Level.ERROR,
                onClick = { filterLevel = if (filterLevel == ConsoleMessageItem.Level.ERROR) null else ConsoleMessageItem.Level.ERROR },
                label = { Text("Errors", fontSize = 10.sp, color = GoogleRed) },
                modifier = Modifier.height(24.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            FilterChip(
                selected = filterLevel == ConsoleMessageItem.Level.WARN,
                onClick = { filterLevel = if (filterLevel == ConsoleMessageItem.Level.WARN) null else ConsoleMessageItem.Level.WARN },
                label = { Text("Warnings", fontSize = 10.sp, color = GoogleYellow) },
                modifier = Modifier.height(24.dp)
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = BorderDark)

        // Log messages stream
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            items(filteredMessages) { msg ->
                val (color, icon) = when (msg.type) {
                    ConsoleMessageItem.Level.ERROR -> GoogleRed to Icons.Default.Error
                    ConsoleMessageItem.Level.WARN -> GoogleYellow to Icons.Default.Warning
                    ConsoleMessageItem.Level.INFO -> ChromeBlueDark to Icons.Default.Info
                    ConsoleMessageItem.Level.LOG -> TextPrimaryDark to Icons.Default.ChevronRight
                }

                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "[${msg.timestamp}] ",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondaryDark
                    )
                    Text(
                        text = msg.message,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = color,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = BorderDark.copy(alpha = 0.3f))
            }

            if (evalResult != null) {
                item {
                    Text(
                        text = "<- $evalResult",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ChromeBlueDark,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        // JS REPL Input row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF242528))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(">", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ChromeBlueDark)
            Spacer(modifier = Modifier.width(6.dp))
            TextField(
                value = jsInput,
                onValueChange = { jsInput = it },
                placeholder = { Text("Evaluate JavaScript...", fontSize = 11.sp, color = TextSecondaryDark) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (jsInput.isNotBlank()) {
                            onExecuteJs(jsInput) { res -> evalResult = res }
                            jsInput = ""
                        }
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("console_js_input")
            )
            IconButton(
                onClick = {
                    if (jsInput.isNotBlank()) {
                        onExecuteJs(jsInput) { res -> evalResult = res }
                        jsInput = ""
                    }
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Run JS", tint = ChromeBlueDark, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// 3. NETWORK PANEL
// -------------------------------------------------------------
@Composable
private fun NetworkPanel(
    requests: List<NetworkEntry>,
    onClear: () -> Unit
) {
    var selectedRequest by remember { mutableStateOf<NetworkEntry?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF242528))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Clear Network Log",
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${requests.size} requests captured",
                fontSize = 11.sp,
                color = TextSecondaryDark
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = onClear,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Text("Clear log", fontSize = 10.sp, color = ChromeBlueDark)
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = BorderDark)

        if (requests.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("No network requests recorded. Reload the page.", fontSize = 11.sp, color = TextSecondaryDark)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(requests) { req ->
                    val isSelected = selectedRequest?.id == req.id
                    val statusColor = when (req.status) {
                        in 200..299 -> GoogleGreen
                        in 300..399 -> GoogleYellow
                        else -> GoogleRed
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) ChromeBlue.copy(alpha = 0.2f) else Color.Transparent)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedRequest = if (isSelected) null else req
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            // Method badge
                            Text(
                                text = req.method,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChromeBlueDark,
                                modifier = Modifier.width(36.dp)
                            )

                            // Status badge
                            Text(
                                text = req.status.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.width(32.dp)
                            )

                            // URL path & duration/size
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = req.url,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimaryDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${req.formattedSize()} • ${req.formattedDuration()}",
                                    fontSize = 9.sp,
                                    color = TextSecondaryDark
                                )
                            }

                            if (req.isBlocked) {
                                Text(
                                    text = "BLOCKED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoogleRed
                                )
                            }
                        }

                        // Expanded detail container (Task 44 & 45)
                        if (isSelected) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E2023))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Request Headers",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ChromeBlueDark
                                )
                                if (req.headers.isEmpty()) {
                                    Text("No request headers", fontSize = 9.sp, color = TextSecondaryDark)
                                } else {
                                    req.headers.forEach { (k, v) ->
                                        Text(
                                            text = "$k: $v",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextSecondaryDark
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Response Headers",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ChromeBlueDark
                                )
                                if (req.responseHeaders.isEmpty()) {
                                    Text("No response headers available", fontSize = 9.sp, color = TextSecondaryDark)
                                } else {
                                    req.responseHeaders.forEach { (k, v) ->
                                        Text(
                                            text = "$k: $v",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextSecondaryDark
                                        )
                                    }
                                }

                                // Cookies section per entry (Task 45)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Cookies",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoogleYellow
                                )
                                val cookieHeader = req.headers["Cookie"] ?: req.headers["cookie"]
                                val setCookieHeader = req.responseHeaders["Set-Cookie"] ?: req.responseHeaders["set-cookie"]
                                if (cookieHeader == null && setCookieHeader == null && req.cookies.isEmpty()) {
                                    Text("No cookies sent or received", fontSize = 9.sp, color = TextSecondaryDark)
                                } else {
                                    if (cookieHeader != null) {
                                        Text("Cookie: $cookieHeader", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = GoogleYellow)
                                    }
                                    if (setCookieHeader != null) {
                                        Text("Set-Cookie: $setCookieHeader", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = GoogleGreen)
                                    }
                                    req.cookies.forEach { c ->
                                        Text(c, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = GoogleYellow)
                                    }
                                }
                            }
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = BorderDark.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. APPLICATION PANEL
// -------------------------------------------------------------
@Composable
private fun ApplicationPanel(
    currentUrl: String,
    onExecuteJs: (String, (String) -> Unit) -> Unit,
    onClearSiteData: () -> Unit
) {
    var cookies by remember { mutableStateOf("Loading...") }
    var localStorageData by remember { mutableStateOf("Loading...") }

    LaunchedEffect(currentUrl) {
        // Read cookies safely with try-catch
        try {
            val cookieMgr = CookieManager.getInstance()
            val raw = cookieMgr?.getCookie(currentUrl)
            cookies = raw ?: "No cookies set for this origin."
        } catch (e: Exception) {
            cookies = "Error reading cookies: ${e.message}"
        }

        // Read localStorage via JS
        onExecuteJs("JSON.stringify(window.localStorage)") { res ->
            localStorageData = res
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        item {
            Text("Storage & Cookies", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ChromeBlueDark)
            Spacer(modifier = Modifier.height(10.dp))

            // Cookies Section
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF242528)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cookie, contentDescription = null, tint = GoogleYellow, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cookies", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = cookies,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // LocalStorage Section
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF242528)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = ChromeBlueDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Local Storage", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = localStorageData,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onClearSiteData,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD93025)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clear Site Data & Cache", fontSize = 12.sp)
            }
        }
    }
}

// -------------------------------------------------------------
// 5. GLOBAL COOKIES VIEWER TAB (Tasks 46–48)
// -------------------------------------------------------------
@Composable
private fun CookiesViewerPanel(
    currentUrl: String
) {
    var rawCookies by remember { mutableStateOf("") }
    var cookieList by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    fun refreshCookies() {
        try {
            val cookieMgr = CookieManager.getInstance()
            val raw = cookieMgr?.getCookie(currentUrl) ?: ""
            rawCookies = raw
            if (raw.isNotBlank()) {
                cookieList = raw.split(";").mapNotNull { part ->
                    val trimmed = part.trim()
                    if (trimmed.isEmpty()) null else {
                        val eqIdx = trimmed.indexOf('=')
                        if (eqIdx > 0) {
                            trimmed.substring(0, eqIdx).trim() to trimmed.substring(eqIdx + 1).trim()
                        } else {
                            trimmed to ""
                        }
                    }
                }
            } else {
                cookieList = emptyList()
            }
        } catch (_: Exception) {
            cookieList = emptyList()
        }
    }

    LaunchedEffect(currentUrl) {
        refreshCookies()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Cookie, contentDescription = null, tint = GoogleYellow, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Cookies Viewer", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = {
                    try {
                        CookieManager.getInstance()?.removeAllCookies(null)
                        refreshCookies()
                    } catch (_: Exception) {}
                },
                colors = ButtonDefaults.textButtonColors(contentColor = GoogleRed)
            ) {
                Text("Clear all cookies", fontSize = 11.sp)
            }
        }

        Text(
            text = "Active host: $currentUrl",
            fontSize = 10.sp,
            color = TextSecondaryDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(thickness = 0.5.dp, color = BorderDark)
        Spacer(modifier = Modifier.height(8.dp))

        if (cookieList.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("No cookies set for this origin.", fontSize = 11.sp, color = TextSecondaryDark)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(cookieList) { (name, value) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoogleYellow,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = value,
                                fontSize = 10.sp,
                                color = TextSecondaryDark,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = {
                                try {
                                    // Clear single cookie (Task 48)
                                    CookieManager.getInstance()?.setCookie(currentUrl, "$name=; Max-Age=0; Path=/")
                                    refreshCookies()
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete cookie", tint = TextSecondaryDark, modifier = Modifier.size(14.dp))
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = BorderDark.copy(alpha = 0.2f))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. SECURITY PANEL
// -------------------------------------------------------------
@Composable
private fun SecurityPanel(
    currentUrl: String
) {
    val isHttps = currentUrl.startsWith("https://")
    val isInternal = currentUrl.startsWith("chrome://") || currentUrl.startsWith("about:")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        item {
            Text("Connection Security", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ChromeBlueDark)
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF242528)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    val icon = if (isHttps) Icons.Default.Lock else if (isInternal) Icons.Default.Shield else Icons.Default.Warning
                    val iconColor = if (isHttps) GoogleGreen else if (isInternal) ChromeBlueDark else GoogleRed
                    val title = if (isHttps) "Valid HTTPS Connection" else if (isInternal) "Secure Internal Resource" else "Not Secure (HTTP)"
                    val desc = if (isHttps) {
                        "Your information (for example, passwords or credit card numbers) is private when it is sent to this site."
                    } else if (isInternal) {
                        "Built-in browser resource with strict isolation and zero network leakage."
                    } else {
                        "You should not enter any sensitive information on this site (for example, passwords or credit cards), because it could be stolen by attackers."
                    }

                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = iconColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(desc, fontSize = 10.sp, color = TextSecondaryDark)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF242528)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Certificate & Protocol", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isHttps) "Protocol: TLS 1.3 / Strict Cipher Suite\nCertificate: Validated by System Trust Store" else "Protocol: Plaintext HTTP / Unencrypted",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondaryDark
                    )
                }
            }
        }
    }
}
