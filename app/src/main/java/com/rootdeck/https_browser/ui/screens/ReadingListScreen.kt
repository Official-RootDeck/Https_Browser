// Built by MD.Nirob
package com.rootdeck.https_browser.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rootdeck.https_browser.R
import com.rootdeck.https_browser.reader.ReaderArticle
import com.rootdeck.https_browser.readinglist.ReadingListEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingListScreen(
    items: List<ReadingListEntity>,
    canSaveCurrentPage: Boolean,
    onSaveCurrentPage: () -> Unit,
    onOpenOfflineArticle: (ReaderArticle) -> Unit,
    onOpenUrl: (String) -> Unit,
    onToggleRead: (ReadingListEntity) -> Unit,
    onDelete: (ReadingListEntity) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onDismiss() }

    var showUnreadOnly by remember { mutableStateOf(false) }
    val filtered = remember(items, showUnreadOnly) {
        if (showUnreadOnly) items.filter { !it.isRead } else items
    }
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.new_reading_list_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("reading_list_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (canSaveCurrentPage) {
                        TextButton(
                            onClick = onSaveCurrentPage,
                            modifier = Modifier.testTag("reading_list_save_current_button")
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.new_reading_list_save), fontSize = 12.sp)
                        }
                    }
                }
            )
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("reading_list_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !showUnreadOnly,
                    onClick = { showUnreadOnly = false },
                    label = { Text("All (${items.size})") }
                )
                FilterChip(
                    selected = showUnreadOnly,
                    onClick = { showUnreadOnly = true },
                    label = { Text("Unread (${items.count { !it.isRead }})") }
                )
            }

            if (filtered.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "No saved pages in your Reading List.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val paragraphs = item.contentText
                                        .split("\n\n")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                    onOpenOfflineArticle(
                                        ReaderArticle(
                                            title = item.title,
                                            byline = "Saved offline • ${dateFormat.format(Date(item.savedAt))}",
                                            siteName = item.url,
                                            excerpt = item.excerpt,
                                            paragraphs = if (paragraphs.isNotEmpty()) paragraphs else listOf(item.excerpt),
                                            textContent = item.contentText,
                                            readingTimeMinutes = maxOf(1, item.contentText.split(" ").size / 200),
                                            sourceUrl = item.url
                                        )
                                    )
                                }
                                .testTag("reading_list_item_${item.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                IconButton(onClick = { onToggleRead(item) }) {
                                    Icon(
                                        imageVector = if (item.isRead) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                        contentDescription = if (item.isRead) "Mark unread" else "Mark read",
                                        tint = if (item.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.excerpt.isNotBlank()) {
                                        Text(
                                            text = item.excerpt,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "${dateFormat.format(Date(item.savedAt))} • ${item.url}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { onOpenUrl(item.url) }) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = "Open live URL")
                                }
                                IconButton(onClick = { onDelete(item) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
