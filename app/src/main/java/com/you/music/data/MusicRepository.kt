package com.you.music.data

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

data class Song(
    val title: String,
    val artist: String,
    val url: String,
    val thumb: String? = null
)

/** Queue me jaane wala item. Asli stream URL playback ke time resolve hota hai. */
fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(url)
    .setUri(Uri.parse("yt://resolve?u=" + Uri.encode(url)))
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(thumb?.let { Uri.parse(it) })
            .build()
    )
    .build()

object MusicRepository {

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        val service = ServiceList.YouTube
        val handler = service.searchQHFactory.fromQuery(
            query,
            listOf(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS),
            ""
        )
        val info = SearchInfo.getInfo(service, handler)
        info.relatedItems
            .filterIsInstance<StreamInfoItem>()
            .map {
                Song(
                    title = it.name,
                    artist = it.uploaderName ?: "Unknown",
                    url = it.url,
                    thumb = it.thumbnails.firstOrNull()?.url
                )
            }
    }

    suspend fun audioUrl(videoUrl: String): String = withContext(Dispatchers.IO) {
        val info = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)
        info.audioStreams
            .filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
            .maxByOrNull { it.averageBitrate }
            ?.content
            ?: error("Audio stream nahi mila")
    }
}
