package com.rootdeck.https_browser

import android.app.Application
import android.util.Log
import com.rootdeck.https_browser.data.BrowserDatabase
import com.rootdeck.https_browser.engine.DnsBlackhole
import com.rootdeck.https_browser.engine.EngineConfig
import com.rootdeck.https_browser.pagefile.MemoryPressureMonitor
import com.rootdeck.https_browser.pagefile.TabPagefileManager

class BrowserApplication : Application() {

    lateinit var database: BrowserDatabase
        private set

    lateinit var pagefileManager: TabPagefileManager
        private set

    lateinit var dnsBlackhole: DnsBlackhole
        private set

    lateinit var memoryMonitor: MemoryPressureMonitor
        private set

    var engineConfig = EngineConfig()

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = BrowserDatabase.getInstance(this)
        pagefileManager = TabPagefileManager(this)
        dnsBlackhole = DnsBlackhole(this)

        memoryMonitor = MemoryPressureMonitor(this) { level ->
            Log.i("BrowserApp", "Memory trim signal: $level")
            // Notify active components if memory is low
            @Suppress("DEPRECATION")
            if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
                // Free background cached pagefile bitmaps or trimmed resources
                Log.d("BrowserApp", "Executing low memory eviction.")
            }
        }
        memoryMonitor.register()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w("BrowserApp", "Critical onLowMemory received!")
    }

    companion object {
        lateinit var instance: BrowserApplication
            private set
    }
}
