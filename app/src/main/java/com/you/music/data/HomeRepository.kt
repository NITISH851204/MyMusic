package com.you.music.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class HomeData(
    val forYou: List<Song>,
    val hits: List<Song>,
    val fresh: List<Song>
) {
    val isEmpty: Boolean
        get() = forYou.isEmpty() && hits.isEmpty() && fresh.isEmpty()
}

object HomeRepository {

    // Yahan apni pasand ke queries likh sakte ho (jaise "punjabi songs", "tamil songs")
    private const val HIT_QUERY = "top hindi songs"
    private const val NEW_QUERY = "new hindi songs 2026"
    private const val DEFAULT_FOR_YOU = "romantic hindi songs"

    private var cached: HomeData? = null
    private var cachedAt = 0L

    fun cachedData(): HomeData? = cached

    suspend fun load(force: Boolean = false): HomeData = coroutineScope {
        val old = cached
        if (!force && old != null && System.currentTimeMillis() - cachedAt < 30 * 60 * 1000L) {
            return@coroutineScope old
        }

        val forYouQuery = forYouQuery()
        val a = async { safeSearch(forYouQuery) }
        val b = async { safeSearch(HIT_QUERY) }
        val c = async { safeSearch(NEW_QUERY) }

        // Ek gaana teeno rows me repeat na ho
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

    private suspend fun safeSearch(query: String): List<Song> =
        try {
            MusicRepository.search(query)
        } catch (e: Throwable) {
            emptyList()
        }

    /** Sabse zyada liked artist ke gaane, warna default query */
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
