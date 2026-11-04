package com.you.music.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

object Likes {

    private lateinit var prefs: SharedPreferences

    private val _liked = MutableStateFlow<List<Song>>(emptyList())
    val liked: StateFlow<List<Song>> = _liked

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("likes", Context.MODE_PRIVATE)
        _liked.value = load()
    }

    fun toggle(song: Song) {
        _liked.value =
            if (_liked.value.any { it.url == song.url }) {
                _liked.value.filter { it.url != song.url }
            } else {
                listOf(song) + _liked.value
            }
        save()
    }

    private fun save() {
        val arr = JSONArray()
        _liked.value.forEach {
            arr.put(
                JSONObject()
                    .put("title", it.title)
                    .put("artist", it.artist)
                    .put("url", it.url)
                    .put("thumb", it.thumb ?: "")
            )
        }
        prefs.edit().putString("list", arr.toString()).apply()
    }

    private fun load(): List<Song> = try {
        val arr = JSONArray(prefs.getString("list", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Song(
                title = o.getString("title"),
                artist = o.getString("artist"),
                url = o.getString("url"),
                thumb = o.optString("thumb").ifEmpty { null }
            )
        }
    } catch (e: Throwable) {
        emptyList()
    }
}
