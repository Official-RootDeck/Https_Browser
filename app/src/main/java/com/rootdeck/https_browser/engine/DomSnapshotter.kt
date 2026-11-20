package com.rootdeck.https_browser.engine

import org.json.JSONArray
import org.json.JSONObject

data class DomNode(
    val id: String,
    val tagName: String,
    val elementId: String,
    val classNames: List<String>,
    val attributes: Map<String, String>,
    val textContent: String,
    val children: List<DomNode>,
    val computedStyles: Map<String, String> = emptyMap()
)

object DomSnapshotter {

    val JS_EXTRACTOR = """
        (function() {
            let idCounter = 1;
            function parseNode(node, depth) {
                if (depth > 6) return null;
                if (node.nodeType === Node.TEXT_NODE) {
                    const text = node.textContent.trim();
                    if (!text) return null;
                    return {
                        id: 'node_' + (idCounter++),
                        tagName: '#text',
                        elementId: '',
                        classNames: [],
                        attributes: {},
                        textContent: text.substring(0, 80),
                        children: [],
                        computedStyles: {}
                    };
                }
                if (node.nodeType !== Node.ELEMENT_NODE) return null;
                const tag = node.tagName.toLowerCase();
                if (tag === 'script' || tag === 'style' || tag === 'noscript') return null;

                const attrs = {};
                for (let i = 0; i < node.attributes.length; i++) {
                    const attr = node.attributes[i];
                    attrs[attr.name] = attr.value;
                }

                const style = window.getComputedStyle(node);
                const computed = {
                    display: style.display || '',
                    color: style.color || '',
                    backgroundColor: style.backgroundColor || '',
                    fontSize: style.fontSize || '',
                    margin: style.margin || '',
                    padding: style.padding || '',
                    width: Math.round(node.offsetWidth) + 'px',
                    height: Math.round(node.offsetHeight) + 'px'
                };

                const childNodes = [];
                for (let i = 0; i < node.childNodes.length; i++) {
                    const parsedChild = parseNode(node.childNodes[i], depth + 1);
                    if (parsedChild) childNodes.push(parsedChild);
                }

                return {
                    id: 'node_' + (idCounter++),
                    tagName: tag,
                    elementId: node.id || '',
                    classNames: Array.from(node.classList || []),
                    attributes: attrs,
                    textContent: (node.children.length === 0 ? node.textContent.trim().substring(0, 60) : ''),
                    children: childNodes,
                    computedStyles: computed
                };
            }

            try {
                return JSON.stringify(parseNode(document.documentElement, 0));
            } catch(e) {
                return JSON.stringify({ error: e.message });
            }
        })();
    """.trimIndent()

    fun parseJson(jsonString: String): DomNode? {
        return try {
            val unquoted = if (jsonString.startsWith("\"") && jsonString.endsWith("\"")) {
                org.json.JSONTokener(jsonString).nextValue().toString()
            } else {
                jsonString
            }
            val json = JSONObject(unquoted)
            if (json.has("error")) return null
            parseNodeJson(json)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseNodeJson(obj: JSONObject): DomNode {
        val classList = mutableListOf<String>()
        val classArr = obj.optJSONArray("classNames")
        if (classArr != null) {
            for (i in 0 until classArr.length()) {
                classList.add(classArr.getString(i))
            }
        }

        val attrs = mutableMapOf<String, String>()
        val attrObj = obj.optJSONObject("attributes")
        if (attrObj != null) {
            val keys = attrObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                attrs[k] = attrObj.optString(k)
            }
        }

        val styles = mutableMapOf<String, String>()
        val stylesObj = obj.optJSONObject("computedStyles")
        if (stylesObj != null) {
            val keys = stylesObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                styles[k] = stylesObj.optString(k)
            }
        }

        val childrenList = mutableListOf<DomNode>()
        val childrenArr = obj.optJSONArray("children")
        if (childrenArr != null) {
            for (i in 0 until childrenArr.length()) {
                val child = childrenArr.optJSONObject(i)
                if (child != null) {
                    childrenList.add(parseNodeJson(child))
                }
            }
        }

        return DomNode(
            id = obj.optString("id", "0"),
            tagName = obj.optString("tagName", ""),
            elementId = obj.optString("elementId", ""),
            classNames = classList,
            attributes = attrs,
            textContent = obj.optString("textContent", ""),
            children = childrenList,
            computedStyles = styles
        )
    }
}
