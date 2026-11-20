package com.rootdeck.https_browser.pagefile

data class TabState(
    val id: String,
    val url: String,
    val title: String,
    val scrollX: Int = 0,
    val scrollY: Int = 0,
    val formInputs: Map<String, String> = emptyMap(),
    val historyStack: List<String> = emptyList(),
    val historyIndex: Int = 0,
    val faviconUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isIncognito: Boolean = false,
    val thumbnailPath: String? = null
)
