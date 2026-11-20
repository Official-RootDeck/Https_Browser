// Built by MD.Nirob
package com.rootdeck.https_browser.pip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Built by MD.Nirob — BroadcastReceiver handling PiP remote media action buttons.
 */
class PipActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            PipManager.ACTION_PIP_PLAY_PAUSE -> onPlayPauseRequested?.invoke()
            PipManager.ACTION_PIP_MUTE -> onMuteToggleRequested?.invoke()
        }
    }

    companion object {
        var onPlayPauseRequested: (() -> Unit)? = null
        var onMuteToggleRequested: (() -> Unit)? = null
    }
}
