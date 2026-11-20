package com.rootdeck.https_browser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rootdeck.https_browser.ui.theme.*

data class ShortcutItem(
    val title: String,
    val url: String,
    val icon: ImageVector,
    val color: Color
)

data class DiscoverArticle(
    val title: String,
    val source: String,
    val timeAgo: String,
    val url: String
)

@Composable
fun NewTabPage(
    isIncognito: Boolean,
    onNavigate: (String) -> Unit,
    onSearchPillClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shortcuts = listOf(
        ShortcutItem("Google", "https://www.google.com", Icons.Default.Search, GoogleBlue),
        ShortcutItem("YouTube", "https://www.youtube.com", Icons.Default.PlayArrow, GoogleRed),
        ShortcutItem("Wikipedia", "https://www.wikipedia.org", Icons.AutoMirrored.Filled.MenuBook, Color(0xFF636466)),
        ShortcutItem("GitHub", "https://github.com", Icons.Default.Code, Color(0xFF24292E)),
        ShortcutItem("Reddit", "https://www.reddit.com", Icons.Default.Forum, Color(0xFFFF4500)),
        ShortcutItem("Hacker News", "https://news.ycombinator.com", Icons.Default.Public, Color(0xFFFF6600)),
        ShortcutItem("Dino Game", "chrome://dino", Icons.Default.SportsEsports, GoogleGreen),
        ShortcutItem("DevTools", "chrome://net-internals", Icons.Default.Terminal, ChromeBlue)
    )

    val articles = listOf(
        DiscoverArticle("Chromium Engine Architecture: How Standalone Multi-Process Rendering Works", "Chromium Blog", "2h ago", "https://blog.chromium.org"),
        DiscoverArticle("Android Memory Optimization: Low-Memory Tab Pagefile Management", "Android Developers", "4h ago", "https://developer.android.com"),
        DiscoverArticle("Deep Dive into Chrome DevTools Elements & Network Inspection Protocols", "Web.dev", "6h ago", "https://web.dev"),
        DiscoverArticle("Play Offline: The Secret History of the Chrome Dinosaur Game", "Chrome Stories", "1d ago", "chrome://dino")
    )

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("new_tab_page")
    ) {
        item {
            Spacer(modifier = Modifier.height(48.dp))

            if (isIncognito) {
                // Incognito Header
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = "Incognito",
                    tint = IncognitoIcon,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "You've gone Incognito",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = IncognitoText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Now you can browse privately, and other people who use this device won't see your activity.",
                    fontSize = 13.sp,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
            } else {
                // Google Wordmark (styled multi-color)
                val googleText = buildAnnotatedString {
                    withStyle(SpanStyle(color = GoogleBlue)) { append("G") }
                    withStyle(SpanStyle(color = GoogleRed)) { append("o") }
                    withStyle(SpanStyle(color = GoogleYellow)) { append("o") }
                    withStyle(SpanStyle(color = GoogleBlue)) { append("g") }
                    withStyle(SpanStyle(color = GoogleGreen)) { append("l") }
                    withStyle(SpanStyle(color = GoogleRed)) { append("e") }
                }
                Text(
                    text = googleText,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                )

                Spacer(modifier = Modifier.height(28.dp))
            }

            // Search Pill (56dp height, 28dp radius)
            Surface(
                shape = SearchPillShape,
                color = if (isIncognito) IncognitoSurfaceVariant else MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 20.dp)
                    .clip(SearchPillShape)
                    .clickable(onClick = onSearchPillClick)
                    .testTag("ntp_search_pill")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Search or type URL",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Shortcuts Grid (4x2)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                for (row in 0..1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (col in 0..3) {
                            val index = row * 4 + col
                            if (index < shortcuts.size) {
                                val s = shortcuts[index]
                                ShortcutItemView(shortcut = s, onClick = { onNavigate(s.url) })
                            }
                        }
                    }
                    if (row == 0) Spacer(modifier = Modifier.height(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Dino Game Banner Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isIncognito) IncognitoSurfaceVariant else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onNavigate("chrome://dino") }
                    .testTag("dino_game_banner_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(GoogleGreen.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = "Dino Game",
                            tint = GoogleGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Play Chrome T-Rex Dino Game",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Offline runner with obstacles and high scores",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Play",
                        tint = GoogleGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Discover Feed Section (if not incognito)
        if (!isIncognito) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Discover",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Customize feed",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            items(articles.size) { i ->
                val article = articles[i]
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNavigate(article.url) }
                        .testTag("discover_card_$i")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = article.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 19.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = article.source,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = " • ${article.timeAgo}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun ShortcutItemView(
    shortcut: ShortcutItem,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag("shortcut_${shortcut.title.lowercase()}")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                imageVector = shortcut.icon,
                contentDescription = shortcut.title,
                tint = shortcut.color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = shortcut.title,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}
