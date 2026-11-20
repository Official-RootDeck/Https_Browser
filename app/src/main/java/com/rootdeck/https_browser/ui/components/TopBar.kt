package com.rootdeck.https_browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rootdeck.https_browser.ui.theme.OmniboxBgDark
import com.rootdeck.https_browser.ui.theme.OmniboxBgLight
import com.rootdeck.https_browser.ui.theme.OmniboxShape

@Composable
fun TopBar(
    currentUrl: String,
    pageTitle: String,
    isLoading: Boolean,
    progress: Float,
    tabCount: Int,
    isIncognito: Boolean,
    onOmniboxClick: () -> Unit,
    onHomeClick: () -> Unit,
    onReloadClick: () -> Unit,
    onStopClick: () -> Unit,
    onTabSwitcherClick: () -> Unit,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onHistory: () -> Unit,
    onBookmarks: () -> Unit,
    onDownloads: () -> Unit,
    onDevTools: () -> Unit,
    onSettings: () -> Unit,
    onFlags: () -> Unit,
    onNetInternals: () -> Unit,
    onDinoGame: () -> Unit,
    onPasswords: () -> Unit = {},
    onReaderMode: () -> Unit = {},
    onCapturePage: () -> Unit = {},
    onReadingList: () -> Unit = {},
    onSessions: () -> Unit = {},
    onEnterPip: () -> Unit = {},
    onAbout: () -> Unit,
    onContactDev: () -> Unit,
    onSupportGitHub: () -> Unit,
    onCloseAllTabs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val omniboxBg = if (isIncognito) OmniboxBgDark else {
        if (androidx.compose.foundation.isSystemInDarkTheme()) OmniboxBgDark else OmniboxBgLight
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("chrome_top_bar")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 4.dp)
        ) {
            // Home button
            IconButton(
                onClick = onHomeClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("home_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Omnibox pill (36dp height, 18dp radius)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(OmniboxShape)
                    .background(omniboxBg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = onOmniboxClick
                    )
                    .padding(horizontal = 12.dp)
                    .testTag("omnibox_container")
            ) {
                SslIcon(url = currentUrl)

                Spacer(modifier = Modifier.width(8.dp))

                val displayText = formatUrlForDisplay(currentUrl, pageTitle)
                Text(
                    text = displayText,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = if (isLoading) onStopClick else onReloadClick,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("reload_stop_button")
                ) {
                    Icon(
                        imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                        contentDescription = if (isLoading) "Stop" else "Reload",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Tab Switcher Button
            TabSwitcherButton(
                tabCount = tabCount,
                onClick = onTabSwitcherClick
            )

            // Overflow Menu
            OverflowMenu(
                isIncognito = isIncognito,
                onNewTab = onNewTab,
                onNewIncognitoTab = onNewIncognitoTab,
                onHistory = onHistory,
                onBookmarks = onBookmarks,
                onDownloads = onDownloads,
                onDevTools = onDevTools,
                onSettings = onSettings,
                onFlags = onFlags,
                onNetInternals = onNetInternals,
                onDinoGame = onDinoGame,
                onPasswords = onPasswords,
                onReaderMode = onReaderMode,
                onCapturePage = onCapturePage,
                onReadingList = onReadingList,
                onSessions = onSessions,
                onEnterPip = onEnterPip,
                onAbout = onAbout,
                onContactDev = onContactDev,
                onSupportGitHub = onSupportGitHub,
                onCloseAllTabs = onCloseAllTabs
            )
        }

        // Linear Progress bar when loading
        if (isLoading && progress < 1f) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        } else {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        }
    }
}

private fun formatUrlForDisplay(url: String, title: String): String {
    if (url.isEmpty() || url == "about:blank" || url.startsWith("chrome://newtab")) {
        return "Search or type URL"
    }
    if (url.startsWith("chrome://")) {
        return url
    }
    return try {
        val uri = android.net.Uri.parse(url)
        uri.host ?: url
    } catch (e: Exception) {
        url
    }
}
