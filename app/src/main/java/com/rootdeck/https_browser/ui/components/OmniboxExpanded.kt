package com.rootdeck.https_browser.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rootdeck.https_browser.data.BookmarkEntity
import com.rootdeck.https_browser.data.HistoryEntity
import com.rootdeck.https_browser.ui.theme.OmniboxShape

data class SearchSuggestion(
    val title: String,
    val subtitle: String,
    val targetUrl: String,
    val type: SuggestionType
) {
    enum class SuggestionType {
        SEARCH, URL, HISTORY, BOOKMARK, INTERNAL
    }
}

@Composable
fun OmniboxExpanded(
    initialUrl: String,
    historyList: List<HistoryEntity>,
    bookmarksList: List<BookmarkEntity>,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onDismiss() }

    var query by remember {
        mutableStateOf(
            if (initialUrl.startsWith("chrome://newtab") || initialUrl == "about:blank") "" else initialUrl
        )
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val suggestions by remember(query, historyList, bookmarksList) {
        derivedStateOf {
            buildSuggestions(query, historyList, bookmarksList)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .testTag("omnibox_expanded_screen")
    ) {
        // Search header bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("omnibox_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search or type URL", fontSize = 15.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        if (query.isNotBlank()) {
                            onNavigate(resolveQuery(query))
                        }
                    }
                ),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear text", modifier = Modifier.size(20.dp))
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = OmniboxShape,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .focusRequester(focusRequester)
                    .testTag("omnibox_input_field")
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // Suggestions List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(suggestions) { item ->
                SuggestionRow(
                    suggestion = item,
                    onClick = { onNavigate(item.targetUrl) }
                )
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    modifier = Modifier.padding(start = 56.dp)
                )
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: SearchSuggestion,
    onClick: () -> Unit
) {
    val (icon, tint) = when (suggestion.type) {
        SearchSuggestion.SuggestionType.SEARCH -> Icons.Default.Search to MaterialTheme.colorScheme.primary
        SearchSuggestion.SuggestionType.URL -> Icons.Default.Language to MaterialTheme.colorScheme.onSurfaceVariant
        SearchSuggestion.SuggestionType.HISTORY -> Icons.Default.History to MaterialTheme.colorScheme.onSurfaceVariant
        SearchSuggestion.SuggestionType.BOOKMARK -> Icons.Default.Bookmark to MaterialTheme.colorScheme.primary
        SearchSuggestion.SuggestionType.INTERNAL -> Icons.Default.Settings to MaterialTheme.colorScheme.primary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("suggestion_row_${suggestion.type.name.lowercase()}")
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (suggestion.subtitle.isNotEmpty()) {
                Text(
                    text = suggestion.subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Navigate",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun resolveQuery(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.startsWith("chrome://") || trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("file:///")) {
        return trimmed
    }
    // Check if looks like a domain name (e.g. google.com, example.org)
    if (trimmed.contains(".") && !trimmed.contains(" ") && trimmed.length > 3) {
        return "https://$trimmed"
    }
    // Otherwise Google search query
    return "https://www.google.com/search?q=" + android.net.Uri.encode(trimmed)
}

private fun buildSuggestions(
    query: String,
    history: List<HistoryEntity>,
    bookmarks: List<BookmarkEntity>
): List<SearchSuggestion> {
    val list = mutableListOf<SearchSuggestion>()
    val q = query.trim()

    if (q.isNotEmpty()) {
        val resolved = resolveQuery(q)
        if (resolved.startsWith("https://www.google.com/search")) {
            list.add(
                SearchSuggestion(
                    title = q,
                    subtitle = "Google Search",
                    targetUrl = resolved,
                    type = SearchSuggestion.SuggestionType.SEARCH
                )
            )
        } else {
            list.add(
                SearchSuggestion(
                    title = resolved,
                    subtitle = "Open URL",
                    targetUrl = resolved,
                    type = SearchSuggestion.SuggestionType.URL
                )
            )
        }

        // Matching bookmarks
        bookmarks.filter { it.title.contains(q, ignoreCase = true) || it.url.contains(q, ignoreCase = true) }
            .take(3)
            .forEach {
                list.add(
                    SearchSuggestion(
                        title = it.title.ifEmpty { it.url },
                        subtitle = it.url,
                        targetUrl = it.url,
                        type = SearchSuggestion.SuggestionType.BOOKMARK
                    )
                )
            }

        // Matching history
        history.filter { it.title.contains(q, ignoreCase = true) || it.url.contains(q, ignoreCase = true) }
            .take(4)
            .forEach {
                list.add(
                    SearchSuggestion(
                        title = it.title.ifEmpty { it.url },
                        subtitle = it.url,
                        targetUrl = it.url,
                        type = SearchSuggestion.SuggestionType.HISTORY
                    )
                )
            }
    } else {
        // Quick suggestions when empty
        list.add(SearchSuggestion("Google", "https://www.google.com", "https://www.google.com", SearchSuggestion.SuggestionType.URL))
        list.add(SearchSuggestion("YouTube", "https://www.youtube.com", "https://www.youtube.com", SearchSuggestion.SuggestionType.URL))
        list.add(SearchSuggestion("Wikipedia", "https://www.wikipedia.org", "https://www.wikipedia.org", SearchSuggestion.SuggestionType.URL))
        list.add(SearchSuggestion("Chrome Dino Game", "chrome://dino", "chrome://dino", SearchSuggestion.SuggestionType.INTERNAL))
        list.add(SearchSuggestion("Chrome Flags", "chrome://flags", "chrome://flags", SearchSuggestion.SuggestionType.INTERNAL))
        list.add(SearchSuggestion("Net Internals", "chrome://net-internals", "chrome://net-internals", SearchSuggestion.SuggestionType.INTERNAL))
    }
    return list
}
