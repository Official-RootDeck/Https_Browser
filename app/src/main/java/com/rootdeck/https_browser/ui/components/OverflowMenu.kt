// Built by MD.Nirob
package com.rootdeck.https_browser.ui.components

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rootdeck.https_browser.R

@Composable
fun OverflowMenu(
    isIncognito: Boolean,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onHistory: () -> Unit,
    onBookmarks: () -> Unit,
    onDownloads: () -> Unit,
    onDevTools: () -> Unit,
    onSettings: () -> Unit,
    onFlags: () -> Unit,
    onNetInternals: () -> Unit,
    onDinoGame: () -> Unit = {},
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
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .size(40.dp)
                .testTag("overflow_menu_button")
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Chrome menu",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.testTag("chrome_overflow_dropdown")
        ) {
            DropdownMenuItem(
                text = { Text("New tab") },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = {
                    expanded = false
                    onNewTab()
                }
            )
            DropdownMenuItem(
                text = { Text("New Incognito tab") },
                leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                onClick = {
                    expanded = false
                    onNewIncognitoTab()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("History") },
                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                onClick = {
                    expanded = false
                    onHistory()
                }
            )
            DropdownMenuItem(
                text = { Text("Bookmarks") },
                leadingIcon = { Icon(Icons.Default.BookmarkBorder, contentDescription = null) },
                onClick = {
                    expanded = false
                    onBookmarks()
                }
            )
            DropdownMenuItem(
                text = { Text("Downloads") },
                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDownloads()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.new_menu_reading_list)) },
                leadingIcon = { Icon(Icons.Default.Bookmarks, contentDescription = null) },
                onClick = {
                    expanded = false
                    onReadingList()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.new_menu_sessions)) },
                leadingIcon = { Icon(Icons.Default.Tab, contentDescription = null) },
                onClick = {
                    expanded = false
                    onSessions()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.new_menu_reader_mode)) },
                leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                onClick = {
                    expanded = false
                    onReaderMode()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.new_menu_capture_page)) },
                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                onClick = {
                    expanded = false
                    onCapturePage()
                }
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.new_menu_pip)) },
                    leadingIcon = { Icon(Icons.Default.PictureInPictureAlt, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onEnterPip()
                    }
                )
            }
            HorizontalDivider()
            // Tasks 27-28: Dev Tools label using string resource
            DropdownMenuItem(
                text = { Text(stringResource(R.string.new_dev_tools_title)) },
                leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDevTools()
                }
            )
            DropdownMenuItem(
                text = { Text("Flags (chrome://flags)") },
                leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                onClick = {
                    expanded = false
                    onFlags()
                }
            )
            DropdownMenuItem(
                text = { Text("Net Internals") },
                leadingIcon = { Icon(Icons.Default.Lan, contentDescription = null) },
                onClick = {
                    expanded = false
                    onNetInternals()
                }
            )
            // Tasks 83-85: Dino game menu item is removed from the menu.
            // Tasks 123-128: Google Password Manager entry feature-gated for API >= 26
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.new_menu_passwords)) },
                    leadingIcon = { Icon(Icons.Default.Password, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onPasswords()
                    }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Settings") },
                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                onClick = {
                    expanded = false
                    onSettings()
                }
            )
            DropdownMenuItem(
                text = { Text("About") },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                onClick = {
                    expanded = false
                    onAbout()
                }
            )
            DropdownMenuItem(
                text = { Text("Contact Dev") },
                leadingIcon = { Icon(Icons.Default.Public, contentDescription = null) },
                onClick = {
                    expanded = false
                    onContactDev()
                }
            )
            DropdownMenuItem(
                text = { Text("Rate on GitHub ⭐") },
                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFFFBBC05)) },
                onClick = {
                    expanded = false
                    onSupportGitHub()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Close all tabs") },
                leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                onClick = {
                    expanded = false
                    onCloseAllTabs()
                }
            )
        }
    }
}
