package com.rootdeck.https_browser.engine

data class EngineConfig(
    val processPerSite: Boolean = true,
    val rendererProcessLimit: Int = 1,
    val maxV8OldSpaceSizeMb: Int = 128,
    val remoteDebuggingEnabled: Boolean = true,
    val remoteDebuggingPort: Int = 9222,
    val trackingProtectionEnabled: Boolean = true,
    val desktopMode: Boolean = false,
    val javascriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val hardwareAccelerated: Boolean = true
) {
    fun toCommandLineFlags(): List<String> = listOf(
        "--process-per-site",
        "--renderer-process-limit=$rendererProcessLimit",
        "--disable-gpu-sandbox",
        "--no-sandbox",
        "--remote-debugging-port=$remoteDebuggingPort",
        "--remote-allow-origins=*",
        "--js-flags=--max-old-space-size=$maxV8OldSpaceSizeMb",
        "--disable-dev-shm-usage"
    )
}
