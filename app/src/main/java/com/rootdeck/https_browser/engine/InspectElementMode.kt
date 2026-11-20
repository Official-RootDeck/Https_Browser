// Built by MD.Nirob
package com.rootdeck.https_browser.engine

import android.webkit.JavascriptInterface
import org.json.JSONObject

/**
 * InspectElementMode handles DOM element selection and inspection.
 * Built by MD.Nirob
 */
class InspectElementMode(
    private val onElementSelected: (tag: String, selector: String, html: String, id: String, className: String) -> Unit
) {

    @JavascriptInterface
    fun onElementClicked(tag: String, selector: String, html: String) {
        onElementSelected(tag, selector, html, "", "")
    }

    @JavascriptInterface
    fun onElementSelectedJson(jsonPayload: String) {
        try {
            val json = JSONObject(jsonPayload)
            val tag = json.optString("tag", "")
            val id = json.optString("id", "")
            val className = json.optString("className", "")
            val selector = json.optString("selector", tag)
            val outerHTML = json.optString("outerHTML", "")
            onElementSelected(tag, selector, outerHTML, id, className)
        } catch (_: Exception) {
            onElementSelected("element", "*", jsonPayload, "", "")
        }
    }

    companion object {
        const val JS_INTERFACE_NAME = "AndroidInspectorBridge"

        fun getEnableScript(): String = """
            (function() {
                if (window.__inspectorOverlayActive) return;
                window.__inspectorOverlayActive = true;

                let overlay = document.getElementById('__chrome_inspect_overlay');
                if (!overlay) {
                    overlay = document.createElement('div');
                    overlay.id = '__chrome_inspect_overlay';
                    overlay.style.position = 'absolute';
                    overlay.style.pointerEvents = 'none';
                    overlay.style.border = '2px solid #1A73E8';
                    overlay.style.backgroundColor = 'rgba(26, 115, 232, 0.2)';
                    overlay.style.zIndex = '9999999';
                    overlay.style.transition = 'all 0.05s ease';
                    document.body.appendChild(overlay);
                }
                overlay.style.display = 'block';

                function highlight(e) {
                    const clientX = e.clientX || (e.touches && e.touches[0] ? e.touches[0].clientX : 0);
                    const clientY = e.clientY || (e.touches && e.touches[0] ? e.touches[0].clientY : 0);
                    const target = document.elementFromPoint(clientX, clientY);
                    if (!target || target === overlay) return;
                    const rect = target.getBoundingClientRect();
                    overlay.style.left = (rect.left + window.scrollX) + 'px';
                    overlay.style.top = (rect.top + window.scrollY) + 'px';
                    overlay.style.width = rect.width + 'px';
                    overlay.style.height = rect.height + 'px';
                }

                function handleClick(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    const clientX = e.clientX || (e.changedTouches && e.changedTouches[0] ? e.changedTouches[0].clientX : 0);
                    const clientY = e.clientY || (e.changedTouches && e.changedTouches[0] ? e.changedTouches[0].clientY : 0);
                    const target = document.elementFromPoint(clientX, clientY);
                    if (target && target !== overlay) {
                        const tag = target.tagName.toLowerCase();
                        const id = target.id || '';
                        const className = (target.className && typeof target.className === 'string') ? target.className : '';
                        const sel = id ? ('#' + id) : (className ? ('.' + className.trim().split(/\s+/).join('.')) : tag);
                        const outer = (target.outerHTML || '').substring(0, 2048);
                        const payload = JSON.stringify({
                            tag: tag,
                            id: id,
                            className: className,
                            selector: sel,
                            outerHTML: outer
                        });
                        if (window.$JS_INTERFACE_NAME) {
                            window.$JS_INTERFACE_NAME.onElementSelectedJson(payload);
                        }
                    }
                    window.__devtools_clear_inspect__();
                }

                window.__highlightHandler = highlight;
                window.__clickHandler = handleClick;

                document.addEventListener('mousemove', highlight, true);
                document.addEventListener('touchmove', highlight, { passive: true, capture: true });
                document.addEventListener('click', handleClick, true);
                document.addEventListener('touchend', handleClick, true);

                window.__devtools_clear_inspect__ = function() {
                    window.__inspectorOverlayActive = false;
                    const o = document.getElementById('__chrome_inspect_overlay');
                    if (o) o.style.display = 'none';
                    if (window.__highlightHandler) {
                        document.removeEventListener('mousemove', window.__highlightHandler, true);
                        document.removeEventListener('touchmove', window.__highlightHandler, true);
                    }
                    if (window.__clickHandler) {
                        document.removeEventListener('click', window.__clickHandler, true);
                        document.removeEventListener('touchend', window.__clickHandler, true);
                    }
                };
            })();
        """.trimIndent()

        fun getDisableScript(): String = """
            (function() {
                if (window.__devtools_clear_inspect__) {
                    window.__devtools_clear_inspect__();
                }
            })();
        """.trimIndent()
    }
}
