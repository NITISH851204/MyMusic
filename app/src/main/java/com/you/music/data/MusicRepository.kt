package com.you.music.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

data class Song(val title: String, val artist: String, val url: String)

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
            .map { Song(it.name, it.uploaderName ?: "Unknown", it.url) }
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
