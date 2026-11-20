// Built by MD.Nirob
package com.rootdeck.https_browser.pip

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import com.rootdeck.https_browser.R

/**
 * Built by MD.Nirob — Manages Picture-in-Picture (PiP) mode for HTML5 video playback.
 */
object PipManager {

    const val ACTION_PIP_PLAY_PAUSE = "com.rootdeck.https_browser.PIP_PLAY_PAUSE"
    const val ACTION_PIP_MUTE = "com.rootdeck.https_browser.PIP_MUTE"

    fun isPipSupported(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    fun buildPipParams(context: Context, isPlaying: Boolean, isMuted: Boolean): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null

        val playPauseIntent = PendingIntent.getBroadcast(
            context,
            101,
            Intent(context, PipActionReceiver::class.java).setAction(ACTION_PIP_PLAY_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val muteIntent = PendingIntent.getBroadcast(
            context,
            102,
            Intent(context, PipActionReceiver::class.java).setAction(ACTION_PIP_MUTE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = Icon.createWithResource(
            context,
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
        val muteIcon = Icon.createWithResource(
            context,
            if (isMuted) android.R.drawable.ic_lock_silent_mode_off else android.R.drawable.ic_lock_silent_mode
        )

        val actions = listOf(
            RemoteAction(
                playPauseIcon,
                context.getString(R.string.new_pip_play_pause),
                context.getString(R.string.new_pip_play_pause),
                playPauseIntent
            ),
            RemoteAction(
                muteIcon,
                context.getString(R.string.new_pip_mute),
                context.getString(R.string.new_pip_mute),
                muteIntent
            )
        )

        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(actions)
            .build()
    }

    fun enterPip(activity: ComponentActivity, isPlaying: Boolean = true, isMuted: Boolean = false): Boolean {
        if (!isPipSupported(activity)) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = buildPipParams(activity, isPlaying, isMuted) ?: return false
            return activity.enterPictureInPictureMode(params)
        }
        return false
    }

    fun updatePipParams(activity: ComponentActivity, isPlaying: Boolean, isMuted: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isPipSupported(activity)) {
            buildPipParams(activity, isPlaying, isMuted)?.let { params ->
                activity.setPictureInPictureParams(params)
            }
        }
    }

    fun getVideoCheckScript(): String = """
        (function() {
            var videos = document.querySelectorAll('video');
            for (var i = 0; i < videos.length; i++) {
                if (!videos[i].paused && !videos[i].ended) return 'playing';
            }
            return videos.length > 0 ? 'paused' : 'none';
        })();
    """.trimIndent()
}
