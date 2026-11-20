package com.rootdeck.https_browser.pagefile

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Debug
import android.util.Log

class MemoryPressureMonitor(
    private val context: Context,
    private val onTrimCallback: (level: Int) -> Unit
) : ComponentCallbacks2 {

    fun register() {
        context.registerComponentCallbacks(this)
    }

    fun unregister() {
        context.unregisterComponentCallbacks(this)
    }

    override fun onTrimMemory(level: Int) {
        Log.i(TAG, "onTrimMemory received level: $level")
        onTrimCallback(level)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {}

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onLowMemory() {
        Log.w(TAG, "onLowMemory received! High memory pressure.")
        onTrimCallback(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
    }

    fun getMemoryStats(): MemoryStats {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val runtime = Runtime.getRuntime()
        val usedHeapMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxHeapMb = runtime.maxMemory() / (1024 * 1024)
        val nativeHeapMb = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)

        return MemoryStats(
            availMemMb = memInfo.availMem / (1024 * 1024),
            totalMemMb = memInfo.totalMem / (1024 * 1024),
            isLowMemory = memInfo.lowMemory,
            usedHeapMb = usedHeapMb,
            maxHeapMb = maxHeapMb,
            nativeHeapMb = nativeHeapMb
        )
    }

    data class MemoryStats(
        val availMemMb: Long,
        val totalMemMb: Long,
        val isLowMemory: Boolean,
        val usedHeapMb: Long,
        val maxHeapMb: Long,
        val nativeHeapMb: Long
    )

    companion object {
        private const val TAG = "MemoryPressureMonitor"
    }
}
