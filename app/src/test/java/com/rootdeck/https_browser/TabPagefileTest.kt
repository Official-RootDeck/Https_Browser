package com.rootdeck.https_browser

import com.rootdeck.https_browser.pagefile.TabPagefileManager
import com.rootdeck.https_browser.pagefile.TabState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TabPagefileTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var pagefileManager: TabPagefileManager

    @Before
    fun setup() {
        val root = tempFolder.newFolder("cache")
        pagefileManager = TabPagefileManager(root)
    }

    @Test
    fun testSerializeAndRestoreTenTabs() {
        val tabs = (1..10).map { i ->
            TabState(
                id = "tab_$i",
                url = "https://example$i.com/page",
                title = "Example Page $i",
                scrollX = i * 10,
                scrollY = i * 250,
                formInputs = mapOf("username" to "user$i", "search" to "query_$i"),
                historyStack = listOf("https://example$i.com", "https://example$i.com/page"),
                historyIndex = 1,
                isIncognito = (i % 2 == 0)
            )
        }

        // Save all 10 tabs to disk
        tabs.forEach { tab ->
            val file = requireNotNull(pagefileManager.saveToDisk(tab)) {
                "Pagefile should be created on disk for ${tab.id}"
            }
            assertTrue("Pagefile should exist on disk", file.exists())
            assertTrue("Pagefile should have non-zero size", file.length() > 0)
        }

        // Restore and verify all 10 tabs
        tabs.forEach { original ->
            val restored = requireNotNull(pagefileManager.restoreFromDisk(original.id)) {
                "Restored tab should not be null for ${original.id}"
            }
            assertEquals("URL must match", original.url, restored.url)
            assertEquals("Title must match", original.title, restored.title)
            assertEquals("scrollX must match", original.scrollX, restored.scrollX)
            assertEquals("scrollY must match", original.scrollY, restored.scrollY)
            assertEquals("Form inputs must match", original.formInputs, restored.formInputs)
            assertEquals("History stack must match", original.historyStack, restored.historyStack)
            assertEquals("Incognito flag must match", original.isIncognito, restored.isIncognito)
        }
    }

    @Test
    fun testDeletePagefile() {
        val tab = TabState(id = "delete_me", url = "https://test.org", title = "Test")
        val file = requireNotNull(pagefileManager.saveToDisk(tab))
        assertTrue(file.exists())

        pagefileManager.deletePagefile("delete_me")
        assertNull(pagefileManager.restoreFromDisk("delete_me"))
    }
}
