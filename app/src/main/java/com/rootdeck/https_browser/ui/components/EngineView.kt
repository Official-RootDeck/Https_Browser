// Built by MD.Nirob
package com.rootdeck.https_browser.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.rootdeck.https_browser.BrowserApplication
import com.rootdeck.https_browser.devtools.NetworkEntry
import com.rootdeck.https_browser.engine.*
import com.rootdeck.https_browser.pdf.PdfViewerActivity
import java.io.ByteArrayInputStream

/**
 * Built by MD.Nirob — Chromium WebView engine integration.
 * Features: DevTools network logging, offline Dino error page, Google Autofill,
 * PDF interceptor, and Android 5.1 legacy pointer-event workaround.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EngineView(
    tabId: String,
    url: String,
    isIncognito: Boolean,
    consoleInterceptor: ConsoleInterceptor,
    networkInterceptor: NetworkInterceptor,
    inspectElementMode: InspectElementMode,
    onTitleChanged: (String) -> Unit,
    onProgressChanged: (Float) -> Unit,
    onLoadingStateChanged: (Boolean) -> Unit,
    onUrlChanged: (String) -> Unit,
    onDomExtracted: (DomNode?) -> Unit,
    onNavigateChromeScheme: (ChromeUrlResult) -> Unit,
    onRenderProcessCrashed: () -> Unit = {},
    onWebViewReady: (WebView?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val app = BrowserApplication.instance
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Built by MD.Nirob — legacy WebView pointer-event workaround
    val legacyPolyfillJs = """
        /* Built by MD.Nirob */
        (function() {
            if (window.__legacyPointerEventInstalled) return;
            window.__legacyPointerEventInstalled = true;
            if (window.PointerEvent === undefined) {
                window.PointerEvent = function(type, params) {
                    params = params || {};
                    var evt = document.createEvent('CustomEvent');
                    evt.initCustomEvent(type, params.bubbles || false, params.cancelable || false, params.detail || null);
                    evt.pointerId = params.pointerId || 0;
                    evt.width = params.width || 0;
                    evt.height = params.height || 0;
                    evt.pressure = params.pressure || 0;
                    evt.tiltX = params.tiltX || 0;
                    evt.tiltY = params.tiltY || 0;
                    evt.pointerType = params.pointerType || 'touch';
                    evt.isPrimary = params.isPrimary || false;
                    return evt;
                };
                console.log('[legacy-polyfill] installed');
            }
            document.addEventListener('touchend', function(e) {
                var target = e.target;
                if (!target) return;
                var tag = target.tagName ? target.tagName.toUpperCase() : '';
                var role = target.getAttribute ? target.getAttribute('role') : '';
                var isClickable = tag === 'BUTTON' || tag === 'A' || role === 'button' || (tag === 'INPUT' && target.type === 'submit');
                if (isClickable) {
                    var clickEvt = document.createEvent('MouseEvents');
                    clickEvt.initMouseEvent('click', true, true, window, 1, 0, 0, 0, 0, false, false, false, false, 0, null);
                    target.dispatchEvent(clickEvt);
                }
            }, true);
        })();
    """.trimIndent()

    fun openPdfViewer(context: Context, pdfUrl: String) {
        try {
            val intent = Intent(context, PdfViewerActivity::class.java).apply {
                putExtra("pdf_url", pdfUrl)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("EngineView", "Failed to launch PdfViewerActivity", e)
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Enable remote debugging for DevTools
                WebView.setWebContentsDebuggingEnabled(true)

                // Task 49: Accept cookies & third-party cookies safely
                try {
                    val cookieMgr = CookieManager.getInstance()
                    cookieMgr?.setAcceptCookie(true)
                    cookieMgr?.setAcceptThirdPartyCookies(this, true)
                } catch (e: Exception) {
                    Log.w("EngineView", "CookieManager initialization warning: ${e.message}")
                }

                // Task 119-122: Enable autofill on login fields
                // Built by MD.Nirob - Security Notice: Never log credentials or autofill data to Logcat.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
                    setAutofillHints(View.AUTOFILL_HINT_USERNAME, View.AUTOFILL_HINT_PASSWORD)
                }

                // Task 164: Hardware layer type if accelerated
                if (isHardwareAccelerated) {
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    loadsImagesAutomatically = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    cacheMode = if (isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
                    saveFormData = true
                    // Task 166: mediaPlaybackRequiresUserGesture optional setting
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess = true
                }

                // Task 152: DownloadListener routing PDF MIME to PdfViewerActivity
                setDownloadListener { downloadUrl, _, contentDisposition, mimeType, _ ->
                    if (mimeType.equals("application/pdf", ignoreCase = true) || downloadUrl.lowercase().contains(".pdf")) {
                        openPdfViewer(context, downloadUrl)
                    }
                }

                // Inject JS bridges for Console and Inspector
                addJavascriptInterface(consoleInterceptor, ConsoleInterceptor.JS_INTERFACE_NAME)
                addJavascriptInterface(inspectElementMode, InspectElementMode.JS_INTERFACE_NAME)

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val reqUrl = request?.url?.toString() ?: return false

                        // Task 99: Intercept reload: scheme from offline error page
                        if (reqUrl.startsWith("reload:")) {
                            val originalUrl = reqUrl.removePrefix("reload:")
                            if (originalUrl.isNotEmpty()) {
                                view?.loadUrl(originalUrl)
                                return true
                            }
                        }

                        // Task 151: Check if URL ends with .pdf
                        val cleanUrl = reqUrl.substringBefore('?').substringBefore('#')
                        if (cleanUrl.endsWith(".pdf", ignoreCase = true)) {
                            openPdfViewer(context, reqUrl)
                            return true
                        }

                        val intercepted = ChromeURLInterceptor.intercept(reqUrl)
                        if (intercepted !is ChromeUrlResult.None) {
                            onNavigateChromeScheme(intercepted)
                            return true
                        }
                        return false
                    }

                    // Task 40 & 50 & 54: Capture network requests and log to DevTools
                    // Note: Sub-resource network requests are captured here. WebView cannot directly intercept
                    // client-side XHR/fetch payload bodies without service-workers; metadata is logged accurately.
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val reqUrl = request?.url?.toString() ?: return null
                        val startTime = System.currentTimeMillis()

                        // Check DNS Blackhole tracker blocker
                        val isBlocked = app.engineConfig.trackingProtectionEnabled && app.dnsBlackhole.isBlocked(reqUrl)

                        val duration = System.currentTimeMillis() - startTime
                        val reqHeaders = request.requestHeaders ?: emptyMap()
                        val cookieHeader = reqHeaders["Cookie"] ?: reqHeaders["cookie"]
                        val cookiesList = if (cookieHeader != null) {
                            cookieHeader.split(";").map { it.trim() }
                        } else emptyList()

                        val entry = NetworkEntry(
                            url = reqUrl,
                            method = request.method ?: "GET",
                            status = if (isBlocked) 404 else 200,
                            sizeBytes = 0,
                            durationMs = duration,
                            cookies = cookiesList,
                            headers = reqHeaders,
                            responseHeaders = emptyMap(),
                            mimeType = if (isBlocked) "text/plain" else null,
                            isBlocked = isBlocked
                        )
                        networkInterceptor.recordRequest(entry)

                        if (isBlocked) {
                            return WebResourceResponse(
                                "text/plain",
                                "UTF-8",
                                404,
                                "Blocked by DNS Blackhole",
                                emptyMap(),
                                ByteArrayInputStream("Blocked by DNS Blackhole".toByteArray())
                            )
                        }

                        // Task 54: Return null to allow default load safely
                        return null
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onLoadingStateChanged(true)
                        if (url != null) onUrlChanged(url)

                        // Task 36: Ensure inspector overlay does not persist across navigation
                        view?.evaluateJavascript(InspectElementMode.getDisableScript(), null)

                        // Task 167: Early injection of legacy pointer-event polyfill
                        view?.evaluateJavascript(legacyPolyfillJs, null)

                        // Inject console interceptor
                        view?.evaluateJavascript(ConsoleInterceptor.INJECTION_SCRIPT, null)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onLoadingStateChanged(false)
                        onProgressChanged(1f)
                        if (url != null) onUrlChanged(url)

                        // Task 160: Re-inject legacy polyfill fallback
                        view?.evaluateJavascript(legacyPolyfillJs, null)

                        // Extract live DOM structure for DevTools
                        view?.evaluateJavascript(DomSnapshotter.JS_EXTRACTOR) { jsonResult ->
                            if (jsonResult != null && jsonResult != "null") {
                                val node = DomSnapshotter.parseJson(jsonResult)
                                onDomExtracted(node)
                            }
                        }
                    }

                    // Tasks 97 & 100: Offline error page with embedded Dino game
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            val failingUrl = request.url?.toString() ?: ""
                            Log.e("EngineView", "Main frame page error on $failingUrl: ${error?.description}")
                            onLoadingStateChanged(false)
                            if (!failingUrl.startsWith("file:///android_asset/error.html")) {
                                view?.loadUrl("file:///android_asset/error.html?url=" + Uri.encode(failingUrl))
                            }
                        }
                    }

                    // Task 98: On HTTP error status >= 500 load error page for main frame
                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?
                    ) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        if (request?.isForMainFrame == true) {
                            val statusCode = errorResponse?.statusCode ?: 200
                            if (statusCode >= 500) {
                                val failingUrl = request.url?.toString() ?: ""
                                if (!failingUrl.startsWith("file:///android_asset/error.html")) {
                                    view?.loadUrl("file:///android_asset/error.html?url=" + Uri.encode(failingUrl))
                                }
                            }
                        }
                    }

                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                        Log.w("EngineView", "SSL error: ${error?.primaryError}")
                        handler?.cancel()
                    }

                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                        val didCrash = detail?.didCrash() ?: false
                        val priority = detail?.rendererPriorityAtExit() ?: 0
                        Log.e("EngineView", "Chromium render process terminated! didCrash=$didCrash, priority=$priority")

                        try {
                            view?.let {
                                val parent = it.parent as? ViewGroup
                                parent?.removeView(it)
                                it.destroy()
                            }
                        } catch (e: Exception) {
                            Log.e("EngineView", "Error cleaning up dead WebView", e)
                        }

                        onRenderProcessCrashed()
                        return true
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChanged(newProgress / 100f)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        if (!title.isNullOrEmpty()) onTitleChanged(title)
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        if (consoleMessage != null) {
                            consoleInterceptor.postMessage(
                                type = consoleMessage.messageLevel().name,
                                message = consoleMessage.message() ?: "",
                                source = consoleMessage.sourceId(),
                                lineNumber = consoleMessage.lineNumber()
                            )
                        }
                        return true
                    }
                }

                if (url.isNotEmpty() && url != "about:blank" && !url.startsWith("chrome://newtab")) {
                    loadUrl(url)
                }
                webViewRef = this
                onWebViewReady(this)
            }
        },
        update = { webView ->
            onWebViewReady(webView)
            if (url.isNotEmpty() && url != "about:blank" && !url.startsWith("chrome://newtab") && webView.url != url) {
                webView.loadUrl(url)
            }
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("engine_web_view")
    )

    DisposableEffect(tabId) {
        onDispose {
            webViewRef?.stopLoading()
            onWebViewReady(null)
        }
    }
}
