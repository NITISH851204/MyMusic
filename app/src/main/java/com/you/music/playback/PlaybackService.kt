package com.you.music.playback

import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.you.music.data.MusicRepository
import com.you.music.data.remote.NewPipeDownloader
import kotlinx.coroutines.runBlocking
import java.io.IOException

class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(NewPipeDownloader.USER_AGENT)
            .setAllowCrossProtocolRedirects(true)

        val resolving = ResolvingDataSource.Factory(
            http,
            ResolvingDataSource.Resolver { spec ->
                val videoUrl = spec.uri.getQueryParameter("u")
                if (spec.uri.scheme == "yt" && videoUrl != null) {
                    try {
                        val real = runBlocking { MusicRepository.audioUrl(videoUrl) }
                        spec.withUri(Uri.parse(real))
                    } catch (e: Exception) {
                        throw IOException(e.message ?: "resolve failed", e)
                    }
                } else {
                    spec
                }
            }
        )

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(resolving))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        session = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? = session

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
