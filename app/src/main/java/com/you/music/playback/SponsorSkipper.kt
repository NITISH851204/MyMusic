package com.you.music.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.you.music.data.Prefs
import com.you.music.data.SponsorBlock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SponsorSkipper(
    private val context: Context,
    private val player: Player,
    private val scope: CoroutineScope
) {
    private var segments: List<LongRange> = emptyList()
    private var loadJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            load(mediaItem?.mediaId)
        }
    }

    init {
        player.addListener(listener)
        load(player.currentMediaItem?.mediaId)
        scope.launch {
            while (isActive) {
                tick()
                delay(500)
            }
        }
    }

    private fun load(mediaId: String?) {
        loadJob?.cancel()
        segments = emptyList()
        if (mediaId.isNullOrEmpty()) return
        loadJob = scope.launch { segments = SponsorBlock.segments(mediaId) }
    }

    private fun tick() {
        if (!player.isPlaying) return
        if (!Prefs.sponsorBlock(context)) return
        val pos = player.currentPosition
        segments.firstOrNull { pos in it }?.let { player.seekTo(it.last + 1) }
    }

    fun release() {
        player.removeListener(listener)
    }
}
