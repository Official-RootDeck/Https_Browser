package com.rootdeck.https_browser.engine

sealed class ChromeUrlResult {
    data class LoadInternalAsset(val assetUrl: String) : ChromeUrlResult()
    data class NavigateRoute(val route: String) : ChromeUrlResult()
    object NewTab : ChromeUrlResult()
    object None : ChromeUrlResult()
}

object ChromeURLInterceptor {

    const val SCHEME_CHROME = "chrome://"

    fun intercept(rawUrl: String): ChromeUrlResult {
        val lower = rawUrl.lowercase().trim()
        if (!lower.startsWith(SCHEME_CHROME)) {
            return ChromeUrlResult.None
        }

        val target = lower.removePrefix(SCHEME_CHROME).removeSuffix("/")

        return when (target) {
            "newtab", "new-tab" -> ChromeUrlResult.NewTab
            "dino" -> ChromeUrlResult.LoadInternalAsset("file:///android_asset/dino/index.html")
            "settings" -> ChromeUrlResult.NavigateRoute("settings")
            "history" -> ChromeUrlResult.NavigateRoute("history")
            "bookmarks" -> ChromeUrlResult.NavigateRoute("bookmarks")
            "downloads" -> ChromeUrlResult.NavigateRoute("downloads")
            "flags" -> ChromeUrlResult.NavigateRoute("flags")
            "net-internals", "version" -> ChromeUrlResult.NavigateRoute("net_internals")
            else -> ChromeUrlResult.None
        }
    }
}
