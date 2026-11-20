// Built by MD.Nirob
package com.rootdeck.https_browser.session

import com.rootdeck.https_browser.data.BrowserDatabase
import com.rootdeck.https_browser.data.TabEntity
import java.util.UUID

/**
 * Built by MD.Nirob — Coordinates saving active browser tabs to Room sessions and restoring them.
 */
class SessionManager(private val database: BrowserDatabase) {

    suspend fun saveCurrentSession(name: String, activeTabs: List<TabEntity>): Long {
        val cleanName = name.trim().ifEmpty { "Session (${activeTabs.size} tabs)" }
        val sessionId = database.sessionDao().insertSession(
            SessionEntity(
                name = cleanName,
                tabCount = activeTabs.size
            )
        )
        val sessionTabs = activeTabs.mapIndexed { index, tab ->
            SessionTabEntity(
                sessionId = sessionId,
                url = tab.url,
                title = tab.title,
                position = index
            )
        }
        database.sessionDao().insertSessionTabs(sessionTabs)
        return sessionId
    }

    suspend fun restoreSession(sessionId: Long): TabEntity? {
        val savedTabs = database.sessionDao().getTabsForSession(sessionId)
        if (savedTabs.isEmpty()) return null

        val restoredTabs = savedTabs.mapIndexed { index, st ->
            TabEntity(
                id = UUID.randomUUID().toString(),
                url = st.url,
                title = st.title,
                isIncognito = false,
                position = index
            )
        }
        database.tabDao().deleteAll(incognito = false)
        database.tabDao().insertAll(restoredTabs)
        return restoredTabs.firstOrNull()
    }

    suspend fun deleteSession(session: SessionEntity) {
        database.sessionDao().deleteSession(session)
    }
}
