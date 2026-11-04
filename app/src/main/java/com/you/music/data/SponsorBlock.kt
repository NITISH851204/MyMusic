package com.you.music.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.security.MessageDigest

object SponsorBlock {

    private val client = OkHttpClient()

    private const val CATEGORIES =
        "[\"sponsor\",\"selfpromo\",\"interaction\",\"intro\",\"outro\",\"music_offtopic\"]"

    /** Skip karne wale hisse milliseconds ke ranges me. Error ya 404 par khali list. */
    suspend fun segments(videoUrl: String): List<LongRange> = withContext(Dispatchers.IO) {
        try {
            val id = Uri.parse(videoUrl).getQueryParameter("v")
                ?: return@withContext emptyList<LongRange>()

            // Privacy: poori ID nahi, sirf SHA-256 hash ke pehle 4 characters bhejte hain
            val prefix = MessageDigest.getInstance("SHA-256")
                .digest(id.toByteArray())
                .joinToString("") { "%02x".format(it) }
                .take(4)

            val url = "https://sponsor.ajay.app/api/skipSegments/$prefix".toHttpUrl()
                .newBuilder()
                .addQueryParameter("categories", CATEGORIES)
                .addQueryParameter("actionType", "skip")
                .build()

            client.newCall(Request.Builder().url(url).build()).execute().use { res ->
                if (!res.isSuccessful) return@use emptyList<LongRange>()
                val arr = JSONArray(res.body?.string() ?: "[]")
                val out = mutableListOf<LongRange>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.optString("videoID") != id) continue
                    val segs = obj.getJSONArray("segments")
                    for (j in 0 until segs.length()) {
                        val range = segs.getJSONObject(j).getJSONArray("segment")
                        val start = (range.getDouble(0) * 1000).toLong()
                        val end = (range.getDouble(1) * 1000).toLong()
                        out.add(start..end)
                    }
                }
                out.toList()
            }
        } catch (e: Throwable) {
            emptyList<LongRange>()
        }
    }
}
