// Built by MD.Nirob
package com.rootdeck.https_browser.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rootdeck.https_browser.R
import com.rootdeck.https_browser.reader.ReaderArticle

private enum class ReaderThemeMode(val bg: Color, val fg: Color, val secondary: Color, val label: String) {
    DARK(Color(0xFF0F0F0F), Color(0xFFE8EAED), Color(0xFF9AA0A6), "Dark"),
    SEPIA(Color(0xFFF4ECD8), Color(0xFF3E3022), Color(0xFF6B5A48), "Sepia"),
    LIGHT(Color(0xFFFFFFFF), Color(0xFF202124), Color(0xFF5F6368), "Light")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderModeScreen(
    article: ReaderArticle,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onDismiss() }

    var fontSizeSp by remember { mutableFloatStateOf(17f) }
    var themeMode by remember { mutableStateOf(ReaderThemeMode.DARK) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.new_reader_mode_title),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = themeMode.fg
                        )
                        if (article.siteName.isNotBlank()) {
                            Text(
                                text = "${article.siteName} • ${article.readingTimeMinutes} min read",
                                fontSize = 11.sp,
                                color = themeMode.secondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("reader_mode_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit Reader Mode",
                            tint = themeMode.fg
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { fontSizeSp = if (fontSizeSp >= 22f) 14f else fontSizeSp + 2f },
                        modifier = Modifier.testTag("reader_font_size_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "Adjust font size",
                            tint = themeMode.fg
                        )
                    }
                    IconButton(
                        onClick = {
                            val nextOrdinal = (themeMode.ordinal + 1) % ReaderThemeMode.entries.size
                            themeMode = ReaderThemeMode.entries[nextOrdinal]
                        },
                        modifier = Modifier.testTag("reader_theme_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Switch reader theme (${themeMode.label})",
                            tint = themeMode.fg
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeMode.bg
                )
            )
        },
        containerColor = themeMode.bg,
        modifier = modifier
            .fillMaxSize()
            .testTag("reader_mode_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(themeMode.bg)
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = article.title,
                    fontSize = (fontSizeSp + 7f).sp,
                    lineHeight = (fontSizeSp + 13f).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = themeMode.fg
                )
                if (article.byline.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = article.byline,
                        fontSize = 13.sp,
                        color = themeMode.secondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = themeMode.secondary.copy(alpha = 0.25f))
            }

            items(article.paragraphs) { paragraph ->
                Text(
                    text = paragraph,
                    fontSize = fontSizeSp.sp,
                    lineHeight = (fontSizeSp * 1.55f).sp,
                    fontFamily = FontFamily.Serif,
                    color = themeMode.fg
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
