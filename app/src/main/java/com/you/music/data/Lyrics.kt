package com.you.music.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.abs

data class LyricLine(val timeMs: Long, val text: String)

data class LyricsResult(val synced: List<LyricLine>, val plain: String)

object LyricsRepository {

    private val client = OkHttpClient()
    private val memory = HashMap<String, LyricsResult>()
    private val lrcRegex = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]""")

    suspend fun fetch(
        context: Context,
        title: String,
        artist: String,
        durationSec: Long
    ): LyricsResult? = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext null
        try {
            val cleanTitle = cleanTitle(title)
            val firstArtist = artist.split(",", "&", " feat", " ft.").first().trim()
            val key = "$cleanTitle|$firstArtist".lowercase()

            memory[key]?.let { return@withContext it }

            val dir = File(context.filesDir, "lyrics").apply { mkdirs() }
            val file = File(dir, key.hashCode().toString(16) + ".json")
            if (file.exists()) {
                parseSaved(file.readText())?.let {
                    memory[key] = it
                    return@withContext it
                }
            }

            var best: JSONObject? = query(
                mapOf("track_name" to cleanTitle, "artist_name" to firstArtist)
            )?.let { pick(it, durationSec) }

            if (best == null) {
                best = query(mapOf("q" to "$cleanTitle $firstArtist"))
                    ?.let { pick(it, durationSec) }
            }
            if (best == null) return@withContext null

            val syncedRaw = best.str("syncedLyrics")
            val plain = best.str("plainLyrics")
            val synced = parseLrc(syncedRaw)
            if (synced.isEmpty() && plain.isBlank()) return@withContext null

            val result = LyricsResult(synced, plain)
            memory[key] = result
            file.writeText(JSONObject().put("s", syncedRaw).put("p", plain).toString())
            result
        } catch (e: Throwable) {
            null
        }
    }

    private fun JSONObject.str(name: String): String =
        if (isNull(name)) "" else optString(name)

    private fun query(params: Map<String, String>): JSONArray? {
        val url = "https://lrclib.net/api/search".toHttpUrl().newBuilder().apply {
            params.forEach { (k, v) -> addQueryParameter(k, v) }
        }.build()
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "MyMusic/0.1 (open-source music player)")
            .build()
        return client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) null else JSONArray(res.body?.string() ?: "[]")
        }
    }

    private fun pick(arr: JSONArray, durationSec: Long): JSONObject? {
        val items = (0 until arr.length())
            .map { arr.getJSONObject(it) }
            .filter {
                !it.optBoolean("instrumental") &&
                    (it.str("syncedLyrics").isNotBlank() || it.str("plainLyrics").isNotBlank())
            }
        return items.sortedWith(
            compareBy<JSONObject> {
                if (durationSec > 0 && abs(it.optDouble("duration", 0.0) - durationSec) > 6) 1 else 0
            }
                .thenByDescending { it.str("syncedLyrics").isNotBlank() }
                .thenBy { abs(it.optDouble("duration", 0.0) - durationSec) }
        ).firstOrNull()
    }

    private fun parseSaved(text: String): LyricsResult? = try {
        val o = JSONObject(text)
        LyricsResult(parseLrc(o.optString("s")), o.optString("p"))
    } catch (e: Throwable) {
        null
    }

    fun parseLrc(lrc: String): List<LyricLine> {
        val out = mutableListOf<LyricLine>()
        lrc.lineSequence().forEach { line ->
            val matches = lrcRegex.findAll(line).toList()
            if (matches.isEmpty()) return@forEach
            val text = line.substring(matches.last().range.last + 1).trim()
            if (text.isEmpty()) return@forEach
            matches.forEach { m ->
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val ms = when (frac.length) {
                    0 -> 0L
                    1 -> frac.toLong() * 100
                    2 -> frac.toLong() * 10
                    else -> frac.take(3).toLong()
                }
                out.add(LyricLine(min * 60000 + sec * 1000 + ms, text))
            }
        }
        return out.sortedBy { it.timeMs }
    }

    /** "Pal Pal (from Blackmail)" ya "Pal Pal - Title Track" ko sirf "Pal Pal" banata hai */
    private fun cleanTitle(t: String): String =
        t.replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("\\s[-–|].*$"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifEmpty { t }
}
