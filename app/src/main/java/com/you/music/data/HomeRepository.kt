package com.you.music.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import java.util.concurrent.ConcurrentHashMap

data class Category(val title: String, val query: String)

data class ArtistEntry(val name: String, val query: String)

data class HomeData(
    val forYou: List<Song>,
    val hits: List<Song>,
    val fresh: List<Song>
) {
    val isEmpty: Boolean
        get() = forYou.isEmpty() && hits.isEmpty() && fresh.isEmpty()
}

object HomeRepository {

    // ---- Upar wale rows ----
    private const val HIT_QUERY = "top hindi songs"
    private const val NEW_QUERY = "new hindi songs 2026"
    private const val DEFAULT_FOR_YOU = "romantic hindi songs"

    // ---- Neeche wale rows (pehla card bada dikhta hai) ----
    val hindiRow = listOf(
        Category("Hindi Songs", "hindi songs"),
        Category("90s", "90s hindi songs"),
        Category("80s", "80s hindi songs"),
        Category("2000s", "2000s hindi songs"),
        Category("Retro", "old hindi classic songs")
    )

    val englishRow = listOf(
        Category("English Pop", "english pop songs"),
        Category("Hip Hop", "english hip hop hits"),
        Category("Lo-fi", "lofi songs"),
        Category("Rock", "rock hits")
    )

    val playlistRow = listOf(
        Category("Hindi Playlist", "hindi playlist best songs"),
        Category("Hindi Love Playlist", "hindi love songs"),
        Category("Party Playlist", "bollywood party songs"),
        Category("Sad Playlist", "hindi sad songs")
    )

    val artists = listOf(
        ArtistEntry("KR\$NA", "KRSNA"),
        ArtistEntry("Yo Yo Honey Singh", "Yo Yo Honey Singh"),
        ArtistEntry("Arijit Singh", "Arijit Singh"),
        ArtistEntry("Badshah", "Badshah"),
        ArtistEntry("Diljit Dosanjh", "Diljit Dosanjh"),
        ArtistEntry("Atif Aslam", "Atif Aslam"),
        ArtistEntry("Shreya Ghoshal", "Shreya Ghoshal"),
        ArtistEntry("Neha Kakkar", "Neha Kakkar")
    )

    private var cached: HomeData? = null
    private var cachedAt = 0L

    private val collections = ConcurrentHashMap<String, List<Song>>()
    private val images = ConcurrentHashMap<String, String>()

    // Ek saath 3 se zyada search nahi, taaki YouTube block na kare
    private val gate = Semaphore(3)

    fun cachedData(): HomeData? = cached

    fun peekCollection(query: String): List<Song>? = collections[query]

    fun peekArtistImage(name: String): String? = images[name]

    suspend fun load(force: Boolean = false): HomeData = coroutineScope {
        val old = cached
        if (!force && old != null && System.currentTimeMillis() - cachedAt < 30 * 60 * 1000L) {
            return@coroutineScope old
        }

        val forYouQuery = forYouQuery()
        val a = async { safeSearch(forYouQuery) }
        val b = async { safeSearch(HIT_QUERY) }
        val c = async { safeSearch(NEW_QUERY) }

        val hits = b.await().take(10)
        val seen = hits.map { it.url }.toMutableSet()
        val forYou = a.await().filter { seen.add(it.url) }.take(10)
        val fresh = c.await().filter { seen.add(it.url) }.take(10)

        val data = HomeData(forYou, hits, fresh)
        if (!data.isEmpty) {
            cached = data
            cachedAt = System.currentTimeMillis()
        }
        data
    }

    /** Ek category ke gaane (card dikhte hi ya tap karte hi load hote hain) */
    suspend fun collection(query: String): List<Song> {
        collections[query]?.let { return it }
        return gate.withPermit {
            collections[query]
                ?: safeSearch(query).take(30).also {
                    if (it.isNotEmpty()) collections[query] = it
                }
        }
    }

    /** Artist ki photo. Na mile to uske pehle gaane ka cover */
    suspend fun artistImage(entry: ArtistEntry): String? {
        images[entry.name]?.let { return it }

        val fromArtist = gate.withPermit {
            withContext(Dispatchers.IO) {
                try {
                    val service = ServiceList.YouTube
                    val handler = service.searchQHFactory.fromQuery(
                        entry.query,
                        listOf(YoutubeSearchQueryHandlerFactory.MUSIC_ARTISTS),
                        ""
                    )
                    SearchInfo.getInfo(service, handler)
                        .relatedItems
                        .filterIsInstance<ChannelInfoItem>()
                        .firstOrNull()
                        ?.thumbnails
                        ?.maxByOrNull { it.height }
                        ?.url
                } catch (e: Throwable) {
                    null
                }
            }
        }

        val url = fromArtist ?: collection("${entry.query} songs").firstOrNull()?.thumb
        if (url != null) images[entry.name] = url
        return url
    }

    private suspend fun safeSearch(query: String): List<Song> =
        try {
            MusicRepository.search(query)
        } catch (e: Throwable) {
            emptyList()
        }

    private fun forYouQuery(): String {
        val artist = Likes.liked.value
            .map { it.artist.split(",", "&").first().trim() }
            .filter { it.isNotEmpty() && it != "Unknown" }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
        return if (artist != null) "$artist songs" else DEFAULT_FOR_YOU
    }
}
