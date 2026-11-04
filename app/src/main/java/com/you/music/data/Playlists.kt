package com.you.music.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

data class Playlist(val id: String, val name: String, val songs: List<Song>)

object Playlists {

    private lateinit var prefs: SharedPreferences

    private val _all = MutableStateFlow<List<Playlist>>(emptyList())
    val all: StateFlow<List<Playlist>> = _all

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("playlists", Context.MODE_PRIVATE)
        _all.value = load()
    }

    fun create(name: String): String {
        val id = System.currentTimeMillis().toString()
        _all.value = _all.value + Playlist(id, name, emptyList())
        save()
        return id
    }

    fun delete(id: String) {
        _all.value = _all.value.filter { it.id != id }
        save()
    }

    fun addSong(id: String, song: Song) {
        _all.value = _all.value.map { p ->
            if (p.id == id && p.songs.none { it.url == song.url }) {
                p.copy(songs = p.songs + song)
            } else p
        }
        save()
    }

    fun removeSong(id: String, url: String) {
        _all.value = _all.value.map { p ->
            if (p.id == id) p.copy(songs = p.songs.filter { it.url != url }) else p
        }
        save()
    }

    private fun save() {
        val arr = JSONArray()
        _all.value.forEach { p ->
            val songs = JSONArray()
            p.songs.forEach { songs.put(it.toJson()) }
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("songs", songs)
            )
        }
        prefs.edit().putString("list", arr.toString()).apply()
    }

    private fun load(): List<Playlist> = try {
        val arr = JSONArray(prefs.getString("list", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val songsArr = o.getJSONArray("songs")
            Playlist(
                id = o.getString("id"),
                name = o.getString("name"),
                songs = (0 until songsArr.length()).map { songsArr.getJSONObject(it).toSong() }
            )
        }
    } catch (e: Throwable) {
        emptyList()
    }
}
