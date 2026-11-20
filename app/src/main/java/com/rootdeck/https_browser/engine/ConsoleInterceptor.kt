package com.rootdeck.https_browser.engine

import android.webkit.JavascriptInterface
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ConsoleMessageItem(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val type: Level,
    val message: String,
    val source: String? = null,
    val lineNumber: Int = 0,
    val timestamp: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
) {
    enum class Level {
        LOG, WARN, ERROR, INFO
    }
}

class ConsoleInterceptor {

    private val _messages = MutableSharedFlow<ConsoleMessageItem>(extraBufferCapacity = 200)
    val messages: SharedFlow<ConsoleMessageItem> = _messages.asSharedFlow()

    private val messageHistory = mutableListOf<ConsoleMessageItem>()

    fun getHistory(): List<ConsoleMessageItem> = synchronized(messageHistory) {
        messageHistory.toList()
    }

    fun clear() {
        synchronized(messageHistory) {
            messageHistory.clear()
        }
    }

    @JavascriptInterface
    fun postMessage(type: String, message: String, source: String?, lineNumber: Int) {
        val level = when (type.uppercase()) {
            "ERROR" -> ConsoleMessageItem.Level.ERROR
            "WARN" -> ConsoleMessageItem.Level.WARN
            "INFO" -> ConsoleMessageItem.Level.INFO
            else -> ConsoleMessageItem.Level.LOG
        }
        val item = ConsoleMessageItem(
            type = level,
            message = message,
            source = source,
            lineNumber = lineNumber
        )
        synchronized(messageHistory) {
            messageHistory.add(item)
            if (messageHistory.size > 500) {
                messageHistory.removeAt(0)
            }
        }
        _messages.tryEmit(item)
    }

    companion object {
        const val JS_INTERFACE_NAME = "AndroidConsoleBridge"

        val INJECTION_SCRIPT = """
            (function() {
                if (window.__consoleInterceptorInstalled) return;
                window.__consoleInterceptorInstalled = true;
                const originalLog = console.log;
                const originalWarn = console.warn;
                const originalError = console.error;
                const originalInfo = console.info;

                function formatArgs(args) {
                    return Array.from(args).map(a => {
                        if (typeof a === 'object') {
                            try { return JSON.stringify(a); } catch(e) { return String(a); }
                        }
                        return String(a);
                    }).join(' ');
                }

                console.log = function() {
                    originalLog.apply(console, arguments);
                    if (window.$JS_INTERFACE_NAME) {
                        window.$JS_INTERFACE_NAME.postMessage('LOG', formatArgs(arguments), '', 0);
                    }
                };

                console.warn = function() {
                    originalWarn.apply(console, arguments);
                    if (window.$JS_INTERFACE_NAME) {
                        window.$JS_INTERFACE_NAME.postMessage('WARN', formatArgs(arguments), '', 0);
                    }
                };

                console.error = function() {
                    originalError.apply(console, arguments);
                    if (window.$JS_INTERFACE_NAME) {
                        window.$JS_INTERFACE_NAME.postMessage('ERROR', formatArgs(arguments), '', 0);
                    }
                };

                console.info = function() {
                    originalInfo.apply(console, arguments);
                    if (window.$JS_INTERFACE_NAME) {
                        window.$JS_INTERFACE_NAME.postMessage('INFO', formatArgs(arguments), '', 0);
                    }
                };
            })();
        """.trimIndent()
    }
}
