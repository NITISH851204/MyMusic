package com.you.music.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType

data class ImportedPlaylist(val name: String, val songs: List<Song>)

object PlaylistImporter {

    /** Public ya unlisted YouTube playlist ke link se gaane nikalta hai (300 tak) */
    suspend fun import(rawUrl: String): ImportedPlaylist = withContext(Dispatchers.IO) {
        val url = rawUrl.trim()
        val service = ServiceList.YouTube
        val info = PlaylistInfo.getInfo(service, url)

        val items = ArrayList<StreamInfoItem>(info.relatedItems)
        var page = info.nextPage
        var rounds = 0
        while (page != null && items.size < 300 && rounds < 10) {
            val more = PlaylistInfo.getMoreItems(service, url, page)
            items.addAll(more.items)
            page = more.nextPage
            rounds++
        }

        val songs = items
            .filter {
                it.streamType != StreamType.LIVE_STREAM &&
                    it.streamType != StreamType.AUDIO_LIVE_STREAM
            }
            .map {
                Song(
                    title = it.name,
                    artist = it.uploaderName ?: "Unknown",
                    url = it.url,
                    thumb = it.thumbnails.firstOrNull()?.url
                )
            }
            .distinctBy { it.url }

        ImportedPlaylist(info.name ?: "Imported playlist", songs)
    }
}
