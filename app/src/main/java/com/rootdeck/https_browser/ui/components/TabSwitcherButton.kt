package com.rootdeck.https_browser.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TabSwitcherButton(
    tabCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayCount = if (tabCount > 99) ":D" else tabCount.toString()
    val shape = RoundedCornerShape(5.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(36.dp)
            .testTag("tab_switcher_button")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 20.dp),
                onClick = onClick
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(20.dp)
                .border(2.dp, MaterialTheme.colorScheme.onSurface, shape)
        ) {
            Text(
                text = displayCount,
                fontSize = if (displayCount.length > 1) 10.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 11.sp
            )
        }
    }
}
