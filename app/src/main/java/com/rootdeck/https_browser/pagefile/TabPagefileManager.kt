package com.rootdeck.https_browser.pagefile

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class TabPagefileManager(private val baseDir: File) {

    constructor(context: Context) : this(context.cacheDir)

    private val pagefileDir: File by lazy {
        File(baseDir, "pagefiles").apply {
            if (!exists()) mkdirs()
        }
    }

    private val MAX_PAGEFILE_SIZE = 2 * 1024 * 1024 // 2MB cap per tab

    @Synchronized
    fun saveToDisk(state: TabState): File? {
        return try {
            val json = JSONObject().apply {
                put("id", state.id)
                put("url", state.url)
                put("title", state.title)
                put("scrollX", state.scrollX)
                put("scrollY", state.scrollY)
                put("historyIndex", state.historyIndex)
                put("timestamp", state.timestamp)
                put("isIncognito", state.isIncognito)
                put("faviconUrl", state.faviconUrl ?: "")
                put("thumbnailPath", state.thumbnailPath ?: "")

                val formsObj = JSONObject()
                state.formInputs.forEach { (k, v) ->
                    formsObj.put(k, v)
                }
                put("formInputs", formsObj)

                val histArr = JSONArray()
                state.historyStack.forEach { histArr.put(it) }
                put("historyStack", histArr)
            }

            val rawBytes = json.toString().toByteArray(Charsets.UTF_8)
            val file = File(pagefileDir, "${state.id}.tab")

            ByteArrayOutputStream().use { byteStream ->
                GZIPOutputStream(byteStream).use { gzip ->
                    gzip.write(rawBytes)
                }
                val compressedBytes = byteStream.toByteArray()
                if (compressedBytes.size > MAX_PAGEFILE_SIZE) {
                    Log.w(TAG, "Pagefile exceeds 2MB limit: ${compressedBytes.size} bytes. Skipping.")
                    return null
                }
                FileOutputStream(file).use { fos ->
                    fos.write(compressedBytes)
                }
            }

            cleanLruIfNeeded()
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save pagefile for tab ${state.id}", e)
            null
        }
    }

    @Synchronized
    fun restoreFromDisk(tabId: String): TabState? {
        val file = File(pagefileDir, "$tabId.tab")
        if (!file.exists()) return null

        return try {
            val jsonString = FileInputStream(file).use { fis ->
                GZIPInputStream(fis).use { gzip ->
                    gzip.bufferedReader(Charsets.UTF_8).readText()
                }
            }

            val json = JSONObject(jsonString)
            val formInputs = mutableMapOf<String, String>()
            if (json.has("formInputs")) {
                val formsObj = json.getJSONObject("formInputs")
                val keys = formsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    formInputs[k] = formsObj.optString(k, "")
                }
            }

            val historyStack = mutableListOf<String>()
            if (json.has("historyStack")) {
                val histArr = json.getJSONArray("historyStack")
                for (i in 0 until histArr.length()) {
                    historyStack.add(histArr.getString(i))
                }
            }

            TabState(
                id = json.getString("id"),
                url = json.getString("url"),
                title = json.optString("title", "New Tab"),
                scrollX = json.optInt("scrollX", 0),
                scrollY = json.optInt("scrollY", 0),
                formInputs = formInputs,
                historyStack = historyStack,
                historyIndex = json.optInt("historyIndex", 0),
                faviconUrl = json.optString("faviconUrl").takeIf { it.isNotEmpty() },
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                isIncognito = json.optBoolean("isIncognito", false),
                thumbnailPath = json.optString("thumbnailPath").takeIf { it.isNotEmpty() }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore pagefile for tab $tabId", e)
            null
        }
    }

    @Synchronized
    fun deletePagefile(tabId: String) {
        val file = File(pagefileDir, "$tabId.tab")
        if (file.exists()) {
            file.delete()
        }
    }

    @Synchronized
    fun clearAllPagefiles() {
        pagefileDir.listFiles()?.forEach { it.delete() }
    }

    private fun cleanLruIfNeeded() {
        val files = pagefileDir.listFiles() ?: return
        val maxFiles = 30
        if (files.size > maxFiles) {
            files.sortedBy { it.lastModified() }
                .take(files.size - maxFiles)
                .forEach { it.delete() }
        }
    }

    companion object {
        private const val TAG = "TabPagefileManager"
    }
}
