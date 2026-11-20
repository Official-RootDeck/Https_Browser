// Built by MD.Nirob
package com.rootdeck.https_browser.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rootdeck.https_browser.BrowserApplication
import com.rootdeck.https_browser.data.*
import com.rootdeck.https_browser.devtools.NetworkEntry
import com.rootdeck.https_browser.engine.*
import com.rootdeck.https_browser.pagefile.TabState
import com.rootdeck.https_browser.reader.ReaderArticle
import com.rootdeck.https_browser.reader.ReaderModeExtractor
import com.rootdeck.https_browser.readinglist.ReadingListEntity
import com.rootdeck.https_browser.readinglist.ReadingListExtractor
import com.rootdeck.https_browser.session.SessionEntity
import com.rootdeck.https_browser.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen {
    BROWSER,
    TAB_SWITCHER,
    SETTINGS,
    HISTORY,
    BOOKMARKS,
    FLAGS,
    NET_INTERNALS,
    READER_MODE,
    READING_LIST,
    SESSIONS
}

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BrowserApplication
    private val db = app.database
    private val pagefileManager = app.pagefileManager
    private val sessionManager = SessionManager(db)

    // Console & Network interceptors
    val consoleInterceptor = ConsoleInterceptor()
    val networkInterceptor = NetworkInterceptor()

    // Inspector
    var inspectElementMode = InspectElementMode { tag, selector, html, id, className ->
        val node = DomNode(
            id = (1000..9999).random().toString(),
            tagName = tag,
            elementId = id,
            classNames = if (className.isNotEmpty()) className.split(" ") else emptyList(),
            textContent = "",
            attributes = mapOf("selector" to selector, "id" to id, "class" to className).filterValues { it.isNotEmpty() },
            computedStyles = mapOf("selector" to selector),
            children = emptyList()
        )
        _domTree.value = node
        _inspectedElement.value = "$tag ($selector)\n$html"
        _isInspectMode.value = false
    }

    // Navigation and screen state
    private val _currentScreen = MutableStateFlow(AppScreen.BROWSER)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _isOmniboxExpanded = MutableStateFlow(false)
    val isOmniboxExpanded: StateFlow<Boolean> = _isOmniboxExpanded.asStateFlow()

    private val _isDevToolsOpen = MutableStateFlow(false)
    val isDevToolsOpen: StateFlow<Boolean> = _isDevToolsOpen.asStateFlow()

    private val _isAboutOpen = MutableStateFlow(false)
    val isAboutOpen: StateFlow<Boolean> = _isAboutOpen.asStateFlow()

    private val _isGitHubPromptOpen = MutableStateFlow(false)
    val isGitHubPromptOpen: StateFlow<Boolean> = _isGitHubPromptOpen.asStateFlow()

    // Developer contact URL - User can edit this URL as needed
    var contactDevUrl: String = "https://rootdeck.org"

    // GitHub repository URL - User can edit this URL as needed
    var gitHubRepoUrl: String = "https://github.com/rootdeck-org/https-browser"

    private val _isInspectMode = MutableStateFlow(false)
    val isInspectMode: StateFlow<Boolean> = _isInspectMode.asStateFlow()

    private val _inspectedElement = MutableStateFlow<String?>(null)
    val inspectedElement: StateFlow<String?> = _inspectedElement.asStateFlow()

    // Incognito state
    private val _isIncognitoMode = MutableStateFlow(false)
    val isIncognitoMode: StateFlow<Boolean> = _isIncognitoMode.asStateFlow()

    // Tabs
    val standardTabs: StateFlow<List<TabEntity>> = db.tabDao().getTabs(incognito = false)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val incognitoTabs: StateFlow<List<TabEntity>> = db.tabDao().getTabs(incognito = true)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _activeTabId = MutableStateFlow<String>("")
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    // Active tab transient states
    private val _currentUrl = MutableStateFlow("chrome://newtab")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _pageTitle = MutableStateFlow("New Tab")
    val pageTitle: StateFlow<String> = _pageTitle.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    // DevTools DOM tree
    private val _domTree = MutableStateFlow<DomNode?>(null)
    val domTree: StateFlow<DomNode?> = _domTree.asStateFlow()

    // Console & Network lists
    private val _consoleMessages = MutableStateFlow<List<ConsoleMessageItem>>(emptyList())
    val consoleMessages: StateFlow<List<ConsoleMessageItem>> = _consoleMessages.asStateFlow()

    private val _networkRequests = MutableStateFlow<List<NetworkEntry>>(emptyList())
    val networkRequests: StateFlow<List<NetworkEntry>> = _networkRequests.asStateFlow()

    // History and Bookmarks
    val historyList: StateFlow<List<HistoryEntity>> = db.historyDao().getAllHistory()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val bookmarksList: StateFlow<List<BookmarkEntity>> = db.bookmarkDao().getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Reading List and Sessions
    val readingList: StateFlow<List<ReadingListEntity>> = db.readingListDao().getAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val sessionsList: StateFlow<List<SessionEntity>> = db.sessionDao().getAllSessions()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _readerArticle = MutableStateFlow<ReaderArticle?>(null)
    val readerArticle: StateFlow<ReaderArticle?> = _readerArticle.asStateFlow()

    // Settings
    private val _trackingProtectionEnabled = MutableStateFlow(app.engineConfig.trackingProtectionEnabled)
    val trackingProtectionEnabled: StateFlow<Boolean> = _trackingProtectionEnabled.asStateFlow()

    private val _blockedTrackerCount = MutableStateFlow(app.dnsBlackhole.blockedCount)
    val blockedTrackerCount: StateFlow<Int> = _blockedTrackerCount.asStateFlow()

    // JS execution bridge callback
    var jsExecutor: ((String, (String) -> Unit) -> Unit)? = null

    init {
        // Collect console messages
        viewModelScope.launch {
            consoleInterceptor.messages.collect { msg ->
                _consoleMessages.update { listOf(msg) + it.take(200) }
            }
        }

        // Collect network requests (Task 52: auto-trim to last 500 entries)
        viewModelScope.launch {
            networkInterceptor.requests.collect { req ->
                _networkRequests.update { (listOf(req) + it).take(500) }
                _blockedTrackerCount.value = app.dnsBlackhole.blockedCount
            }
        }

        // Initialize default tab if empty
        viewModelScope.launch(Dispatchers.IO) {
            val tabs = db.tabDao().getTabs(incognito = false).first()
            if (tabs.isEmpty()) {
                val newId = UUID.randomUUID().toString()
                val tab = TabEntity(
                    id = newId,
                    url = "chrome://newtab",
                    title = "New Tab",
                    isIncognito = false,
                    position = 0
                )
                db.tabDao().insertOrUpdate(tab)
                _activeTabId.value = newId
            } else {
                _activeTabId.value = tabs.first().id
                _currentUrl.value = tabs.first().url
                _pageTitle.value = tabs.first().title
            }
        }
    }

    fun openUrl(url: String) {
        val intercepted = ChromeURLInterceptor.intercept(url)
        when (intercepted) {
            is ChromeUrlResult.NavigateRoute -> {
                when (intercepted.route) {
                    "settings" -> _currentScreen.value = AppScreen.SETTINGS
                    "history" -> _currentScreen.value = AppScreen.HISTORY
                    "bookmarks" -> _currentScreen.value = AppScreen.BOOKMARKS
                    "flags" -> _currentScreen.value = AppScreen.FLAGS
                    "net_internals" -> _currentScreen.value = AppScreen.NET_INTERNALS
                }
                return
            }
            is ChromeUrlResult.LoadInternalAsset -> {
                _currentUrl.value = intercepted.assetUrl
                _pageTitle.value = "Chrome Dino Game"
            }
            is ChromeUrlResult.NewTab -> {
                _currentUrl.value = "chrome://newtab"
                _pageTitle.value = "New Tab"
            }
            is ChromeUrlResult.None -> {
                _currentUrl.value = url
            }
        }

        _isOmniboxExpanded.value = false
        _currentScreen.value = AppScreen.BROWSER

        // Update active tab in DB
        val activeId = _activeTabId.value
        if (activeId.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                val tab = db.tabDao().getTabById(activeId)
                if (tab != null) {
                    db.tabDao().insertOrUpdate(tab.copy(url = _currentUrl.value, title = _pageTitle.value))
                }
            }
        }

        // Record history if not incognito
        if (!_isIncognitoMode.value && !_currentUrl.value.startsWith("chrome://") && _currentUrl.value != "about:blank") {
            viewModelScope.launch(Dispatchers.IO) {
                db.historyDao().insert(HistoryEntity(url = _currentUrl.value, title = _pageTitle.value))
            }
        }
    }

    fun newTab(isIncognito: Boolean = _isIncognitoMode.value) {
        val newId = UUID.randomUUID().toString()
        val newTab = TabEntity(
            id = newId,
            url = "chrome://newtab",
            title = "New Tab",
            isIncognito = isIncognito,
            position = 999
        )
        viewModelScope.launch(Dispatchers.IO) {
            // Save current tab state to pagefile
            val currentId = _activeTabId.value
            if (currentId.isNotEmpty()) {
                pagefileManager.saveToDisk(
                    TabState(
                        id = currentId,
                        url = _currentUrl.value,
                        title = _pageTitle.value,
                        isIncognito = _isIncognitoMode.value
                    )
                )
            }
            db.tabDao().insertOrUpdate(newTab)
            _activeTabId.value = newId
            _isIncognitoMode.value = isIncognito
            _currentUrl.value = "chrome://newtab"
            _pageTitle.value = "New Tab"
            _currentScreen.value = AppScreen.BROWSER
        }
    }

    fun selectTab(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Save outgoing tab to pagefile
            val oldId = _activeTabId.value
            if (oldId.isNotEmpty() && oldId != id) {
                pagefileManager.saveToDisk(
                    TabState(
                        id = oldId,
                        url = _currentUrl.value,
                        title = _pageTitle.value,
                        isIncognito = _isIncognitoMode.value
                    )
                )
            }

            // Restore target tab
            val tab = db.tabDao().getTabById(id)
            if (tab != null) {
                val pagefile = pagefileManager.restoreFromDisk(id)
                _activeTabId.value = id
                _isIncognitoMode.value = tab.isIncognito
                _currentUrl.value = pagefile?.url ?: tab.url
                _pageTitle.value = pagefile?.title ?: tab.title
                _currentScreen.value = AppScreen.BROWSER
            }
        }
    }

    fun closeTab(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.tabDao().deleteById(id)
            pagefileManager.deletePagefile(id)

            val currentTabs = if (_isIncognitoMode.value) incognitoTabs.value else standardTabs.value
            val remaining = currentTabs.filter { it.id != id }

            if (_activeTabId.value == id) {
                if (remaining.isNotEmpty()) {
                    selectTab(remaining.last().id)
                } else {
                    newTab(_isIncognitoMode.value)
                }
            }
        }
    }

    fun closeAllTabs() {
        viewModelScope.launch(Dispatchers.IO) {
            db.tabDao().deleteAll(_isIncognitoMode.value)
            pagefileManager.clearAllPagefiles()
            newTab(_isIncognitoMode.value)
        }
    }

    fun toggleIncognitoMode(isIncognito: Boolean) {
        _isIncognitoMode.value = isIncognito
        val tabs = if (isIncognito) incognitoTabs.value else standardTabs.value
        if (tabs.isNotEmpty()) {
            selectTab(tabs.first().id)
        } else {
            newTab(isIncognito)
        }
    }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setOmniboxExpanded(expanded: Boolean) {
        _isOmniboxExpanded.value = expanded
    }

    fun toggleDevTools() {
        _isDevToolsOpen.update { !it }
    }

    fun openAbout() {
        _isAboutOpen.value = true
    }

    fun closeAbout() {
        _isAboutOpen.value = false
    }

    fun openContactDev() {
        openUrl(contactDevUrl)
    }

    fun openGitHubPrompt() {
        _isGitHubPromptOpen.value = true
    }

    fun closeGitHubPrompt() {
        _isGitHubPromptOpen.value = false
    }

    fun onGitHubOpen(url: String) {
        _isGitHubPromptOpen.value = false
        openUrl(url)
    }

    fun onRenderProcessCrashed() {
        Log.e("BrowserViewModel", "Chromium render process crashed! Recovering active session...")
        val target = _currentUrl.value
        if (target.isNotEmpty() && target != "chrome://newtab" && target != "about:blank") {
            viewModelScope.launch(Dispatchers.Main) {
                _isLoading.value = false
                _progress.value = 0f
                // Re-trigger load after renderer teardown
                kotlinx.coroutines.delay(150)
                _currentUrl.value = target
            }
        }
    }

    fun toggleInspectMode() {
        _isInspectMode.update { !it }
        val script = if (_isInspectMode.value) InspectElementMode.getEnableScript() else InspectElementMode.getDisableScript()
        jsExecutor?.invoke(script) {}
    }

    fun refreshDom() {
        jsExecutor?.invoke(DomSnapshotter.JS_EXTRACTOR) { jsonResult ->
            if (jsonResult.isNotEmpty() && jsonResult != "null") {
                _domTree.value = DomSnapshotter.parseJson(jsonResult)
            }
        }
    }

    fun executeJs(code: String, callback: (String) -> Unit) {
        jsExecutor?.invoke(code, callback)
    }

    fun clearConsole() {
        consoleInterceptor.clear()
        _consoleMessages.value = emptyList()
    }

    fun clearNetwork() {
        networkInterceptor.clear()
        _networkRequests.value = emptyList()
    }

    fun clearSiteData() {
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        android.webkit.WebStorage.getInstance().deleteAllData()
        jsExecutor?.invoke("window.localStorage.clear(); window.sessionStorage.clear();") {}
    }

    fun clearAllBrowsingData() {
        viewModelScope.launch(Dispatchers.IO) {
            db.historyDao().clearAll()
            pagefileManager.clearAllPagefiles()
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.WebStorage.getInstance().deleteAllData()
        }
    }

    fun toggleTrackingProtection(enabled: Boolean) {
        _trackingProtectionEnabled.value = enabled
        app.engineConfig = app.engineConfig.copy(trackingProtectionEnabled = enabled)
    }

    fun toggleBookmark(url: String, title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val exists = db.bookmarkDao().isBookmarked(url)
            if (exists) {
                db.bookmarkDao().deleteByUrl(url)
            } else {
                db.bookmarkDao().insert(BookmarkEntity(url = url, title = title.ifEmpty { url }))
            }
        }
    }

    fun deleteHistoryItem(item: HistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.historyDao().delete(item)
        }
    }

    fun deleteBookmark(item: BookmarkEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.bookmarkDao().delete(item)
        }
    }

    fun onUrlChanged(newUrl: String) {
        _currentUrl.value = newUrl
        // Update DB
        val activeId = _activeTabId.value
        if (activeId.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                val tab = db.tabDao().getTabById(activeId)
                if (tab != null && tab.url != newUrl) {
                    db.tabDao().insertOrUpdate(tab.copy(url = newUrl))
                }
            }
        }
    }

    fun onTitleChanged(newTitle: String) {
        _pageTitle.value = newTitle
        val activeId = _activeTabId.value
        if (activeId.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                val tab = db.tabDao().getTabById(activeId)
                if (tab != null && tab.title != newTitle) {
                    db.tabDao().insertOrUpdate(tab.copy(title = newTitle))
                }
            }
        }
    }

    fun onLoadingStateChanged(loading: Boolean) {
        _isLoading.value = loading
    }

    fun onProgressChanged(prog: Float) {
        _progress.value = prog
    }

    fun onDomExtracted(node: DomNode?) {
        _domTree.value = node
    }

    // -------------------------------------------------------------
    // Reader Mode
    // -------------------------------------------------------------
    fun enterReaderMode(onFailed: () -> Unit = {}) {
        val executor = jsExecutor
        if (executor == null || _currentUrl.value.startsWith("chrome://")) {
            onFailed()
            return
        }
        val script = ReaderModeExtractor.loadReadabilityScript(app.assets)
        executor.invoke(script) { rawResult ->
            val article = ReaderModeExtractor.parseResult(rawResult, _currentUrl.value)
            if (article != null) {
                _readerArticle.value = article
                _currentScreen.value = AppScreen.READER_MODE
            } else {
                onFailed()
            }
        }
    }

    fun openOfflineArticle(article: ReaderArticle) {
        _readerArticle.value = article
        _currentScreen.value = AppScreen.READER_MODE
    }

    // -------------------------------------------------------------
    // Reading List (Offline)
    // -------------------------------------------------------------
    fun saveCurrentPageToReadingList(onSaved: () -> Unit = {}) {
        val url = _currentUrl.value
        val title = _pageTitle.value
        if (url.isBlank() || url.startsWith("chrome://")) return

        val executor = jsExecutor
        if (executor != null) {
            val script = ReadingListExtractor.getExtractionScript(app.assets)
            executor.invoke(script) { rawResult ->
                val entity = ReadingListExtractor.buildEntity(rawResult, url, title)
                viewModelScope.launch(Dispatchers.IO) {
                    db.readingListDao().insert(entity)
                }
                onSaved()
            }
        } else {
            val entity = ReadingListExtractor.buildEntity(null, url, title)
            viewModelScope.launch(Dispatchers.IO) {
                db.readingListDao().insert(entity)
            }
            onSaved()
        }
    }

    fun toggleReadingListRead(item: ReadingListEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.readingListDao().setReadStatus(item.id, !item.isRead)
        }
    }

    fun deleteReadingListItem(item: ReadingListEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.readingListDao().delete(item)
        }
    }

    // -------------------------------------------------------------
    // Session Manager
    // -------------------------------------------------------------
    fun saveCurrentSession(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val tabs = standardTabs.value
            if (tabs.isNotEmpty()) {
                sessionManager.saveCurrentSession(name, tabs)
            }
        }
    }

    fun restoreSession(session: SessionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val firstTab = sessionManager.restoreSession(session.id)
            if (firstTab != null) {
                _isIncognitoMode.value = false
                _activeTabId.value = firstTab.id
                _currentUrl.value = firstTab.url
                _pageTitle.value = firstTab.title
                _currentScreen.value = AppScreen.BROWSER
            }
        }
    }

    fun deleteSession(session: SessionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            sessionManager.deleteSession(session)
        }
    }
}
