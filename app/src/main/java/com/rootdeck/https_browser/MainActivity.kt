// Built by MD.Nirob
package com.rootdeck.https_browser

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rootdeck.https_browser.capture.FullPageCapture
import com.rootdeck.https_browser.engine.ChromeUrlResult
import com.rootdeck.https_browser.pdf.PdfViewerActivity
import com.rootdeck.https_browser.pip.PipActionReceiver
import com.rootdeck.https_browser.pip.PipManager
import com.rootdeck.https_browser.ui.AppScreen
import com.rootdeck.https_browser.ui.BrowserViewModel
import com.rootdeck.https_browser.ui.components.AboutDialog
import com.rootdeck.https_browser.ui.components.CapturePreviewDialog
import com.rootdeck.https_browser.ui.components.EngineView
import com.rootdeck.https_browser.ui.components.GitHubSupportDialog
import com.rootdeck.https_browser.ui.components.OmniboxExpanded
import com.rootdeck.https_browser.ui.components.TopBar
import com.rootdeck.https_browser.ui.screens.*
import com.rootdeck.https_browser.ui.theme.ChromeTheme
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()
    private var activeWebView: WebView? = null
    private var isInPipState by mutableStateOf(false)
    private var isPipVideoPlaying by mutableStateOf(true)
    private var isPipVideoMuted by mutableStateOf(false)
    private var capturedBitmap by mutableStateOf<Bitmap?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Provide JS executor to ViewModel
        viewModel.jsExecutor = { script, callback ->
            activeWebView?.post {
                activeWebView?.evaluateJavascript(script) { res ->
                    callback(res ?: "")
                }
            }
        }

        // Hook PiP remote action callbacks
        PipActionReceiver.onPlayPauseRequested = {
            isPipVideoPlaying = !isPipVideoPlaying
            activeWebView?.post {
                activeWebView?.evaluateJavascript(
                    "document.querySelectorAll('video').forEach(v => v.paused ? v.play() : v.pause());",
                    null
                )
            }
            PipManager.updatePipParams(this, isPipVideoPlaying, isPipVideoMuted)
        }
        PipActionReceiver.onMuteToggleRequested = {
            isPipVideoMuted = !isPipVideoMuted
            activeWebView?.post {
                activeWebView?.evaluateJavascript(
                    "document.querySelectorAll('video').forEach(v => { v.muted = !v.muted; });",
                    null
                )
            }
            PipManager.updatePipParams(this, isPipVideoPlaying, isPipVideoMuted)
        }

        handleIncomingIntent(intent)

        setContent {
            val isIncognito by viewModel.isIncognitoMode.collectAsStateWithLifecycle()

            // Handle FLAG_SECURE for Incognito
            LaunchedEffect(isIncognito) {
                if (isIncognito) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            ChromeTheme(isIncognito = isIncognito) {
                BrowserApp(
                    viewModel = viewModel,
                    isInPipMode = isInPipState,
                    capturedBitmap = capturedBitmap,
                    onClearCapture = { capturedBitmap = null },
                    onBindWebView = { activeWebView = it },
                    onPasswords = { openPasswordManager() },
                    onCapturePage = { captureCurrentPage() },
                    onEnterPip = { enterVideoPip(manual = true) }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PipActionReceiver.onPlayPauseRequested = null
        PipActionReceiver.onMuteToggleRequested = null
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val webView = activeWebView ?: return
        if (PipManager.isPipSupported(this)) {
            webView.evaluateJavascript(PipManager.getVideoCheckScript()) { result ->
                if (result?.contains("playing") == true) {
                    isPipVideoPlaying = true
                    PipManager.enterPip(this, isPipVideoPlaying, isPipVideoMuted)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipState = isInPictureInPictureMode
    }

    private fun enterVideoPip(manual: Boolean) {
        if (!PipManager.isPipSupported(this)) {
            if (manual) {
                Toast.makeText(this, getString(R.string.new_pip_unavailable), Toast.LENGTH_SHORT).show()
            }
            return
        }
        isPipVideoPlaying = true
        val entered = PipManager.enterPip(this, isPipVideoPlaying, isPipVideoMuted)
        if (!entered && manual) {
            Toast.makeText(this, getString(R.string.new_pip_unavailable), Toast.LENGTH_SHORT).show()
        }
    }

    private fun captureCurrentPage() {
        val webView = activeWebView
        if (webView == null || viewModel.currentUrl.value.startsWith("chrome://")) {
            Toast.makeText(this, getString(R.string.new_capture_failed), Toast.LENGTH_SHORT).show()
            return
        }
        val bmp = FullPageCapture.captureFullPageBitmap(webView)
        if (bmp != null) {
            capturedBitmap = bmp
        } else {
            Toast.makeText(this, getString(R.string.new_capture_failed), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val uri = intent.data
        if (Intent.ACTION_VIEW == action && uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            val mimeType = intent.type ?: contentResolver.getType(uri) ?: ""
            val uriStr = uri.toString()
            val fileName = uri.lastPathSegment ?: ""

            when {
                // Tasks 66 & 72: .link file
                fileName.endsWith(".link", ignoreCase = true) || uriStr.endsWith(".link", ignoreCase = true) -> {
                    try {
                        contentResolver.openInputStream(uri)?.use { stream ->
                            val reader = BufferedReader(InputStreamReader(stream))
                            var firstUrl: String? = null
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                val trimmed = line?.trim() ?: ""
                                if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                                    firstUrl = trimmed
                                    break
                                }
                            }
                            if (!firstUrl.isNullOrEmpty()) {
                                viewModel.openUrl(firstUrl)
                            } else {
                                Toast.makeText(this, getString(R.string.new_file_open_unsupported), Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, getString(R.string.new_file_open_unsupported), Toast.LENGTH_SHORT).show()
                    }
                }
                // Task 67: .html / .xhtml
                mimeType.contains("html", ignoreCase = true) || fileName.endsWith(".html", ignoreCase = true) || fileName.endsWith(".htm", ignoreCase = true) -> {
                    activeWebView?.settings?.allowFileAccessFromFileURLs = true
                    activeWebView?.settings?.allowUniversalAccessFromFileURLs = true
                    viewModel.openUrl(uriStr)
                }
                // Task 68: .mhtml / multipart/related
                mimeType.contains("mhtml", ignoreCase = true) || mimeType.contains("multipart/related", ignoreCase = true) || fileName.endsWith(".mhtml", ignoreCase = true) -> {
                    activeWebView?.settings?.allowFileAccessFromFileURLs = true
                    activeWebView?.settings?.allowUniversalAccessFromFileURLs = true
                    viewModel.openUrl(uriStr)
                }
                // PDF
                mimeType.equals("application/pdf", ignoreCase = true) || fileName.endsWith(".pdf", ignoreCase = true) -> {
                    val pdfIntent = Intent(this, PdfViewerActivity::class.java).apply {
                        data = uri
                    }
                    startActivity(pdfIntent)
                }
                uri.scheme == "http" || uri.scheme == "https" -> {
                    viewModel.openUrl(uriStr)
                }
                else -> {
                    Toast.makeText(this, getString(R.string.new_file_open_unsupported), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openPasswordManager() {
        val intents = listOf(
            Intent("android.settings.CREDENTIAL_PROVIDER"),
            Intent("android.credentials.ACTION_CREDENTIALS"),
            Intent(Settings.ACTION_SETTINGS)
        )
        var launched = false
        for (cand in intents) {
            try {
                startActivity(cand)
                launched = true
                break
            } catch (_: Exception) {}
        }
        if (!launched) {
            Toast.makeText(this, getString(R.string.new_password_manager_unavailable), Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun BrowserApp(
    viewModel: BrowserViewModel,
    isInPipMode: Boolean = false,
    capturedBitmap: Bitmap? = null,
    onClearCapture: () -> Unit = {},
    onBindWebView: (WebView?) -> Unit,
    onPasswords: () -> Unit = {},
    onCapturePage: () -> Unit = {},
    onEnterPip: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isIncognito by viewModel.isIncognitoMode.collectAsStateWithLifecycle()
    val isOmniboxExpanded by viewModel.isOmniboxExpanded.collectAsStateWithLifecycle()
    val isDevToolsOpen by viewModel.isDevToolsOpen.collectAsStateWithLifecycle()
    val isAboutOpen by viewModel.isAboutOpen.collectAsStateWithLifecycle()
    val isGitHubPromptOpen by viewModel.isGitHubPromptOpen.collectAsStateWithLifecycle()
    val isInspectMode by viewModel.isInspectMode.collectAsStateWithLifecycle()

    val currentUrl by viewModel.currentUrl.collectAsStateWithLifecycle()
    val pageTitle by viewModel.pageTitle.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()

    val standardTabs by viewModel.standardTabs.collectAsStateWithLifecycle()
    val incognitoTabs by viewModel.incognitoTabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()

    val domTree by viewModel.domTree.collectAsStateWithLifecycle()
    val consoleMessages by viewModel.consoleMessages.collectAsStateWithLifecycle()
    val networkRequests by viewModel.networkRequests.collectAsStateWithLifecycle()

    val historyList by viewModel.historyList.collectAsStateWithLifecycle()
    val bookmarksList by viewModel.bookmarksList.collectAsStateWithLifecycle()
    val readingList by viewModel.readingList.collectAsStateWithLifecycle()
    val sessionsList by viewModel.sessionsList.collectAsStateWithLifecycle()
    val readerArticle by viewModel.readerArticle.collectAsStateWithLifecycle()

    val trackingProtectionEnabled by viewModel.trackingProtectionEnabled.collectAsStateWithLifecycle()
    val blockedTrackerCount by viewModel.blockedTrackerCount.collectAsStateWithLifecycle()

    val activeTabsList = if (isIncognito) incognitoTabs else standardTabs

    // Back handling for main browser
    BackHandler(enabled = currentScreen == AppScreen.BROWSER && currentUrl != "chrome://newtab") {
        viewModel.openUrl("chrome://newtab")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (currentScreen) {
            AppScreen.TAB_SWITCHER -> {
                TabSwitcherScreen(
                    tabs = activeTabsList,
                    activeTabId = activeTabId,
                    isIncognitoMode = isIncognito,
                    onTabSelected = { viewModel.selectTab(it) },
                    onTabClosed = { viewModel.closeTab(it) },
                    onNewTab = { viewModel.newTab(it) },
                    onToggleIncognito = { viewModel.toggleIncognitoMode(it) },
                    onCloseAllTabs = { viewModel.closeAllTabs() },
                    onDismiss = { viewModel.setScreen(AppScreen.BROWSER) }
                )
            }
            AppScreen.SETTINGS -> {
                SettingsScreen(
                    trackingProtectionEnabled = trackingProtectionEnabled,
                    onToggleTrackingProtection = { viewModel.toggleTrackingProtection(it) },
                    onClearBrowsingData = { viewModel.clearAllBrowsingData() },
                    onDismiss = { viewModel.setScreen(AppScreen.BROWSER) }
                )
            }
            AppScreen.HISTORY -> {
                HistoryScreen(
                    historyList = historyList,
                    onSelectUrl = { viewModel.openUrl(it) },
                    onDeleteHistoryItem = { viewModel.deleteHistoryItem(it) },
                    onClearAllHistory = { viewModel.clearAllBrowsingData() },
                    onDismiss = { viewModel.setScreen(AppScreen.BROWSER) }
                )
            }
            AppScreen.BOOKMARKS -> {
                BookmarksScreen(
                    bookmarks = bookmarksList,
                    onSelectBookmark = { viewModel.openUrl(it) },
                    onDeleteBookmark = { viewModel.deleteBookmark(it) },
                    onDismiss = { viewModel.setScreen(AppScreen.BROWSER) }
                )
            }
            AppScreen.FLAGS -> {
                FlagsScreen(
                    onRelaunchEngine = {
                        viewModel.clearSiteData()
                        viewModel.openUrl("chrome://newtab")
                    },
                    onDismiss = { viewModel.setScreen(AppScreen.BROWSER) }
                )
            }
            AppScreen.NET_INTERNALS -> {
                NetInternalsScreen(
                    openTabCount = activeTabsList.size,
                    blockedTrackerCount = blockedTrackerCount,
                    onTrimMemoryNow = { viewModel.clearSiteData() },
                    onDismiss = { viewModel.setScreen(AppScreen.BROWSER) }
                )
            }
            AppScreen.READER_MODE -> {
                val article = readerArticle
                if (article != null) {
                    ReaderModeScreen(
                        article = article,
                        onDismiss = { viewModel.setScreen(AppScreen.BROWSER) }
                    )
                } else {
                    viewModel.setScreen(AppScreen.BROWSER)
                }
            }
            AppScreen.READING_LIST -> {
                ReadingListScreen(
                    items = readingList,
                    canSaveCurrentPage = currentUrl.isNotBlank() && !currentUrl.startsWith("chrome://"),
                    onSaveCurrentPage = {
                        viewModel.saveCurrentPageToReadingList {
                            Toast.makeText(context, context.getString(R.string.new_reading_list_saved), Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenOfflineArticle = { article -> viewModel.openOfflineArticle(article) },
                    onOpenUrl = { url -> viewModel.openUrl(url) },
                    onToggleRead = { item -> viewModel.toggleReadingListRead(item) },
                    onDelete = { item -> viewModel.deleteReadingListItem(item) },
                    onDismiss = { viewModel.setScreen(AppScreen.BROWSER) }
                )
            }
            AppScreen.SESSIONS -> {
                SessionListScreen(
                    sessions = sessionsList,
                    currentTabCount = standardTabs.size,
                    onSaveCurrentSession = { name -> viewModel.saveCurrentSession(name) },
                    onRestoreSession = { session -> viewModel.restoreSession(session) },
                    onDeleteSession = { session -> viewModel.deleteSession(session) },
                    onDismiss = { viewModel.setScreen(AppScreen.BROWSER) }
                )
            }
            AppScreen.BROWSER -> {
                Scaffold(
                    topBar = {
                        if (!isInPipMode) {
                            TopBar(
                                currentUrl = currentUrl,
                                pageTitle = pageTitle,
                                isLoading = isLoading,
                                progress = progress,
                                tabCount = activeTabsList.size,
                                isIncognito = isIncognito,
                                onOmniboxClick = { viewModel.setOmniboxExpanded(true) },
                                onHomeClick = { viewModel.openUrl("chrome://newtab") },
                                onReloadClick = { viewModel.openUrl(currentUrl) },
                                onStopClick = { viewModel.onLoadingStateChanged(false) },
                                onTabSwitcherClick = { viewModel.setScreen(AppScreen.TAB_SWITCHER) },
                                onNewTab = { viewModel.newTab(false) },
                                onNewIncognitoTab = { viewModel.newTab(true) },
                                onHistory = { viewModel.setScreen(AppScreen.HISTORY) },
                                onBookmarks = { viewModel.setScreen(AppScreen.BOOKMARKS) },
                                onDownloads = { viewModel.openUrl("chrome://downloads") },
                                onDevTools = { viewModel.toggleDevTools() },
                                onSettings = { viewModel.setScreen(AppScreen.SETTINGS) },
                                onFlags = { viewModel.setScreen(AppScreen.FLAGS) },
                                onNetInternals = { viewModel.setScreen(AppScreen.NET_INTERNALS) },
                                onDinoGame = { viewModel.openUrl("chrome://dino") },
                                onPasswords = onPasswords,
                                onReaderMode = {
                                    viewModel.enterReaderMode {
                                        Toast.makeText(context, context.getString(R.string.new_reader_extract_failed), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onCapturePage = onCapturePage,
                                onReadingList = { viewModel.setScreen(AppScreen.READING_LIST) },
                                onSessions = { viewModel.setScreen(AppScreen.SESSIONS) },
                                onEnterPip = onEnterPip,
                                onAbout = { viewModel.openAbout() },
                                onContactDev = { viewModel.openContactDev() },
                                onSupportGitHub = { viewModel.openGitHubPrompt() },
                                onCloseAllTabs = { viewModel.closeAllTabs() },
                                modifier = Modifier.statusBarsPadding()
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Main Web Content or New Tab Page
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            if (currentUrl.startsWith("chrome://newtab") || currentUrl == "about:blank") {
                                NewTabPage(
                                    isIncognito = isIncognito,
                                    onNavigate = { viewModel.openUrl(it) },
                                    onSearchPillClick = { viewModel.setOmniboxExpanded(true) }
                                )
                            } else {
                                EngineView(
                                    tabId = activeTabId,
                                    url = currentUrl,
                                    isIncognito = isIncognito,
                                    consoleInterceptor = viewModel.consoleInterceptor,
                                    networkInterceptor = viewModel.networkInterceptor,
                                    inspectElementMode = viewModel.inspectElementMode,
                                    onTitleChanged = { viewModel.onTitleChanged(it) },
                                    onProgressChanged = { viewModel.onProgressChanged(it) },
                                    onLoadingStateChanged = { viewModel.onLoadingStateChanged(it) },
                                    onUrlChanged = { viewModel.onUrlChanged(it) },
                                    onDomExtracted = { viewModel.onDomExtracted(it) },
                                    onNavigateChromeScheme = { result ->
                                        when (result) {
                                            is ChromeUrlResult.NavigateRoute -> {
                                                when (result.route) {
                                                    "settings" -> viewModel.setScreen(AppScreen.SETTINGS)
                                                    "history" -> viewModel.setScreen(AppScreen.HISTORY)
                                                    "bookmarks" -> viewModel.setScreen(AppScreen.BOOKMARKS)
                                                    "flags" -> viewModel.setScreen(AppScreen.FLAGS)
                                                    "net_internals" -> viewModel.setScreen(AppScreen.NET_INTERNALS)
                                                }
                                            }
                                            is ChromeUrlResult.LoadInternalAsset -> viewModel.openUrl(result.assetUrl)
                                            is ChromeUrlResult.NewTab -> viewModel.openUrl("chrome://newtab")
                                            is ChromeUrlResult.None -> {}
                                        }
                                    },
                                    onRenderProcessCrashed = { viewModel.onRenderProcessCrashed() },
                                    onWebViewReady = onBindWebView
                                )
                            }
                        }

                        // DevTools Bottom Drawer (Elements, Console, Network, Application, Cookies, Security)
                        if (!isInPipMode) {
                            AnimatedVisibility(
                                visible = isDevToolsOpen,
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                            ) {
                                DevToolsSheet(
                                    currentUrl = currentUrl,
                                    domTree = domTree,
                                    consoleMessages = consoleMessages,
                                    networkRequests = networkRequests,
                                    isInspectMode = isInspectMode,
                                    onToggleInspectMode = { viewModel.toggleInspectMode() },
                                    onRefreshDom = { viewModel.refreshDom() },
                                    onExecuteJs = { code, cb -> viewModel.executeJs(code, cb) },
                                    onClearConsole = { viewModel.clearConsole() },
                                    onClearNetwork = { viewModel.clearNetwork() },
                                    onClearSiteData = { viewModel.clearSiteData() },
                                    onClose = { viewModel.toggleDevTools() }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fullscreen Omnibox Expanded Suggestion view
        AnimatedVisibility(
            visible = isOmniboxExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            OmniboxExpanded(
                initialUrl = currentUrl,
                historyList = historyList,
                bookmarksList = bookmarksList,
                onNavigate = { viewModel.openUrl(it) },
                onDismiss = { viewModel.setOmniboxExpanded(false) }
            )
        }

        // About Dialog displaying about.txt
        if (isAboutOpen) {
            AboutDialog(
                onDismiss = { viewModel.closeAbout() },
                onContactDev = {
                    viewModel.closeAbout()
                    viewModel.openContactDev()
                }
            )
        }

        // GitHub Support Dialog
        if (isGitHubPromptOpen) {
            GitHubSupportDialog(
                gitHubUrl = viewModel.gitHubRepoUrl,
                onOpen = { url -> viewModel.onGitHubOpen(url) },
                onLater = { viewModel.closeGitHubPrompt() },
                onDontAskAgain = { viewModel.closeGitHubPrompt() }
            )
        }

        // Full-Page Capture & PDF Export Preview Dialog
        if (capturedBitmap != null) {
            CapturePreviewDialog(
                bitmap = capturedBitmap,
                pageTitle = pageTitle,
                onDismiss = onClearCapture
            )
        }
    }
}
