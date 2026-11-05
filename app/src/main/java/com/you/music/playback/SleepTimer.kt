package com.you.music.playback

import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** PlaybackService apna player yahan rakhta hai, taaki app band ho tab bhi timer gaana roke */
object PlaybackHolder {
    @Volatile
    var player: Player? = null
}

object SleepTimer {

    private val handler = Handler(Looper.getMainLooper())

    private val _endAt = MutableStateFlow(0L)
    val endAt: StateFlow<Long> = _endAt

    private val stopTask = Runnable {
        PlaybackHolder.player?.pause()
        _endAt.value = 0L
    }

    fun start(minutes: Int) {
        handler.removeCallbacks(stopTask)
        val millis = minutes * 60_000L
        _endAt.value = System.currentTimeMillis() + millis
        handler.postDelayed(stopTask, millis)
    }

    fun cancel() {
        handler.removeCallbacks(stopTask)
        _endAt.value = 0L
    }
}
