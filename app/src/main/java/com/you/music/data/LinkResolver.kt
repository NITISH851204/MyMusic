package com.you.music.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo

/** YouTube se share kiye gaye link ko gaane me badalta hai */
object LinkResolver {

    private val urlRegex = Regex("https?://[^\\s]+")

    /** Share text me se YouTube/YouTube Music ka link nikalta hai, warna null */
    fun extractUrl(text: String): String? {
        val url = urlRegex.find(text)?.value ?: return null
        val host = Uri.parse(url).host?.lowercase() ?: return null
        val ok = host == "youtu.be" || host.endsWith("youtube.com")
        return if (ok) url else null
    }

    suspend fun song(url: String): Song = withContext(Dispatchers.IO) {
        val info = StreamInfo.getInfo(ServiceList.YouTube, url)
        Song(
            title = info.name,
            artist = info.uploaderName ?: "Unknown",
            url = info.url,
            thumb = info.thumbnails.firstOrNull()?.url
        )
    }
}
