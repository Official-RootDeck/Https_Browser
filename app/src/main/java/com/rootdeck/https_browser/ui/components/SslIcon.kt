package com.rootdeck.https_browser.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.rootdeck.https_browser.ui.theme.GoogleGreen
import com.rootdeck.https_browser.ui.theme.GoogleRed

@Composable
fun SslIcon(
    url: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (icon, color, description) = when {
        url.startsWith("https://") -> Triple(Icons.Default.Lock, MaterialTheme.colorScheme.onSurfaceVariant, "Secure Connection")
        url.startsWith("http://") -> Triple(Icons.Default.Info, MaterialTheme.colorScheme.onSurfaceVariant, "Not Secure")
        url.startsWith("chrome://") -> Triple(Icons.Default.Lock, GoogleGreen, "Chrome Internal Page")
        else -> Triple(Icons.Default.Lock, MaterialTheme.colorScheme.onSurfaceVariant, "Local Page")
    }

    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = color,
        modifier = modifier
            .size(18.dp)
            .testTag("ssl_icon")
            .clickable(onClick = onClick)
    )
}
